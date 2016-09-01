package me.ele.amigo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
import static me.ele.amigo.utils.CommonUtils.getVersionCode;
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
    private static final String VERSION_CODE = "version_code";

    private File directory;
    private File demoAPk;
    private File optimizedDir;
    private File dexDir;
    private File nativeLibraryDir;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");

        try {
            if (!ProcessUtils.isMainProcess(this)) {
                runOriginalApplication(getClassLoader());
                return;
            }

            directory = new File(getFilesDir(), "amigo");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            demoAPk = new File(directory, "demo.apk");

            optimizedDir = new File(directory, "dex_opt");
            if (!optimizedDir.exists()) {
                optimizedDir.mkdir();
            }
            dexDir = new File(directory, "dex");
            if (!dexDir.exists()) {
                dexDir.mkdir();
            }
            nativeLibraryDir = new File(directory, "lib");
            if (!nativeLibraryDir.exists()) {
                nativeLibraryDir.mkdir();
            }

            Log.e(TAG, "demoAPk.exists-->" + demoAPk.exists() + ", this--->" + this);

            ClassLoader classLoader = getClassLoader();

            SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS);

            if (checkUpgrade(sp)) {
                Log.e(TAG, "upgraded host app");
                clear(this);
                runOriginalApplication(classLoader);
                return;
            }

            if (!demoAPk.exists()) {
                Log.e(TAG, "demoApk not exist");
                clear(this);
                runOriginalApplication(classLoader);
                return;
            }

            if (!isSignatureRight(this, demoAPk)) {
                Log.e(TAG, "signature is illegal");
                clear(this);
                runOriginalApplication(classLoader);
                return;
            }

            if (!checkPatchApkVersion(this, demoAPk)) {
                Log.e(TAG, "patch apk version cannot be less than host apk");
                clear(this);
                runOriginalApplication(classLoader);
                return;
            }

            runPatchApk(sp);

        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void runPatchApk(SharedPreferences sp) throws Throwable {
        String demoApkChecksum = getCrc(demoAPk);
        boolean isFirstRun = !sp.getString(NEW_APK_SIG, "").equals(demoApkChecksum);
        Log.e(TAG, "demoApkChecksum-->" + demoApkChecksum + ", sig--->" + sp.getString(NEW_APK_SIG, ""));
        if (isFirstRun) {
            //clear previous working dir
            Amigo.clearWithoutApk(this);

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

            ApkReleaser.work(this, layoutId, themeId);
            Log.e(TAG, "release apk once");
        } else {
            checkDexAndSoChecksum();
        }

        AmigoClassLoader amigoClassLoader = new AmigoClassLoader(demoAPk.getAbsolutePath(), getRootClassLoader());
        setAPKClassLoader(amigoClassLoader);

        setDexElements(amigoClassLoader);
        setNativeLibraryDirectories(amigoClassLoader);

        AssetManager assetManager = AssetManager.class.newInstance();
        Method addAssetPath = getDeclaredMethod(AssetManager.class, "addAssetPath", String.class);
        addAssetPath.setAccessible(true);
        addAssetPath.invoke(assetManager, demoAPk.getAbsolutePath());
        setAPKResources(assetManager);

        runOriginalApplication(amigoClassLoader);
    }

    private boolean checkUpgrade(SharedPreferences sp) {
        boolean result = false;
        int recordVersion = sp.getInt(VERSION_CODE, 0);
        int currentVersion = getVersionCode(this);
        if (currentVersion > recordVersion) {
            result = true;
        }
        sp.edit().putInt(VERSION_CODE, currentVersion).commit();
        return result;
    }

    private void runOriginalApplication(ClassLoader classLoader) throws Throwable {
        Class acd = classLoader.loadClass("me.ele.amigo.acd");
        String applicationName = (String) readStaticField(acd, "n");
        Application application = (Application) classLoader.loadClass(applicationName).newInstance();
        Method attach = getDeclaredMethod(Application.class, "attach", Context.class);
        attach.setAccessible(true);
        attach.invoke(application, getBaseContext());
        setAPKApplication(application);
        application.onCreate();
    }


    private void checkDexAndSoChecksum() throws IOException, NoSuchAlgorithmException {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS);
        File[] dexFiles = dexDir.listFiles();
        for (File dexFile : dexFiles) {
            String savedChecksum = sp.getString(dexFile.getAbsolutePath(), "");
            String checksum = getCrc(dexFile);
            if (!savedChecksum.equals(checksum)) {
                crash();
            }
        }

        File[] dexOptFiles = optimizedDir.listFiles();
        for (File dexFile : dexFiles) {
            String savedChecksum = sp.getString(dexFile.getAbsolutePath(), "");
            String checksum = getCrc(dexFile);
            Log.e(TAG, "opt dexFile-->" + dexFile);
            Log.e(TAG, "savedChecksum-->" + savedChecksum + ", checksum--->" + checksum);
            if (!savedChecksum.equals(checksum)) {
                crash();
            }
        }

        File[] nativeFiles = nativeLibraryDir.listFiles();
        if (nativeFiles != null && nativeFiles.length > 0) {
            for (File nativeFile : nativeFiles) {
                String savedChecksum = sp.getString(nativeFile.getAbsolutePath(), "");
                String checksum = getCrc(nativeFile);
                if (!savedChecksum.equals(checksum)) {
                    crash();
                }
            }
        }
    }

    private void setNativeLibraryDirectories(AmigoClassLoader hackClassLoader)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        injectSoAtFirst(hackClassLoader, nativeLibraryDir.getAbsolutePath());
        nativeLibraryDir.setReadOnly();
        File[] libs = nativeLibraryDir.listFiles();
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
        File[] listFiles = dexDir.listFiles();

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
            Array.set(dexElements, k, getElementWithDex(dexes[k], optimizedDir));
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

    private static void crash() {
        String str = null;
        int a = str.length();
    }

    public static void workLater(Context context) {
        File directory = new File(context.getFilesDir(), "amigo");
        File demoAPk = new File(directory, "demo.apk");
        workLater(context, demoAPk);
    }

    public static void workLater(Context context, File apkFile) {
        if (context == null) {
            throw new NullPointerException("param context cannot be null");
        }

        if (apkFile == null) {
            throw new NullPointerException("param apkFile cannot be null");
        }

        if (!apkFile.exists()) {
            throw new IllegalArgumentException("param apkFile doesn't exist");
        }

        if (!apkFile.canRead()) {
            throw new IllegalArgumentException("param apkFile cannot be read");
        }

        if (!isSignatureRight(context, apkFile)) {
            Log.e(TAG, "no valid apk");
            return;
        }

        if (!checkPatchApkVersion(context, apkFile)) {
            Log.e(TAG, "patch apk version cannot be less than host apk");
            return;
        }

        File directory = new File(context.getFilesDir(), "amigo");
        File demoAPk = new File(directory, "demo.apk");
        if (!apkFile.getAbsolutePath().equals(demoAPk.getAbsolutePath())) {
            copyFile(apkFile, demoAPk);
        }

        AmigoService.start(context, true);
    }

    public static void work(Context context) {
        File directory = new File(context.getFilesDir(), "amigo");
        File demoAPk = new File(directory, "demo.apk");
        work(context, demoAPk);
    }

    // auto restart the whole app
    public static void work(Context context, File apkFile) {
        if (context == null) {
            throw new NullPointerException("param context cannot be null");
        }

        if (apkFile == null) {
            throw new NullPointerException("param apkFile cannot be null");
        }

        if (!apkFile.exists()) {
            throw new IllegalArgumentException("param apkFile doesn't exist");
        }

        if (!apkFile.canRead()) {
            throw new IllegalArgumentException("param apkFile cannot be read");
        }

        if (!isSignatureRight(context, apkFile)) {
            Log.e(TAG, "no valid apk");
            return;
        }

        if (!checkPatchApkVersion(context, apkFile)) {
            Log.e(TAG, "patch apk version cannot be less than host apk");
            return;
        }

        File directory = new File(context.getFilesDir(), "amigo");
        File demoAPk = new File(directory, "demo.apk");
        if (!apkFile.getAbsolutePath().equals(demoAPk.getAbsolutePath())) {
            copyFile(apkFile, demoAPk);
        }

        AmigoService.start(context, false);

        System.exit(0);
        Process.killProcess(Process.myPid());
    }

    private static boolean isSignatureRight(Context context, File apkFile) {
        try {
            Signature appSig = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures[0];
            Signature demoSig = context.getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_SIGNATURES).signatures[0];
            return appSig.hashCode() == demoSig.hashCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean checkPatchApkVersion(Context hostCtx, File apkFile) {
        return CommonUtils.getVersionCode(hostCtx, apkFile) >= getVersionCode(hostCtx);
    }

    public static void clear(Context context) {
        if (context == null) {
            throw new NullPointerException("param context cannot be null");
        }
        File directory = new File(context.getFilesDir(), "amigo");
        if (!directory.exists()) {
            return;
        }

        removeFile(directory);
        context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS)
                .edit()
                .clear()
                .putInt(VERSION_CODE, getVersionCode(context))
                .commit();
    }

    private static void clearWithoutApk(Context context) {
        if (context == null) {
            throw new NullPointerException("param context cannot be null");
        }
        File directory = new File(context.getFilesDir(), "amigo");
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        String demoApkPath = getHotfixApk(context).getAbsolutePath();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (!file.getAbsolutePath().equals(demoApkPath)) {
                    removeFile(file, false);
                }
            }
        }
    }

    public static File getHotfixApk(Context context) {
        File directory = new File(context.getFilesDir(), "amigo");
        return new File(directory, "demo.apk");
    }

    public static boolean hasWorked() {
        ClassLoader classLoader = null;
        try {
            classLoader = (ClassLoader) readField(getLoadedApk(), "mClassLoader");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return classLoader instanceof AmigoClassLoader;
    }
}
