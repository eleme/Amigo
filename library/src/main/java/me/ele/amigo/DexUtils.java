package me.ele.amigo;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class DexUtils {

    private static final String TAG = DexUtils.class.getSimpleName();

    public static void injectDexAtFirst(String dexPath, String defaultDexOptPath) throws NoSuchFieldException, IllegalAccessException {
        DexClassLoader dexClassLoader = new DexClassLoader(dexPath, defaultDexOptPath, dexPath, getPathClassLoader());
        Object baseDexElements = getDexElements(getPathList(getPathClassLoader()));
        Object newDexElements = getDexElements(getPathList(dexClassLoader));
        Object allDexElements = combineArray(newDexElements, baseDexElements);
        Object pathList = getPathList(getPathClassLoader());
        ReflectionUtils.setField(pathList, pathList.getClass(), "dexElements", allDexElements);
    }

    public static Object getElementWithDex(File dex, File dexOptDir) throws NoSuchFieldException, IllegalAccessException {
        DexClassLoader dexClassLoader = new DexClassLoader(dex.getAbsolutePath(), dexOptDir.getAbsolutePath(), null, getPathClassLoader());
        Object elements = getDexElements(getPathList(dexClassLoader));
        int length = Array.getLength(elements);
        if (length == 0) {
            return null;
        }
        return Array.get(elements, 0);
    }

    public static void injectSoAtFirst(ClassLoader hackClassLoader, String soPath) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        Object[] baseDexElements = getNativeLibraryDirectories(hackClassLoader);
        Object newElement;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Constructor constructor = baseDexElements[0].getClass().getConstructors()[0];
            constructor.setAccessible(true);
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] == File.class) {
                    args[i] = new File(soPath);
                } else if (parameterTypes[i] == boolean.class) {
                    args[i] = true;
                }
            }

            newElement = constructor.newInstance(args);
        } else {
            newElement = new File(soPath);
        }
        Object newDexElements = Array.newInstance(baseDexElements[0].getClass(), 1);
        Array.set(newDexElements, 0, newElement);
        Object allDexElements = combineArray(newDexElements, baseDexElements);
        Object pathList = getPathList(hackClassLoader);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ReflectionUtils.setField(pathList, pathList.getClass(), "nativeLibraryPathElements", allDexElements);
        } else {
            ReflectionUtils.setField(pathList, pathList.getClass(), "nativeLibraryDirectories", allDexElements);
        }
    }

    public static Object[] getNativeLibraryDirectories(ClassLoader hackClassLoader) throws NoSuchFieldException, IllegalAccessException {
        Object pathList = getPathList(hackClassLoader);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (Object[]) ReflectionUtils.getField(pathList, pathList.getClass(), "nativeLibraryPathElements");
        } else {
            return (Object[]) ReflectionUtils.getField(pathList, pathList.getClass(), "nativeLibraryDirectories");
        }
    }

    public static PathClassLoader getPathClassLoader() {
        PathClassLoader pathClassLoader = (PathClassLoader) DexUtils.class.getClassLoader();
        Log.e(TAG, "pathClassLoader-->" + pathClassLoader);
        return pathClassLoader;
    }

    private static Object getDexElements(Object dexPathList) throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        return ReflectionUtils.getField(dexPathList, dexPathList.getClass(), "dexElements");
    }

    public static Object getPathList(Object baseDexClassLoader) throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        return ReflectionUtils.getField(baseDexClassLoader, BaseDexClassLoader.class, "pathList");
    }

    private static Object combineArray(Object firstArray, Object secondArray) {
        Class<?> localClass = firstArray.getClass().getComponentType();
        int firstArrayLength = Array.getLength(firstArray);
        int allLength = firstArrayLength + Array.getLength(secondArray);
        Object result = Array.newInstance(localClass, allLength);
        for (int k = 0; k < allLength; ++k) {
            if (k < firstArrayLength) {
                Array.set(result, k, Array.get(firstArray, k));
            } else {
                Array.set(result, k, Array.get(secondArray, k - firstArrayLength));
            }
        }
        return result;
    }

}
