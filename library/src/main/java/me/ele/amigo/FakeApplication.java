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
        nativeLibraryDir = new File(dexDir.getAbsolutePath() + "/lib/" + Build.CPU_ABI);
        if (!nativeLibraryDir.exists()) {
            nativeLibraryDir.mkdirs();
        }

        try {
            Log.e(TAG, "hackApk.exists-->" + hackApk.exists());
            if (hackApk.exists()) {

                SoReleaser.release(hackApk.getAbsolutePath(), dexDir.getAbsolutePath());

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
            Object[] dexElements = (Object[]) ReflectionUtils.getField(dexPathList, dexPathList.getClass(), "dexElements");
            for (Object dexElement : dexElements) {
                Log.e(TAG, "file-->" + ReflectionUtils.getField(dexElement, dexElement.getClass(), "file"));
                Log.e(TAG, "zip-->" + ReflectionUtils.getField(dexElement, dexElement.getClass(), "zip"));
                Log.e(TAG, "dexFile-->" + ReflectionUtils.getField(dexElement, dexElement.getClass(), "dexFile"));
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
                Class<?> activityThread = Class.forName("android.app.ActivityThread");
                Field fMActiveResources = activityThread.getDeclaredField("mActiveResources");
                fMActiveResources.setAccessible(true);
                Object thread = getActivityThread(activityThread);

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

    private Object getActivityThread(Class<?> activityThread) {
        try {
            // ActivityThread.currentActivityThread()
            Method m = activityThread.getMethod("currentActivityThread", new Class[0]);
            m.setAccessible(true);
            Object thread = m.invoke(null, new Object[0]);
            if (thread != null) return thread;

            // context.@mLoadedApk.@mActivityThread
            Field mLoadedApk = getClass().getField("mLoadedApk");
            mLoadedApk.setAccessible(true);
            Object apk = mLoadedApk.get(this);
            Field mActivityThreadField = apk.getClass().getDeclaredField("mActivityThread");
            mActivityThreadField.setAccessible(true);
            return mActivityThreadField.get(apk);
        } catch (Throwable ignore) {
        }

        return null;
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
            Object originDexElements = ReflectionUtils.getField(dexPathList, dexPathList.getClass(), "dexElements");
            Class<?> localClass = originDexElements.getClass().getComponentType();
            int length = dexes.length;
            Object dexElements = Array.newInstance(localClass, length);
            for (int k = 0; k < length; k++) {
                Array.set(dexElements, k, DexUtils.getElementWithDex(dexes[k], optimizedDir));
            }
            ReflectionUtils.setField(dexPathList, dexPathList.getClass(), "dexElements", dexElements);
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

    private Field getField(Class<?> clazz, String fieldName) {
        Field field = null;

        while (field == null) {
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }

            if (clazz == Object.class) {
                break;
            }
        }

        return field;
    }

    private Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Method method = null;

        while (method == null) {
            try {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
                break;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }

            if (clazz == Object.class) {
                break;
            }
        }
        return method;
    }

    private ClassLoader getRootClassLoader() {
        ClassLoader rootClassLoader = null;
        ClassLoader classLoader = getClassLoader();
        while (classLoader != null) {
            rootClassLoader = classLoader;
            classLoader = classLoader.getParent();
        }
        return rootClassLoader;
    }

}
