package me.ele.amigo;

import android.app.Application;
import android.content.Context;
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

import static me.ele.amigo.DexUtils.getRootClassLoader;
import static me.ele.amigo.ReflectionUtils.getField;
import static me.ele.amigo.ReflectionUtils.getMethod;

public class FakeApplication extends Application {

    private static final String TAG = FakeApplication.class.getSimpleName();

    private Application application;

    private File directory;
    private File hackApk;
    private File optimizedDir;
    private File dexDir;
    private File nativeLibraryDir;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.e(TAG, "attachBaseContext: ");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");

        //// TODO: 16/8/8 hack ClassLoader
        directory = new File(getFilesDir(), "amigo");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        hackApk = new File(directory, "demo.apk");
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
            Log.e(TAG, "hackApk.exists-->" + hackApk.exists());
            if (hackApk.exists()) {

                DexReleaser.release(hackApk.getAbsolutePath(), dexDir.getAbsolutePath());
                NativeLibraryHelperCompat.copyNativeBinaries(hackApk, nativeLibraryDir);

                AmigoClassLoader hackClassLoader = new AmigoClassLoader(hackApk.getAbsolutePath(), getRootClassLoader());
                setAPKClassLoader(hackClassLoader);

                setNativeLibraryDirectories(hackClassLoader);

                AssetManager assetManager = AssetManager.class.newInstance();
                Method addAssetPath = getMethod(AssetManager.class, "addAssetPath", String.class);
                addAssetPath.setAccessible(true);
                addAssetPath.invoke(assetManager, hackApk.getAbsolutePath());
                setAPKResources(assetManager);
            }

            application = (Application) Class.forName("me.ele.amigo.demo.ApplicationContext").newInstance();
            Method attach = getMethod(Application.class, "attach", Context.class);
            attach.setAccessible(true);
            attach.invoke(application, getBaseContext());
            setAPKApplication(application);

            for (Object o : DexUtils.getNativeLibraryDirectories(getClassLoader())) {
                Log.e(TAG, "native-->" + o);
            }

            Object dexPathList = DexUtils.getPathList(getClassLoader());
            Object[] dexElements = (Object[]) ReflectionUtils.getField(dexPathList, "dexElements");
            for (Object dexElement : dexElements) {
                Log.e(TAG, "file-->" + ReflectionUtils.getField(dexElement, "file"));
                Log.e(TAG, "zip-->" + ReflectionUtils.getField(dexElement, "zip"));
                Log.e(TAG, "dexFile-->" + ReflectionUtils.getField(dexElement, "dexFile"));
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        if (application != null) {
            application.onCreate();
        }
    }

