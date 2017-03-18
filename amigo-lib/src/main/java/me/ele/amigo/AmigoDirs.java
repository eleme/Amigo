package me.ele.amigo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;
import java.io.File;
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
    private static final String PATCH_INFO_FILE_NAME = "amigo.json";

    private static AmigoDirs sInstance;
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

    public synchronized static AmigoDirs getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AmigoDirs(context);
        }
        return sInstance;
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

    public File patchInfoFile() {
        ensureAmigoDir();
        return new File(amigoDir, PATCH_INFO_FILE_NAME);
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
            // NOTE: use adb install xxx.apk on Android N would cause the code_cache dir not
            // writable, refer to https://code.google.com/p/android/issues/detail?id=225735
            odexDir.mkdirs();
            if (!odexDir.canRead() || !odexDir.canWrite()) {
                throw new RuntimeException("do not have access to " + odexDir);
            }
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
        File[] odexes = dexOptDir(checksum).listFiles();
        int odexFilesLength;
        if (odexes == null
                || (odexFilesLength = odexes.length) == 0) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= 21) {
            return odexFilesLength == 1 && odexes[0].exists();
        }

        File[] dexFiles = dexDir(checksum).listFiles();
        int dexFilesLength;
        if (dexFiles == null || (dexFilesLength = dexFiles.length) != odexFilesLength) {
            return false;
        }

        for (int i = 0; i < dexFilesLength; i++) {
            if (!dexFiles[i].exists() || !odexes[i].exists()) {
                return false;
            }
        }

        return true;
    }

    // delete dex, odex, so files of a patch and also clear unused patches
    public void deletePatchExceptApk(String checksum) {
        FileUtils.removeFile(odexDir);
        clearAmigoDir(PatchApks.getInstance(context).patchFile(checksum));
        Log.d(TAG, "deletePatchExceptApk: " + checksum);
    }

    public void clear() {
        FileUtils.removeFile(amigoDir);
        FileUtils.removeFile(odexDir);
        Log.d(TAG, "clear:");
    }

    public void deleteAllPatches(String excludeChecksum) {
        FileUtils.removeFile(odexDir, new File(odexDir, excludeChecksum));
        File excludeDir = new File(amigoDir, excludeChecksum);
        clearAmigoDir(excludeDir);
        Log.d(TAG, "deleted old patches");
    }

    private void clearAmigoDir(File excludeFile) {
        File[] subFiles = amigoDir.listFiles();
        if (subFiles == null) {
            return;
        }

        //always keep amigo.json
        File patchInfoFile = patchInfoFile();
        for (File subFile : subFiles) {
            if (!subFile.equals(patchInfoFile) && !subFile.equals(excludeFile)) {
                FileUtils.removeFile(subFile, excludeFile);
            }
        }
    }
}
