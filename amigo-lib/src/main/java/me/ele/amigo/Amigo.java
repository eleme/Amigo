package me.ele.amigo;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import me.ele.amigo.hook.HookFactory;
import me.ele.amigo.reflect.MethodUtils;
import me.ele.amigo.release.ApkReleaser;
import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.CrcUtils;
import me.ele.amigo.utils.PermissionChecker;
import me.ele.amigo.utils.ProcessUtils;
import me.ele.amigo.utils.component.ReceiverFinder;

import static android.content.pm.PackageManager.GET_META_DATA;
import static me.ele.amigo.compat.ActivityThreadCompat.instance;
import static me.ele.amigo.reflect.FieldUtils.getField;
import static me.ele.amigo.reflect.FieldUtils.readField;
import static me.ele.amigo.reflect.FieldUtils.readStaticField;
import static me.ele.amigo.reflect.FieldUtils.writeField;
import static me.ele.amigo.reflect.MethodUtils.getMatchedMethod;
import static me.ele.amigo.reflect.MethodUtils.invokeMethod;
import static me.ele.amigo.reflect.MethodUtils.invokeStaticMethod;
import static me.ele.amigo.utils.ClassLoaderUtils.getRootClassLoader;
import static me.ele.amigo.utils.CrcUtils.getCrc;
import static me.ele.amigo.utils.FileUtils.copyFile;
import static me.ele.amigo.utils.FileUtils.removeFile;

public class Amigo extends Application {
    private static final String TAG = Amigo.class.getSimpleName();

    public static final String SP_NAME = "Amigo";
    public static final String WORKING_PATCH_APK_CHECKSUM = "working_patch_apk_checksum";
    public static final String VERSION_CODE = "version_code";

    private static LoadPatchError loadPatchError;

    private SharedPreferences sharedPref;

    private ClassLoader originalClassLoader;
    private AmigoClassLoader patchedClassLoader;

