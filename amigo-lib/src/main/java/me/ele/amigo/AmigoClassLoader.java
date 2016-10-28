package me.ele.amigo;

import android.content.Context;
import android.util.Log;

import java.io.File;

import dalvik.system.BaseDexClassLoader;

public class AmigoClassLoader extends BaseDexClassLoader {
    private static final String TAG = AmigoClassLoader.class.getName();

    public AmigoClassLoader(String dexPath, File optimizedDirectory, String libraryPath,
                            ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
    }

    public static AmigoClassLoader newInstance(Context context, String checksum) {
        return new AmigoClassLoader(getDexPath(context, checksum),
                AmigoDirs.getInstance(context).dexOptDir(checksum),
                getLibraryPath(context, checksum),
                AmigoClassLoader.class.getClassLoader().getParent());
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

    private static String getLibraryPath(Context context, String checksum) {
        return AmigoDirs.getInstance(context).libDir(checksum).getAbsolutePath();
    }

    private static String joinPath(File folder) {
        StringBuilder path = new StringBuilder();
        File[] libFiles = folder.listFiles();
        if (libFiles == null || libFiles.length == 0) {
            return null;
        }

        for (File libFile : libFiles) {
            path.append(File.pathSeparatorChar);
            path.append(libFile.getAbsolutePath());
        }
        return path.toString();
    }

    private static String getDexPath(Context context, String checksum)
            throws IllegalStateException {
        String dexPath = joinPath(AmigoDirs.getInstance(context).dexDir(checksum));
        if (dexPath == null) {
            throw new IllegalStateException("Amigo: no patch dex available");
        }
        return dexPath;
    }

}
