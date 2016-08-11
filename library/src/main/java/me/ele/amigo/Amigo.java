package me.ele.amigo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.ele.amigo.utils.MD5;

import static me.ele.amigo.compat.ActivityThreadCompat.instance;
import static me.ele.amigo.compat.NativeLibraryHelperCompat.copyNativeBinaries;
import static me.ele.amigo.reflect.FieldUtils.getField;
import static me.ele.amigo.reflect.FieldUtils.readField;
import static me.ele.amigo.reflect.FieldUtils.readStaticField;
import static me.ele.amigo.reflect.FieldUtils.writeField;
import static me.ele.amigo.reflect.MethodUtils.getDeclaredMethod;
import static me.ele.amigo.reflect.MethodUtils.invokeMethod;
import static me.ele.amigo.reflect.MethodUtils.invokeStaticMethod;
import static me.ele.amigo.utils.DexReleaser.releaseDexes;
import static me.ele.amigo.utils.DexUtils.getElementWithDex;
import static me.ele.amigo.utils.DexUtils.getNativeLibraryDirectories;
import static me.ele.amigo.utils.DexUtils.getPathList;
import static me.ele.amigo.utils.DexUtils.getRootClassLoader;
import static me.ele.amigo.utils.DexUtils.injectSoAtFirst;

public class Amigo extends Application {

    private static final String TAG = Amigo.class.getSimpleName();

    private static final String SP_NAME = "Amigo";
    private static final String NEW_APK_SIG = "new_apk_sig";

    private File directory;
    private File demoAPk;
    private File optimizedDir;
    private File dexDir;
    private File nativeLibraryDir;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");

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

        try {

            AmigoClassLoader amigoClassLoader = null;
            Log.e(TAG, "demoAPk.exists-->" + demoAPk.exists());
            if (demoAPk.exists() && isSignatureRight()) {
                SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
                String demoApkChecksum = MD5.checksum(demoAPk);
                boolean isFirstRun = !sp.getString(NEW_APK_SIG, "").equals(demoApkChecksum);
                if (isFirstRun) {
                    releaseDexes(demoAPk.getAbsolutePath(), dexDir.getAbsolutePath());
                    copyNativeBinaries(demoAPk, nativeLibraryDir);

                    sp.edit().putString(NEW_APK_SIG, demoApkChecksum).commit();
                    saveDexAndSoChecksum();
                    Log.e(TAG, "release apk once");
                } else {
                    checkDexAndSoChecksum();
                }

                amigoClassLoader = new AmigoClassLoader(demoAPk.getAbsolutePath(), getRootClassLoader());
                setAPKClassLoader(amigoClassLoader);

                setDexElements(amigoClassLoader);
                if (isFirstRun) {
                    saveDexOptChecksum();
                } else {
                    checkDexOptChecksum();
                }
                setNativeLibraryDirectories(amigoClassLoader);

                AssetManager assetManager = AssetManager.class.newInstance();
                Method addAssetPath = getDeclaredMethod(AssetManager.class, "addAssetPath", String.class);
                addAssetPath.setAccessible(true);
                addAssetPath.invoke(assetManager, demoAPk.getAbsolutePath());
                setAPKResources(assetManager);
            }


            Class acd = Class.forName(getPackageName() + ".acd");
            String applicationName = (String) readStaticField(acd, "n");
            Application application = (Application) Class.forName(applicationName).newInstance();
            Method attach = getDeclaredMethod(Application.class, "attach", Context.class);
            attach.setAccessible(true);
            attach.invoke(application, getBaseContext());
            setAPKApplication(application);

            if (amigoClassLoader != null) {
                for (Object o : getNativeLibraryDirectories(amigoClassLoader)) {
                    Log.e(TAG, "native-->" + o);
                }

                Object dexPathList = getPathList(amigoClassLoader);
                Object[] dexElements = (Object[]) readField(dexPathList, "dexElements");
                for (Object dexElement : dexElements) {
                    Log.e(TAG, "file-->" + readField(dexElement, "file"));
                    Log.e(TAG, "zip-->" + readField(dexElement, "zip"));
                    Log.e(TAG, "dexFile-->" + readField(dexElement, "dexFile"));
                }
            }
            application.onCreate();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveDexOptChecksum() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        File[] dexFiles = optimizedDir.listFiles();
        for (File dexFile : dexFiles) {
            String checksum = MD5.checksum(dexFile);
            sp.edit().putString(dexFile.getAbsolutePath(), checksum).commit();
        }
    }

