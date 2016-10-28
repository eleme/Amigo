package me.ele.amigo;

import android.util.Log;

import java.io.File;

import dalvik.system.BaseDexClassLoader;

public class AmigoClassLoader extends BaseDexClassLoader {
    private static final String TAG = AmigoClassLoader.class.getName();

    public AmigoClassLoader(String dexPath, File optimizedDirectory, String libraryPath,
                            ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            if (!name.startsWith("me.ele.amigo")) {
                throw e;
            }
            Log.d(TAG, "loadClass: can't find amigo-lib in the patch apk, trying to load ["
                    + name + "] from original classloader");
            return AmigoClassLoader.class.getClassLoader().loadClass(name);
        }
    }
}
