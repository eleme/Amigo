package me.ele.amigo;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.io.File;

import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.FileUtils;

public class AmigoFiles {
    private static final String CODE_CACHE_NAME = "code_cache";

    private static final String CODE_CACHE_AMIGO_DEX_FOLDER_NAME = "amigo-dexes";

    private static final String AMIGO_FOLDER_NAME = "amigo";

    private static final String AMIGO_APK_FILE_NAME = "patch.apk";

    private static final String AMIGO_DEX_FOLDER_NAME = "dexes";

    private static final String AMIGO_LIB_FOLDER_NAME = "libs";

    private static AmigoFiles sInstance;

    public static AmigoFiles getInstance(Context context) throws RuntimeException {
        if (sInstance != null) {
            synchronized (AmigoFiles.class) {
                if (sInstance == null) {
                    sInstance = new AmigoFiles(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * /data/data/{package_name}/files/amigo
     */
    private File amigoDir;

    /**
     * /data/data/{package_name}/files/amigo/patch.apk
     */
    private File apkFile;

    /**
     * /data/data/{package_name}/code_cache/amigo-dexes
     */
    private File optDir;

    /**
     * /data/data/{package_name}/files/amigo/dexes
     */
    private File dexDir;

    /**
     * /data/data/{package_name}/files/amigo/libs
     */
    private File libDir;

    private AmigoFiles(Context context) throws RuntimeException {
        try {
            ApplicationInfo applicationInfo = CommonUtils.getApplicationInfo(context);
            if (applicationInfo == null) {
                // Looks like running on a test Context, so just return without initiate.
                return;
            }
            amigoDir = new File(context.getFilesDir(), AMIGO_FOLDER_NAME);
            FileUtils.mkdirChecked(amigoDir);

            apkFile = new File(amigoDir, AMIGO_APK_FILE_NAME);

            dexDir = new File(amigoDir, AMIGO_DEX_FOLDER_NAME);
            libDir = new File(amigoDir, AMIGO_LIB_FOLDER_NAME);
            FileUtils.mkdirChecked(dexDir);
            FileUtils.mkdirChecked(libDir);

            File cacheDir = new File(applicationInfo.dataDir, CODE_CACHE_NAME);
            FileUtils.mkdirChecked(cacheDir);
            optDir = new File(cacheDir, CODE_CACHE_AMIGO_DEX_FOLDER_NAME);
            FileUtils.mkdirChecked(optDir);
        } catch (Exception e) {
            throw new RuntimeException("Initiate amigo files failed (" + e.getMessage() + ").");
        }
    }

    public File getAmigoDir() {
        return amigoDir;
    }

    public File getPatchApk() {
        return apkFile;
    }

    public File getDexOptDir() {
        return optDir;
    }

    public File getDexDir() {
        return dexDir;
    }

    public File getLibDir() {
        return libDir;
    }
}