    private AmigoDirs amigoDirs;
    private PatchApks patchApks;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            originalClassLoader = getClassLoader();
            sharedPref = getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS);
            amigoDirs = AmigoDirs.getInstance(this);
            patchApks = PatchApks.getInstance(this);
            String workingPatchApkChecksum = sharedPref.getString(WORKING_PATCH_APK_CHECKSUM, "");
            try {
                Log.e(TAG, "working checksum: " + workingPatchApkChecksum);
                if (checkUpgrade()) {
                    throw new RuntimeException("Host app has upgrade");
                }
                if (TextUtils.isEmpty(workingPatchApkChecksum)
                        || !patchApks.exists(workingPatchApkChecksum)) {
                    throw new RuntimeException("Patch apk doesn't exists");
                }
                if (!checkSignature(workingPatchApkChecksum)) {
                    loadPatchError = LoadPatchError.record(LoadPatchError.SIG_ERR, null);
                    throw new RuntimeException("Patch apk has illegal signature");
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                if (ProcessUtils.isMainProcess(this)) {
                    // clear is a dangerous operation, only need to be operated by main process
                    doClear(this);
                }
                runOriginalApplication();
                return;
            }

            // ensure load dex process always run host apk not patch apk
            if (ProcessUtils.isLoadDexProcess(this)) {
                Log.e(TAG, "load dex process");
                runOriginalApplication();
                return;
            }

            if (!ProcessUtils.isMainProcess(this) && isPatchApkFirstRun(workingPatchApkChecksum)) {
                Log.e(TAG, "None main process and patch apk is not released yet");
                runOriginalApplication();
                return;
            }

            // only release loaded apk in the main process
            runPatchApk(workingPatchApkChecksum);
        } catch (LoadPatchApkException e) {
            e.printStackTrace();
            loadPatchError = LoadPatchError.record(LoadPatchError.LOAD_ERR, e);
            try {
                runOriginalApplication();
            } catch (Throwable e2) {
                throw new RuntimeException(e2);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void runPatchApk(String checksum) throws LoadPatchApkException {
        try {
            Log.e(TAG, "patchApkChecksum-->" + checksum + ", sp record checksum--->"
                    + sharedPref.getString(WORKING_PATCH_APK_CHECKSUM, ""));
            if (isPatchApkFirstRun(checksum) || !isOptedDexExists(checksum)) {
                // TODO This is workaround for now, refactor in future.
                sharedPref.edit().remove(checksum).commit();
                releasePatchApk(checksum);
            } else {
                checkDexAndSoChecksum(checksum);
            }

            String dexPathes = getDexPath(checksum);
            AmigoClassLoader amigoClassLoader = new AmigoClassLoader(dexPathes,
                    AmigoDirs.getInstance(this).dexOptDir(checksum),
                    AmigoDirs.getInstance(this).libDir(checksum).getAbsolutePath(),
                    getRootClassLoader());
            setAPKClassLoader(amigoClassLoader);
            patchedClassLoader = amigoClassLoader;

            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = getMatchedMethod(AssetManager.class, "addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            addAssetPath.invoke(assetManager, patchApks.patchPath(checksum));
            setAPKResources(assetManager);

            setApkInstrumentation();
            setApkHandler();
            dynamicRegisterReceivers(amigoClassLoader);

            sharedPref.edit().putString(WORKING_PATCH_APK_CHECKSUM, checksum).commit();
            clearOldPatches(checksum);
            installHook(amigoClassLoader);
            runPatchedApplication();
        } catch (Exception e) {
            throw new LoadPatchApkException(e);
        }
    }

  private String getDexPath(String checksum) throws LoadPatchApkException {
        File[] patchDexFiles = AmigoDirs.getInstance(this).dexDir(checksum).listFiles(
                new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.getName().endsWith(".dex");
                    }
                });
        String dexPath = "";
        if (patchDexFiles != null && patchDexFiles.length > 0) {
            for (File patchDex : patchDexFiles) {
                dexPath += ":" + patchDex.getAbsolutePath();
            }
        } else {
            LoadPatchApkException e= new LoadPatchApkException("Amigo: no dexes avilable");
            e.fillInStackTrace();
            throw  e;
        }
        return dexPath;
    }

    private void installHook(AmigoClassLoader amigoClassLoader) throws Exception {
        Class hookFactoryClazz = amigoClassLoader.loadClass(HookFactory.class.getName());
        MethodUtils.invokeStaticMethod(hookFactoryClazz, "install", this, amigoClassLoader);
    }

    private void dynamicRegisterReceivers(ClassLoader classLoader) {
        ReceiverFinder.registerNewReceivers(this, classLoader);
    }

    private void setApkInstrumentation() throws Exception {
        Instrumentation oldInstrumentation = (Instrumentation) readField(instance(), "mInstrumentation", true);
        Log.e(TAG, "oldInstrumentation--->" + oldInstrumentation);
        AmigoInstrumentation instrumentation = new AmigoInstrumentation(oldInstrumentation);
        writeField(instance(), "mInstrumentation", instrumentation, true);
        Log.e(TAG, "setApkInstrumentation success classloader-->" + instrumentation.getClass().getClassLoader());
    }

    private void setApkHandler() throws Exception {
        Handler handler = (Handler) readField(instance(), "mH", true);
        Object callback = readField(handler, "mCallback", true);
        AmigoCallback value = new AmigoCallback(this, patchedClassLoader, (Handler.Callback) callback);
        writeField(handler, "mCallback", value);
        Log.e(TAG, "hook handler success");
    }

    private void releasePatchApk(String checksum) throws Exception {
        //clear previous working dir
        clearWithoutPatchApk(checksum);

        //start a new process to handle time-tense operation
        ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), GET_META_DATA);
        String layoutName = appInfo.metaData.getString("amigo_layout");
        String themeName = appInfo.metaData.getString("amigo_theme");
        int layoutId = 0;
        int themeId = 0;
        if (!TextUtils.isEmpty(layoutName)) {
            layoutId = (int) readStaticField(Class.forName(getPackageName() + ".R$layout"), layoutName);
        }
        if (!TextUtils.isEmpty(themeName)) {
            themeId = (int) readStaticField(Class.forName(getPackageName() + ".R$style"), themeName);
        }
        Log.e(TAG, String.format("layoutName-->%s, themeName-->%s", layoutName, themeName));
        Log.e(TAG, String.format("layoutId-->%d, themeId-->%d", layoutId, themeId));

        ApkReleaser.getInstance(this).work(checksum, layoutId, themeId);
        Log.e(TAG, "release apk once");
    }

    private boolean isPatchApkFirstRun(String checksum) {
        return !sharedPref.getString(WORKING_PATCH_APK_CHECKSUM, "").equals(checksum);
    }

    private boolean isOptedDexExists(String checksum) {
        if (amigoDirs.dexOptDir(checksum).listFiles() != null) {
            return amigoDirs.dexOptDir(checksum).listFiles().length > 0;
        }
        return false;
    }

    private boolean checkUpgrade() {
        boolean result = false;
        int recordVersion = sharedPref.getInt(VERSION_CODE, 0);
        int currentVersion = CommonUtils.getVersionCode(this);
        if (currentVersion > recordVersion) {
            result = true;
        }
        sharedPref.edit().putInt(VERSION_CODE, currentVersion).commit();
        return result;
    }

    private void runOriginalApplication() throws Exception {
        setAPKClassLoader(originalClassLoader);
        Class acd = originalClassLoader.loadClass("me.ele.amigo.acd");
        String applicationName = (String) readStaticField(acd, "n");
        Application application =
                (Application) originalClassLoader.loadClass(applicationName).newInstance();
        Method attach = getMatchedMethod(Application.class, "attach", Context.class);
        attach.setAccessible(true);
        attach.invoke(application, getBaseContext());
        setAPKApplication(application);
        application.onCreate();
    }

    private void runPatchedApplication() throws Exception {
        setAPKClassLoader(patchedClassLoader);
        Class acd = patchedClassLoader.loadClass("me.ele.amigo.acd");
        String applicationName = (String) readStaticField(acd, "n");
        Application application = (Application) patchedClassLoader.loadClass(applicationName).newInstance();
        Method attach = getMatchedMethod(Application.class, "attach", Context.class);
        attach.setAccessible(true);
        attach.invoke(application, getBaseContext());
        setAPKApplication(application);
        application.onCreate();
    }

    private void checkDexAndSoChecksum(String apkChecksum) throws Exception {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS);
        File[] dexFiles = amigoDirs.dexDir(apkChecksum).listFiles();
        for (File dexFile : dexFiles) {
            String savedChecksum = sp.getString(dexFile.getAbsolutePath(), "");
            String checksum = getCrc(dexFile);
            if (!savedChecksum.equals(checksum)) {
                throw new IllegalStateException("wrong dex check sum");
            }
        }

        File[] dexOptFiles = amigoDirs.dexOptDir(apkChecksum).listFiles();
        for (File dexOptFile : dexOptFiles) {
            String savedChecksum = sp.getString(dexOptFile.getAbsolutePath(), "");
            String checksum = getCrc(dexOptFile);
            Log.e(TAG, "opt dexFile-->" + dexOptFile);
            Log.e(TAG, "savedChecksum-->" + savedChecksum + ", checksum--->" + checksum);
            if (!savedChecksum.equals(checksum)) {
                throw new IllegalStateException("wrong opt dex check sum");
            }
        }

        File[] nativeFiles = amigoDirs.libDir(apkChecksum).listFiles();
        if (nativeFiles != null && nativeFiles.length > 0) {
            for (File nativeFile : nativeFiles) {
                String savedChecksum = sp.getString(nativeFile.getAbsolutePath(), "");
                String checksum = getCrc(nativeFile);
                if (!savedChecksum.equals(checksum)) {
                    throw new IllegalStateException("wrong native lib check sum");
                }
            }
        }
    }

    private void setAPKResources(AssetManager newAssetManager) throws Exception {
        invokeMethod(newAssetManager, "ensureStringBlocks");

        Collection<WeakReference<Resources>> references;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Class<?> resourcesManagerClass = Class.forName("android.app.ResourcesManager");
            Object resourcesManager = invokeStaticMethod(resourcesManagerClass, "getInstance");

            if (getField(resourcesManagerClass, "mActiveResources") != null) {
                ArrayMap<?, WeakReference<Resources>> arrayMap = (ArrayMap) readField(resourcesManager, "mActiveResources", true);
                references = arrayMap.values();
            } else {
                references = (Collection) readField(resourcesManager, "mResourceReferences", true);
            }
        } else {
            HashMap<?, WeakReference<Resources>> map = (HashMap) readField(instance(), "mActiveResources", true);
            references = map.values();
        }

        for (WeakReference<Resources> wr : references) {
            Resources resources = wr.get();
            if (resources == null) continue;

            try {
                writeField(resources, "mAssets", newAssetManager);
            } catch (Throwable ignore) {
                Object resourceImpl = readField(resources, "mResourcesImpl", true);
                writeField(resourceImpl, "mAssets", newAssetManager);
            }

            resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (WeakReference<Resources> wr : references) {
                Resources resources = wr.get();
                if (resources == null) continue;

                // android.util.Pools$SynchronizedPool<TypedArray>
                Object typedArrayPool = readField(resources, "mTypedArrayPool", true);

                // Clear all the pools
                while (invokeMethod(typedArrayPool, "acquire") != null) ;
            }
        }
    }

    private void setAPKClassLoader(ClassLoader classLoader) throws Exception {
        writeField(getLoadedApk(), "mClassLoader", classLoader);
    }

    private void setAPKApplication(Application application) throws Exception {
        Object apk = getLoadedApk();
        writeField(apk, "mApplication", application);
    }

    private static Object getLoadedApk() throws Exception {
        Map<String, WeakReference<Object>> mPackages = (Map<String, WeakReference<Object>>) readField(instance(), "mPackages", true);
        for (String s : mPackages.keySet()) {
            WeakReference wr = mPackages.get(s);
            if (wr != null && wr.get() != null) {
                return wr.get();
            }
        }
        return null;
    }

    public static void workLater(Context context, File patchFile) {
        checkPatchApk(context, patchFile);
        String patchChecksum = CrcUtils.getCrc(patchFile);
        if (!PatchApks.getInstance(context).exists(patchChecksum)) {
            copyFile(patchFile, PatchApks.getInstance(context).patchFile(patchChecksum));
        }

        AmigoService.start(context, patchChecksum, true);
    }

    public static void work(Context context, File patchFile) {
        checkPatchApk(context, patchFile);
        String patchChecksum = CrcUtils.getCrc(patchFile);
        if (!PatchApks.getInstance(context).exists(patchChecksum)) {
            copyFile(patchFile, PatchApks.getInstance(context).patchFile(patchChecksum));
        }

        context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS)
                .edit()
                .putString(Amigo.WORKING_PATCH_APK_CHECKSUM, patchChecksum)
                .commit();
        AmigoService.start(context, patchChecksum, false);
        System.exit(0);
        Process.killProcess(Process.myPid());
    }

    private static void checkPatchApk(Context context, File patchFile) {
        if (patchFile == null) {
            throw new NullPointerException("param apkFile cannot be null");
        }

        if (!patchFile.exists()) {
            throw new IllegalArgumentException("param apkFile doesn't exist");
        }

        if (!patchFile.canRead()) {
            throw new IllegalArgumentException("param apkFile cannot be read");
        }

        if (!PermissionChecker.checkPatchPermission(context, patchFile)) {
            throw new IllegalStateException("patch apk cannot request more permissions than host");
        }
    }

    private boolean checkSignature(String checksum) {
        try {
            Signature appSig = CommonUtils.getSignature(this);
            Signature patchSig = patchApks.signature(checksum);
            return appSig.hashCode() == patchSig.hashCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void doClear(Context context) {
        Log.e(TAG, "clear");
        removeFile(AmigoDirs.getInstance(context).amigoDir());
        context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS)
                .edit()
                .clear()
                .putInt(VERSION_CODE, CommonUtils.getVersionCode(context))
                .commit();
    }

    private void clearWithoutPatchApk(String checksum) {
        Log.e(TAG, "clear without patch");
        File[] patchDirs = amigoDirs.amigoDir().listFiles();
        if (patchDirs != null && patchDirs.length > 0) {
            for (File patchDir : patchDirs) {
                if (patchDir.getName().equals(checksum)) {
                    File[] files = patchDir.listFiles();
                    for (File file : files) {
                        if (!file.getAbsolutePath().equals(patchApks.patchPath(checksum))) {
                            Log.e(TAG, "remove file: " + file.getAbsolutePath());
                            removeFile(file, false);
                        }
                    }
                }
            }
        }
    }

    private void clearOldPatches(String exclude) {
        Log.e(TAG, "clear old patches");
        File[] patchDirs = amigoDirs.amigoDir().listFiles();
        if (patchDirs == null || patchDirs.length == 0) {
            return;
        }
        for (File patchDir : patchDirs) {
            if (!patchDir.getName().equals(exclude)) {
                removeFile(patchDir, true);
            }
        }
    }

    public static boolean hasWorked() {
        ClassLoader classLoader = null;
        try {
            classLoader = (ClassLoader) readField(getLoadedApk(), "mClassLoader");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return classLoader != null && classLoader.getClass().getName().equals(AmigoClassLoader.class.getName());
    }

    public static int workingPatchVersion(Context ctx) {
        if (!hasWorked() || TextUtils.isEmpty(getWorkingPatchApkChecksum(ctx))) return -1;
        return CommonUtils.getVersionCode(ctx, PatchApks.getInstance(ctx).patchFile(getWorkingPatchApkChecksum(ctx)));
    }

    public static String getWorkingPatchApkChecksum(Context ctx) {
        if (!hasWorked()) return "";
        return ctx.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS).getString(WORKING_PATCH_APK_CHECKSUM, "");
    }

    public static void clear(Context context) {
        context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS)
                .edit()
                .putString(WORKING_PATCH_APK_CHECKSUM, "")
                .commit();
    }

    public static LoadPatchError getLoadPatchError() {
        return loadPatchError;
    }

    private static class LoadPatchApkException extends Exception {
        public LoadPatchApkException(Throwable throwable) {
            super(throwable);
        }

        public LoadPatchApkException(String msg) {

        }
    }
}
