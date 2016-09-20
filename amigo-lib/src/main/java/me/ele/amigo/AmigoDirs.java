package me.ele.amigo;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.io.File;

import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.FileUtils;

public final class AmigoDirs {
    private static final String CODE_CACHE_NAME = "code_cache";

    private static final String CODE_CACHE_AMIGO_DEX_FOLDER_NAME = "amigo-dexes";

    private static final String AMIGO_FOLDER_NAME = "amigo";

    private static final String AMIGO_DEX_FOLDER_NAME = "dexes";

    private static final String AMIGO_LIB_FOLDER_NAME = "libs";

    private static AmigoDirs sInstance;

    private static boolean sInitialized;

    public static void init(Context context) throws Exception {
        sInstance = new AmigoDirs(context);
        sInitialized = true;
    }

    public static AmigoDirs getInstance() {
        if (!sInitialized) {
            throw new RuntimeException("Can't get instance before initialized");
        }
        return sInstance;
    }

    /**
     * /data/data/{package_name}/files/amigo
     */
    private File amigoDir;

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

    private AmigoDirs(Context context) throws RuntimeException {
        try {
            ApplicationInfo applicationInfo = CommonUtils.getApplicationInfo(context);
            if (applicationInfo == null) {
                // Looks like running on a test Context, so just return without initiate.
                return;
            }
            amigoDir = new File(context.getFilesDir(), AMIGO_FOLDER_NAME);
            FileUtils.mkdirChecked(amigoDir);

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

    public File amigoDir() {
        return amigoDir;
    }

    public File dexOptDir() {
        return optDir;
    }

    public File dexDir() {
        return dexDir;
    }

    public File libDir() {
        return libDir;
    }
}
