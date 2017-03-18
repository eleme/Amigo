package me.ele.amigo;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;

public class AmigoClassLoader extends DexClassLoader {
    private static final String TAG = AmigoClassLoader.class.getName();

    private ZipFile zipFile;
    private File patchApk;

    public AmigoClassLoader(String patchApkPath, String dexPath, String optimizedDirectory, String
            libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
        try {
            patchApk = new File(patchApkPath);
            zipFile = new ZipFile(patchApkPath);
        } catch (IOException e) {
            e.printStackTrace();
            zipFile = null;
        }
    }

    public static AmigoClassLoader newInstance(Context context, String checksum) {
        return new AmigoClassLoader(PatchApks.getInstance(context).patchPath(checksum),
                getDexPath(context, checksum),
                AmigoDirs.getInstance(context).dexOptDir(checksum).getAbsolutePath(),
                getLibraryPath(context, checksum),
                AmigoClassLoader.class.getClassLoader().getParent());
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

    private static String getDexPath(Context context, String checksum) {
        if (Build.VERSION.SDK_INT >= 21) {
            return PatchApks.getInstance(context).patchPath(checksum);
        }

        String dexPath = joinPath(AmigoDirs.getInstance(context).dexDir(checksum));
        return dexPath != null ? dexPath : PatchApks.getInstance(context).patchPath(checksum);
    }

    @Override
    protected URL findResource(String name) {
        if ((zipFile == null) || (zipFile.getEntry(name) == null)) {
            return null;
        }

        try {
            /*
             * File.toURL() is compliant with RFC 1738 in
             * always creating absolute path names. If we
             * construct the URL by concatenating strings, we
             * might end up with illegal URLs for relative
             * names.
             */
            return new URL("jar:" + patchApk.toURL() + "!/" + name);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
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
