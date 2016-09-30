package me.ele.amigo;

import java.io.File;

import dalvik.system.BaseDexClassLoader;

public class AmigoClassLoader extends BaseDexClassLoader {

    public AmigoClassLoader(String dexPath, File optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
    }

}
