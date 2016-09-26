package me.ele.amigo;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.ele.amigo.release.ApkReleaser;
import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.CrcUtils;
import me.ele.amigo.utils.ComponentUtils;
import me.ele.amigo.utils.ProcessUtils;

import static android.content.pm.PackageManager.GET_META_DATA;
import static me.ele.amigo.compat.ActivityThreadCompat.instance;
import static me.ele.amigo.reflect.FieldUtils.getField;
import static me.ele.amigo.reflect.FieldUtils.readField;
import static me.ele.amigo.reflect.FieldUtils.readStaticField;
import static me.ele.amigo.reflect.FieldUtils.writeField;
import static me.ele.amigo.reflect.MethodUtils.getDeclaredMethod;
import static me.ele.amigo.reflect.MethodUtils.invokeMethod;
import static me.ele.amigo.reflect.MethodUtils.invokeStaticMethod;
import static me.ele.amigo.utils.CrcUtils.getCrc;
import static me.ele.amigo.utils.DexUtils.getElementWithDex;
import static me.ele.amigo.utils.DexUtils.getPathList;
import static me.ele.amigo.utils.DexUtils.getRootClassLoader;
import static me.ele.amigo.utils.DexUtils.injectSoAtFirst;
import static me.ele.amigo.utils.FileUtils.copyFile;
import static me.ele.amigo.utils.FileUtils.removeFile;

public class Amigo extends Application {
    private static final String TAG = Amigo.class.getSimpleName();

    public static final String SP_NAME = "Amigo";
    public static final String WORKING_PATCH_APK_CHECKSUM = "working_patch_apk_checksum";
    public static final String VERSION_CODE = "version_code";

    private SharedPreferences sharedPref;

    private ClassLoader originalClassLoader;
    private ClassLoader patchedClassLoader;

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
                if (TextUtils.isEmpty(workingPatchApkChecksum) || !patchApks.exists(workingPatchApkChecksum)) {
                    throw new RuntimeException("Patch apk doesn't exists");
                }
                if (!checkSignature(workingPatchApkChecksum)) {
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
            Log.e(TAG, "patchApkChecksum-->" + checksum + ", sp record checksum--->" + sharedPref.getString(WORKING_PATCH_APK_CHECKSUM, ""));
            if (isPatchApkFirstRun(checksum) || !isOptedDexExists(checksum)) {
                // TODO This is workaround for now, refactor in future.
                sharedPref.edit().remove(checksum).commit();
                releasePatchApk(checksum);
            } else {
                checkDexAndSoChecksum(checksum);
            }

            AmigoClassLoader amigoClassLoader = new AmigoClassLoader(patchApks.patchPath(checksum), getRootClassLoader());
            setAPKClassLoader(amigoClassLoader);
            setDexElements(amigoClassLoader, checksum);
            setNativeLibraryDirectories(amigoClassLoader, checksum);

            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = getDeclaredMethod(AssetManager.class, "addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            addAssetPath.invoke(assetManager, patchApks.patchPath(checksum));
            setAPKResources(assetManager);

            dynamicRegisterReceivers();
            setApkInstrumentation();
            setApkHandler();

            patchedClassLoader = amigoClassLoader;

            sharedPref.edit().putString(WORKING_PATCH_APK_CHECKSUM, checksum).commit();
            clearOldPatches(checksum);

            runPatchedApplication();
        } catch (Exception e) {
            throw new LoadPatchApkException(e);
        }
    }

    private void dynamicRegisterReceivers() {
        ComponentUtils.registerNewReceivers(this);
    }

