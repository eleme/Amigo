package me.ele.amigo;

import dalvik.system.PathClassLoader;

public class AmigoClassLoader extends PathClassLoader {

    public AmigoClassLoader(String dexPath, String libraryPath, ClassLoader parent) {
        super(dexPath, libraryPath, parent);
    }

    public AmigoClassLoader(String dexPath, ClassLoader parent) {
        super(dexPath, parent);
    }
}
