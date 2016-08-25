package me.ele.amigo.utils;

import android.os.Build;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

import static me.ele.amigo.reflect.FieldUtils.readField;
import static me.ele.amigo.reflect.FieldUtils.writeField;

public class DexUtils {

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
            writeField(pathList, "nativeLibraryPathElements", allDexElements);
        } else {
            writeField(pathList, "nativeLibraryDirectories", allDexElements);
        }
    }

    public static Object[] getNativeLibraryDirectories(ClassLoader hackClassLoader) throws NoSuchFieldException, IllegalAccessException {
        Object pathList = getPathList(hackClassLoader);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (Object[]) readField(pathList, "nativeLibraryPathElements");
        } else {
            return (Object[]) readField(pathList, "nativeLibraryDirectories");
        }
    }

    public static PathClassLoader getPathClassLoader() {
        PathClassLoader pathClassLoader = (PathClassLoader) DexUtils.class.getClassLoader();
        return pathClassLoader;
    }

    public static ClassLoader getRootClassLoader() {
        ClassLoader rootClassLoader = null;
        ClassLoader classLoader = DexUtils.class.getClassLoader();
        while (classLoader != null) {
            rootClassLoader = classLoader;
            classLoader = classLoader.getParent();
        }
        return rootClassLoader;
    }

    private static Object getDexElements(Object dexPathList) throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        return readField(dexPathList, "dexElements");
    }

    public static Object getPathList(Object baseDexClassLoader) throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        return readField(baseDexClassLoader, "pathList");
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
