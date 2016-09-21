package me.ele.amigo;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.FileUtils;

public final class AmigoDirs {
    private static final String TAG = AmigoDirs.class.getSimpleName();

    private static final String CODE_CACHE_NAME = "code_cache";
    private static final String CODE_CACHE_AMIGO_DEX_FOLDER_NAME = "amigo-dexes";
    private static final String AMIGO_FOLDER_NAME = "amigo";
    private static final String AMIGO_DEX_FOLDER_NAME = "dexes";
    private static final String AMIGO_LIB_FOLDER_NAME = "libs";

    private static AmigoDirs sInstance;

    public synchronized static AmigoDirs getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AmigoDirs(context);
        }
        return sInstance;
    }

    private Context context;
    /**
     * /data/data/{package_name}/files/amigo
     */
    private File amigoDir;

    /**
     * System manages
     */
    private File cacheDir;

    /**
     * /data/data/{package_name}/code_cache/{checksum}/amigo-dexes
     */
    private Map<String, File> optDirs = new HashMap<>();

    /**
     * /data/data/{package_name}/files/amigo/{checksum}/dexes
     */
    private Map<String, File> dexDirs = new HashMap<>();

    /**
     * /data/data/{package_name}/files/amigo/{checksum}/libs
     */
    private Map<String, File> libDirs = new HashMap<>();

    private AmigoDirs(Context context) {
        this.context = context;
    }

    public File amigoDir() {
        ensureAmigoDir();
        return amigoDir;
    }

    public File dexOptDir(String checksum) {
        ensurePatchDirs(checksum);
        return optDirs.get(checksum);
    }

    public File dexDir(String checksum) {
        ensurePatchDirs(checksum);
        return dexDirs.get(checksum);
    }

    public File libDir(String checksum) {
        ensurePatchDirs(checksum);
        return libDirs.get(checksum);
    }

    private void ensureAmigoDir() throws RuntimeException {
        if (amigoDir != null && amigoDir.exists() && cacheDir != null && cacheDir.exists()) return;
        try {
            ApplicationInfo applicationInfo = CommonUtils.getApplicationInfo(context);
            if (applicationInfo == null) {
                // Looks like running on a test Context, so just return without initiate.
                return;
            }

            amigoDir = new File(context.getFilesDir(), AMIGO_FOLDER_NAME);
            FileUtils.mkdirChecked(amigoDir);

            cacheDir = new File(applicationInfo.dataDir, CODE_CACHE_NAME);
            FileUtils.mkdirChecked(cacheDir);
        } catch (Exception e) {
            throw new RuntimeException("Initiate amigo files failed (" + e.getMessage() + ").");
        }
    }

    private void ensurePatchDirs(String checksum) {
        try {
            ensureAmigoDir();
            File patchDir = new File(amigoDir, checksum);
            if (!patchDir.exists()) {
                FileUtils.mkdirChecked(patchDir);
            }

            File dexDir = new File(patchDir, AMIGO_DEX_FOLDER_NAME);
            if (!dexDir.exists()) {
                FileUtils.mkdirChecked(dexDir);
            }
            dexDirs.put(checksum, dexDir);

            File libDir = new File(patchDir, AMIGO_LIB_FOLDER_NAME);
            if (!libDir.exists()) {
                FileUtils.mkdirChecked(libDir);
            }
            libDirs.put(checksum, libDir);

            File patchCacheDir = new File(cacheDir, checksum);
            if (!patchCacheDir.exists()) {
                FileUtils.mkdirChecked(patchCacheDir);
            }
            File optDir = new File(patchCacheDir, CODE_CACHE_AMIGO_DEX_FOLDER_NAME);
            if (!optDir.exists()) {
                FileUtils.mkdirChecked(optDir);
            }
            optDirs.put(checksum, optDir);
        } catch (Exception e) {
            throw new RuntimeException("Initiate amigo files for patch apk: " + checksum + " failed (" + e.getMessage() + ").");
        }
    }
}
