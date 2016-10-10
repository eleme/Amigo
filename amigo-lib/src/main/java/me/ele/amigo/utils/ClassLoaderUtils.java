package me.ele.amigo.utils;

public class ClassLoaderUtils {

    public static ClassLoader getRootClassLoader() {
        ClassLoader rootClassLoader = null;
        ClassLoader classLoader = ClassLoaderUtils.class.getClassLoader();
        while (classLoader != null) {
            rootClassLoader = classLoader;
            classLoader = classLoader.getParent();
        }
        return rootClassLoader;
    }
}