    private void checkDexOptChecksum() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        File[] dexFiles = optimizedDir.listFiles();
        for (File dexFile : dexFiles) {
            String savedChecksum = sp.getString(dexFile.getAbsolutePath(), "");
            String checksum = MD5.checksum(dexFile);
            if (!savedChecksum.equals(checksum)) {
                crash();
            }
        }
    }

    private void saveDexAndSoChecksum() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        File[] dexFiles = dexDir.listFiles();
        for (File dexFile : dexFiles) {
            String checksum = MD5.checksum(dexFile);
            sp.edit().putString(dexFile.getAbsolutePath(), checksum).commit();
        }

        File[] nativeFiles = nativeLibraryDir.listFiles();
        if (nativeFiles != null && nativeFiles.length > 0) {
            for (File nativeFile : nativeFiles) {
                String checksum = MD5.checksum(nativeFile);
                sp.edit().putString(nativeFile.getAbsolutePath(), checksum).commit();
            }
        }
    }

    private void checkDexAndSoChecksum() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        File[] dexFiles = dexDir.listFiles();
        for (File dexFile : dexFiles) {
            String savedChecksum = sp.getString(dexFile.getAbsolutePath(), "");
            String checksum = MD5.checksum(dexFile);
            if (!savedChecksum.equals(checksum)) {
                crash();
            }
        }

        File[] nativeFiles = nativeLibraryDir.listFiles();
        if (nativeFiles != null && nativeFiles.length > 0) {
            for (File nativeFile : nativeFiles) {
                String savedChecksum = sp.getString(nativeFile.getAbsolutePath(), "");
                String checksum = MD5.checksum(nativeFile);
                if (!savedChecksum.equals(checksum)) {
                    crash();
                }
            }
        }
    }

    private void crash() {
        String str = null;
        int a = str.length();
    }

    private boolean isSignatureRight() {
        try {
            Signature appSig = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES).signatures[0];
            Signature demoSig = getPackageManager().getPackageArchiveInfo(demoAPk.getAbsolutePath(), PackageManager.GET_SIGNATURES).signatures[0];
            return appSig.hashCode() == demoSig.hashCode();
        } catch (Exception e) {
            e.printStackTrace();
        }

        crash();

        return false;
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
            throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
        Object apk = getLoadedApk();
        Class apkClass = apk.getClass();
        Field mClassLoader = getField(apkClass, "mClassLoader");
        mClassLoader.setAccessible(true);
        mClassLoader.set(apk, classLoader);
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
        Class apkClass = apk.getClass();
        Field mApplication = getField(apkClass, "mApplication");
        mApplication.setAccessible(true);
        mApplication.set(apk, application);
    }

    private Object getLoadedApk()
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

    public static void clear(Context context) {
        if (context == null) {
            throw new NullPointerException("param context cannot be null");
        }
        File directory = new File(context.getFilesDir(), "amigo");
        if (!directory.exists()) {
            return;
        }

        deleteFile(directory);
    }

    private static void deleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        file.setWritable(true);

        if (file.isFile()) {
            file.delete();
            return;
        }

        if (file.isDirectory()) {

            File[] listFiles = file.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                for (File f : listFiles) {
                    deleteFile(f);
                }
            }
            file.delete();
        }
    }
}
