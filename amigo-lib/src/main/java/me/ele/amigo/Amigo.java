package me.ele.amigo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
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
    public static final String NEW_APK_SIG = "new_apk_sig";
    public static final String VERSION_CODE = "version_code";

    private ClassLoader originalClassLoader;
    private ClassLoader patchedClassLoader;
    private SharedPreferences sharedPref;

    private AmigoDirs amigoDirs;
    private PatchApk patchApk;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            originalClassLoader = getClassLoader();
            sharedPref = getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS);
            try {
                ensureAmigoDirs();
                if (checkUpgrade()) {
                    throw new RuntimeException("Host app has upgrade");
                }
                if (!patchApk.exists()) {
                    throw new RuntimeException("Patch apk doesn't exists");
                }
                if (!checkSignature()) {
                    throw new RuntimeException("Patch apk has illegal signature");
                }
                if (!checkVersion()) {
                    throw new RuntimeException("Patch apk version can't downgrade");
                }

            } catch (RuntimeException e) {
                e.printStackTrace();
                clear(this);
                runOriginalApplication();
                return;
            }

            if (!ProcessUtils.isMainProcess(this) && isPatchApkFirstRun()) {
                Log.e(TAG, "None main process and patch apk is not released yet");
                runOriginalApplication();
                return;
            }

            // only release loaded apk in the main process
            runPatchApk();
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

    private void runPatchApk() throws LoadPatchApkException {
        try {
            Log.e(TAG, "patchApkChecksum-->" + patchApk.checksum() + ", sig--->" + sharedPref.getString(NEW_APK_SIG, ""));
            if (isPatchApkFirstRun() || !isOptedDexExists()) {
                // TODO This is workaround for now, refactor in future.
                sharedPref.edit().remove(patchApk.checksum()).commit();
                releasePatchApk();
            } else {
                checkDexAndSoChecksum();
            }

            AmigoClassLoader amigoClassLoader = new AmigoClassLoader(patchApk.patchPath(), getRootClassLoader());
            setAPKClassLoader(amigoClassLoader);
            setDexElements(amigoClassLoader);
            setNativeLibraryDirectories(amigoClassLoader);

            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = getDeclaredMethod(AssetManager.class, "addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            addAssetPath.invoke(assetManager, patchApk.patchPath());
            setAPKResources(assetManager);

            patchedClassLoader = amigoClassLoader;
            runPatchedApplication();
        } catch (Exception e) {
            throw new LoadPatchApkException(e);
        }
    }

    private void releasePatchApk() throws Exception {
        //clear previous working dir
        clearWithoutPatch(this);

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

        ApkReleaser.getInstance(this).work(layoutId, themeId);
        Log.e(TAG, "release apk once");
    }

    private boolean isPatchApkFirstRun() {
        return !sharedPref.getString(NEW_APK_SIG, "").equals(patchApk.checksum());
    }

    private boolean isOptedDexExists() {
        if (amigoDirs.dexOptDir().listFiles() != null) {
            return amigoDirs.dexOptDir().listFiles().length > 0;
        }
        return false;
    }

    private void ensureAmigoDirs() throws Exception {
        AmigoDirs.init(this);
        amigoDirs = AmigoDirs.getInstance();
        patchApk = PatchApk.getInstance();
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
        Method attach = getDeclaredMethod(Application.class, "attach", Context.class);
        attach.setAccessible(true);
        attach.invoke(application, getBaseContext());
        setAPKApplication(application);
        application.onCreate();
    }

    private void runPatchedApplication() throws Exception {
        setAPKClassLoader(patchedClassLoader);
        Class acd = patchedClassLoader.loadClass("me.ele.amigo.acd");
        String applicationName = (String) readStaticField(acd, "n");
        Application application =
                (Application) patchedClassLoader.loadClass(applicationName).newInstance();
        Method attach = getDeclaredMethod(Application.class, "attach", Context.class);
        attach.setAccessible(true);
        attach.invoke(application, getBaseContext());
        setAPKApplication(application);
        application.onCreate();
    }

    private void checkDexAndSoChecksum() throws IOException, NoSuchAlgorithmException {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS);
        File[] dexFiles = amigoDirs.dexDir().listFiles();
        for (File dexFile : dexFiles) {
            String savedChecksum = sp.getString(dexFile.getAbsolutePath(), "");
            String checksum = getCrc(dexFile);
            if (!savedChecksum.equals(checksum)) {
                throw new IllegalStateException("wrong dex check sum");
            }
        }

        File[] dexOptFiles = amigoDirs.dexOptDir().listFiles();
        for (File dexOptFile : dexOptFiles) {
            String savedChecksum = sp.getString(dexOptFile.getAbsolutePath(), "");
            String checksum = getCrc(dexOptFile);
            Log.e(TAG, "opt dexFile-->" + dexOptFile);
            Log.e(TAG, "savedChecksum-->" + savedChecksum + ", checksum--->" + checksum);
            if (!savedChecksum.equals(checksum)) {
                throw new IllegalStateException("wrong opt dex check sum");
            }
        }

        File[] nativeFiles = amigoDirs.libDir().listFiles();
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

    private void setNativeLibraryDirectories(AmigoClassLoader hackClassLoader)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        File libDir = amigoDirs.libDir();
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

    private void setDexElements(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
        Object dexPathList = getPathList(classLoader);
        File[] listFiles = amigoDirs.dexDir().listFiles();

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
            Array.set(dexElements, k, getElementWithDex(dexes[k], amigoDirs.dexOptDir()));
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

    public static void workLater(Context context) {
        workLater(context, PatchApk.getInstance().patchFile());
    }

    public static void workLater(Context context, File patchFile) {
        if (context == null) {
            throw new NullPointerException("param context cannot be null");
        }

        if (patchFile == null) {
            throw new NullPointerException("param patchFile cannot be null");
        }

        if (!patchFile.exists()) {
            throw new IllegalArgumentException("param patchFile doesn't exist");
        }

        if (!patchFile.canRead()) {
            throw new IllegalArgumentException("param patchFile cannot be read");
        }
        if (!patchFile.getAbsolutePath().equals(PatchApk.getInstance().patchPath())) {
            copyFile(patchFile, PatchApk.getInstance().patchFile());
        }

        AmigoService.start(context, true);
    }

    public static void work(Context context) {
        work(context, PatchApk.getInstance().patchFile());
    }

    public static void work(Context context, File patchFile) {
        if (context == null) {
            throw new NullPointerException("param context cannot be null");
        }

        if (patchFile == null) {
            throw new NullPointerException("param apkFile cannot be null");
        }

        if (!patchFile.exists()) {
            throw new IllegalArgumentException("param apkFile doesn't exist");
        }

        if (!patchFile.canRead()) {
            throw new IllegalArgumentException("param apkFile cannot be read");
        }

        if (!patchFile.getAbsolutePath().equals(PatchApk.getInstance().patchPath())) {
            copyFile(patchFile, PatchApk.getInstance().patchFile());
        }

        AmigoService.start(context, false);
        System.exit(0);
        Process.killProcess(Process.myPid());
    }

    private boolean checkSignature() {
        try {
            Signature appSig = CommonUtils.getSignature(this);
            Signature patchSig = patchApk.signature(this);
            return appSig.hashCode() == patchSig.hashCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkVersion() {
        return patchApk.versionCode(this) >= CommonUtils.getVersionCode(this);
    }

    public static void clear(Context context) {
        if (context == null) {
            throw new NullPointerException("param context cannot be null");
        }

        removeFile(AmigoDirs.getInstance().amigoDir());
        context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS)
                .edit()
                .clear()
                .putInt(VERSION_CODE, CommonUtils.getVersionCode(context))
                .commit();
    }

    private void clearWithoutPatch(Context context) {
        if (context == null) {
            throw new NullPointerException("param context cannot be null");
        }

        File[] files = amigoDirs.amigoDir().listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (!file.getAbsolutePath().equals(patchApk.patchPath())) {
                    removeFile(file, false);
                }
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

    private static class LoadPatchApkException extends Exception {
        public LoadPatchApkException(Throwable throwable) {
            super(throwable);
        }
    }
}