    private void setNativeLibraryDirectories(AmigoClassLoader hackClassLoader) {
        try {
            DexUtils.injectSoAtFirst(hackClassLoader, nativeLibraryDir.getAbsolutePath());
            nativeLibraryDir.setReadOnly();
            File[] libs = nativeLibraryDir.listFiles();
            if (libs != null && libs.length > 0) {
                for (File lib : libs) {
                    lib.setReadOnly();
                }
            }

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    private void setAPKResources(AssetManager newAssetManager) {
        try {
            Method mEnsureStringBlocks = AssetManager.class.getDeclaredMethod("ensureStringBlocks", new Class[0]);
            mEnsureStringBlocks.setAccessible(true);
            mEnsureStringBlocks.invoke(newAssetManager, new Object[0]);

            Collection<WeakReference<Resources>> references;

            if (Build.VERSION.SDK_INT >= 19) {
                Class<?> resourcesManagerClass = Class.forName("android.app.ResourcesManager");
                Method mGetInstance = resourcesManagerClass.getDeclaredMethod("getInstance", new Class[0]);
                mGetInstance.setAccessible(true);
                Object resourcesManager = mGetInstance.invoke(null, new Object[0]);
                try {
                    Field fMActiveResources = resourcesManagerClass.getDeclaredField("mActiveResources");
                    fMActiveResources.setAccessible(true);

                    ArrayMap<?, WeakReference<Resources>> arrayMap = (ArrayMap) fMActiveResources.get(resourcesManager);
                    references = arrayMap.values();
                } catch (NoSuchFieldException ignore) {
                    Field mResourceReferences = resourcesManagerClass.getDeclaredField("mResourceReferences");
                    mResourceReferences.setAccessible(true);

                    references = (Collection) mResourceReferences.get(resourcesManager);
                }
            } else {
                Field fMActiveResources = ActivityThreadCompat.clazz().getDeclaredField("mActiveResources");
                fMActiveResources.setAccessible(true);
                Object thread = ActivityThreadCompat.instance();

                HashMap<?, WeakReference<Resources>> map = (HashMap) fMActiveResources.get(thread);

                references = map.values();
            }

            for (WeakReference<Resources> wr : references) {
                Resources resources = wr.get();
                if (resources == null) continue;

                try {
                    Field mAssets = Resources.class.getDeclaredField("mAssets");
                    mAssets.setAccessible(true);
                    mAssets.set(resources, newAssetManager);
                } catch (Throwable ignore) {
                    Field mResourcesImpl = Resources.class.getDeclaredField("mResourcesImpl");
                    mResourcesImpl.setAccessible(true);
                    Object resourceImpl = mResourcesImpl.get(resources);
                    Field implAssets = resourceImpl.getClass().getDeclaredField("mAssets");
                    implAssets.setAccessible(true);
                    implAssets.set(resourceImpl, newAssetManager);
                }

                resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
            }

            if (Build.VERSION.SDK_INT >= 21) {
                for (WeakReference<Resources> wr : references) {
                    Resources resources = wr.get();
                    if (resources == null) continue;

                    // android.util.Pools$SynchronizedPool<TypedArray>
                    Field mTypedArrayPool = Resources.class.getDeclaredField("mTypedArrayPool");
                    mTypedArrayPool.setAccessible(true);
                    Object typedArrayPool = mTypedArrayPool.get(resources);
                    // Clear all the pools
                    Method acquire = typedArrayPool.getClass().getMethod("acquire");
                    acquire.setAccessible(true);
                    while (acquire.invoke(typedArrayPool) != null) ;
                }
            }
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }


    private void setAPKClassLoader(ClassLoader classLoader) {
        try {
            Object apk = getLoadedApk();
            Class apkClass = apk.getClass();
            Field mClassLoader = getField(apkClass, "mClassLoader");
            mClassLoader.setAccessible(true);
            mClassLoader.set(apk, classLoader);


            Object dexPathList = DexUtils.getPathList(getClassLoader());
            File[] listFiles = dexDir.listFiles();

            List<File> validDexes = new ArrayList<>();
            for (File listFile : listFiles) {
                if (listFile.getName().endsWith(".dex")) {
                    validDexes.add(listFile);
                }
            }
            File[] dexes = validDexes.toArray(new File[validDexes.size()]);
            Object originDexElements = ReflectionUtils.getField(dexPathList, "dexElements");
            Class<?> localClass = originDexElements.getClass().getComponentType();
            int length = dexes.length;
            Object dexElements = Array.newInstance(localClass, length);
            for (int k = 0; k < length; k++) {
                Array.set(dexElements, k, DexUtils.getElementWithDex(dexes[k], optimizedDir));
            }
            ReflectionUtils.setField(dexPathList, "dexElements", dexElements);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private void setAPKApplication(Application application) {
        try {
            Object apk = getLoadedApk();
            Class apkClass = apk.getClass();
            Field mApplication = getField(apkClass, "mApplication");
            mApplication.setAccessible(true);
            mApplication.set(apk, application);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private Object getLoadedApk() throws IllegalAccessException {
        Field mLoadedApk = getField(Application.class, "mLoadedApk");
        mLoadedApk.setAccessible(true);
        return mLoadedApk.get(this);
    }


}