    private void setApkInstrumentation() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Instrumentation oldInstrumentation = (Instrumentation) readField(instance(), "mInstrumentation", true);
        Log.e(TAG, "oldInstrumentation--->" + oldInstrumentation);
        AmigoInstrumentation instrumentation = new AmigoInstrumentation(oldInstrumentation);
        writeField(instance(), "mInstrumentation", instrumentation, true);
        Log.e(TAG, "setApkInstrumentation success classloader-->" + instrumentation.getClass().getClassLoader());
    }

    private void setApkHandler() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Handler handler = (Handler) readField(instance(), "mH", true);
        Object callback = readField(handler, "mCallback", true);
        AmigoCallback value = new AmigoCallback(this, (Handler.Callback) callback);
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
        initAmigoSdk(originalClassLoader);
        Class acd = originalClassLoader.loadClass("me.ele.amigo.acd");
        String applicationName = (String) readStaticField(acd, "n");
        Application application = (Application) originalClassLoader.loadClass(applicationName).newInstance();
        Method attach = getDeclaredMethod(Application.class, "attach", Context.class);
        attach.setAccessible(true);
        attach.invoke(application, getBaseContext());
        setAPKApplication(application);
        application.onCreate();
    }

    private void initAmigoSdk(ClassLoader classLoader) {
        if (ProcessUtils.isMainProcess(this)) {
            try {
                String appId = getAppId();
                if (TextUtils.isEmpty(appId)) {
                    return;
                }
                Class cls = classLoader.loadClass("me.ele.amigo.sdk.AmigoSdk");
                Method initMtd = cls.getDeclaredMethod("init", Context.class, String.class);
                initMtd.setAccessible(true);
                initMtd.invoke(null, this, appId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getAppId() {
        try {
            return getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA)
                    .metaData.get("amigo_app_id").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void runPatchedApplication() throws Exception {
        setAPKClassLoader(patchedClassLoader);
        initAmigoSdk(originalClassLoader);
        Class acd = patchedClassLoader.loadClass("me.ele.amigo.acd");
        String applicationName = (String) readStaticField(acd, "n");
        Application application = (Application) patchedClassLoader.loadClass(applicationName).newInstance();
        Method attach = getDeclaredMethod(Application.class, "attach", Context.class);
        attach.setAccessible(true);
        attach.invoke(application, getBaseContext());
        setAPKApplication(application);
        application.onCreate();
    }

    private void checkDexAndSoChecksum(String apkChecksum) throws IOException, NoSuchAlgorithmException {
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

    private void setNativeLibraryDirectories(AmigoClassLoader hackClassLoader, String checksum)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        File libDir = amigoDirs.libDir(checksum);
        injectSoAtFirst(hackClassLoader, libDir.getAbsolutePath());
        libDir.setReadOnly();
        File[] libs = libDir.listFiles();
        if (libs != null && libs.length > 0) {
            for (File lib : libs) {
                lib.setReadOnly();
            }
        }
    }

    private void setAPKResources(AssetManager newAssetManager)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
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

    private void setAPKClassLoader(ClassLoader classLoader)
            throws IllegalAccessException, NoSuchMethodException, ClassNotFoundException, InvocationTargetException {
        writeField(getLoadedApk(), "mClassLoader", classLoader);
    }

    private void setDexElements(ClassLoader classLoader, String checksum) throws NoSuchFieldException, IllegalAccessException {
        Object dexPathList = getPathList(classLoader);
        File[] listFiles = amigoDirs.dexDir(checksum).listFiles();

        List<File> validDexes = new ArrayList<>();
        for (File listFile : listFiles) {
            if (listFile.getName().endsWith(".dex")) {
                validDexes.add(listFile);
            }
        }
        File[] dexes = validDexes.toArray(new File[validDexes.size()]);
        Object originDexElements = readField(dexPathList, "dexElements");
        Class<?> localClass = originDexElements.getClass().getComponentType();
        int length = dexes.length;
        Object dexElements = Array.newInstance(localClass, length);
        for (int k = 0; k < length; k++) {
            Array.set(dexElements, k, getElementWithDex(dexes[k], amigoDirs.dexOptDir(checksum)));
        }
        writeField(dexPathList, "dexElements", dexElements);
    }

    private void setAPKApplication(Application application)
            throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException {
        Object apk = getLoadedApk();
        writeField(apk, "mApplication", application);
    }

    private static Object getLoadedApk()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
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
        if (patchFile == null) {
            throw new NullPointerException("param patchFile cannot be null");
        }

        if (!patchFile.exists()) {
            throw new IllegalArgumentException("param patchFile doesn't exist");
        }

        if (!patchFile.canRead()) {
            throw new IllegalArgumentException("param patchFile cannot be read");
        }

        String patchChecksum = CrcUtils.getCrc(patchFile);
        if (!PatchApks.getInstance(context).exists(patchChecksum)) {
            copyFile(patchFile, PatchApks.getInstance(context).patchFile(patchChecksum));
        }

        AmigoService.start(context, patchChecksum, true);
    }

    public static void work(Context context, File patchFile) {
        if (patchFile == null) {
            throw new NullPointerException("param apkFile cannot be null");
        }

        if (!patchFile.exists()) {
            throw new IllegalArgumentException("param apkFile doesn't exist");
        }

        if (!patchFile.canRead()) {
            throw new IllegalArgumentException("param apkFile cannot be read");
        }

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

    public static void doClear(Context context) {
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

    private static String getWorkingPatchApkChecksum(Context ctx) {
        if (!hasWorked()) return "";
        return ctx.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS).getString(WORKING_PATCH_APK_CHECKSUM, "");
    }

    public static void clear(Context context) {
        context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS)
                .edit()
                .putString(WORKING_PATCH_APK_CHECKSUM, "")
                .commit();
    }

    private static class LoadPatchApkException extends Exception {
        public LoadPatchApkException(Throwable throwable) {
            super(throwable);
        }
    }
}
