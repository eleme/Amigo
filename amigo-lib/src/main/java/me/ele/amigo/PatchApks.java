package me.ele.amigo;

import android.content.Context;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class PatchApks {
    private static final String PATCH_APK_FILE_NAME = "patch.apk";

    private static PatchApks sInstance;

    public static PatchApks getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PatchApks.class) {
                if (sInstance == null) {
                    sInstance = new PatchApks(context);
                }
            }
        }
        return sInstance;
    }

    private Context context;
    /**
     * /data/data/{package_name}/files/amigo/{checksum}/patch.apk
     */
    private Map<String, File> apkFiles = new HashMap<>();

    private PatchApks(Context context) {
        this.context = context;
    }

    private void ensureDir(String checksum) {
        File patchDir = new File(AmigoDirs.getInstance(context).amigoDir(), checksum);
        if (!patchDir.exists()) {
            patchDir.mkdir();
        }
        apkFiles.put(checksum, new File(patchDir, PATCH_APK_FILE_NAME));
    }

    public File patchFile(String checksum) {
        ensureDir(checksum);
        return apkFiles.get(checksum);
    }

    public String patchPath(String checksum) {
        ensureDir(checksum);
        return apkFiles.get(checksum).getAbsolutePath();
    }

    public boolean exists(String checksum) {
        ensureDir(checksum);
        return apkFiles.containsKey(checksum) && apkFiles.get(checksum).exists();
    }
}
