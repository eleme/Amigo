package me.ele.amigo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.FileUtils;

public final class AmigoDirs {
    private static final String TAG = AmigoDirs.class.getSimpleName();

    private static final String CODE_CACHE_NAME = "code_cache/amigo_odex";
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
    private File odexDir;

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
        ensureAmigoDir();
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
        if (amigoDir != null && amigoDir.exists() && odexDir != null && odexDir.exists()) return;
        try {
            ApplicationInfo applicationInfo = CommonUtils.getApplicationInfo(context);
            if (applicationInfo == null) {
                // Looks like running on a test Context, so just return without initiate.
                return;
            }

            // data dir's real path
            amigoDir = new File(context.getFilesDir().getCanonicalPath(), AMIGO_FOLDER_NAME);
            amigoDir.mkdirs();

            odexDir = new File(new File(applicationInfo.dataDir).getCanonicalPath(),
                    CODE_CACHE_NAME);
            odexDir.mkdirs();
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

            File patchCacheDir = new File(odexDir, checksum);
            if (!patchCacheDir.exists()) {
                patchCacheDir.mkdirs();
            }
            optDirs.put(checksum, patchCacheDir);
        } catch (Exception e) {
            throw new RuntimeException("Initiate amigo files for patch apk: " + checksum + " " +
                    "failed (" + e.getMessage() + ").");
        }
    }

    public boolean isOptedDexExists(String checksum) {
        return dexOptDir(checksum).listFiles() != null
                && dexOptDir(checksum).listFiles().length > 0;
    }

    // delete dex, odex, so files of a patch and also clear unused patches
    public void deletePatchExceptApk(String checksum) {
        FileUtils.removeFile(odexDir);
        FileUtils.removeFile(amigoDir(), PatchApks.getInstance(context).patchFile(checksum));
        Log.d(TAG, "deletePatchExceptApk: " + checksum);
    }

    public void clear() {
        FileUtils.removeFile(amigoDir);
        FileUtils.removeFile(odexDir);
        Log.d(TAG, "clear:");
    }

    public void deleteAllPatches(String excludeFile) {
        FileUtils.removeFile(amigoDir, new File(amigoDir, excludeFile));
        FileUtils.removeFile(odexDir, new File(odexDir, excludeFile));
        Log.d(TAG, "deleteAllPatches: " + excludeFile);
    }
}
