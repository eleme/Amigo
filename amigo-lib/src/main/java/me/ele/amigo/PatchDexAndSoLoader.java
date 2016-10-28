package me.ele.amigo;

import android.content.Context;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;

import static me.ele.amigo.compat.ActivityThreadCompat.instance;
import static me.ele.amigo.reflect.FieldUtils.readField;
import static me.ele.amigo.reflect.FieldUtils.writeField;

class PatchDexAndSoLoader {

    static void loadPatchDexAndSo(Context context, String checksum) throws Exception {
        AmigoClassLoader amigoClassLoader = new AmigoClassLoader(getDexPath(context, checksum),
                AmigoDirs.getInstance(context).dexOptDir(checksum),
                getLibraryPath(context, checksum),
                PatchDexAndSoLoader.class.getClassLoader().getParent());
        writeField(getLoadedApk(), "mClassLoader", amigoClassLoader);
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

    private static Object getLoadedApk() throws Exception {
        Map<String, WeakReference<Object>> mPackages =
                (Map<String, WeakReference<Object>>) readField(instance(), "mPackages", true);
        for (String s : mPackages.keySet()) {
            WeakReference wr = mPackages.get(s);
            if (wr != null && wr.get() != null) {
                return wr.get();
            }
        }
        return null;
    }
}
