package me.ele.amigo;

import android.content.Context;
import android.content.pm.Signature;

import java.io.File;

import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.CrcUtils;

public final class PatchApk {
    private static final String PATCH_APK_FILE_NAME = "patch.apk";

    private static PatchApk sInstance;

    public static PatchApk getInstance() {
        if (sInstance == null) {
            synchronized (PatchApk.class) {
                if (sInstance == null) {
                    sInstance = new PatchApk();
                }
            }
        }
        return sInstance;
    }

    /**
     * /data/data/{package_name}/files/amigo/patch.apk
     */
    private File apkFile;

    private PatchApk() {
        apkFile = new File(AmigoDirs.getInstance().amigoDir(), PATCH_APK_FILE_NAME);
    }

    public File patchFile() {
        return apkFile;
    }

    public String patchPath() {
        return apkFile.getAbsolutePath();
    }

    public boolean exists() {
        return apkFile.exists();
    }

    public Signature signature(Context context) {
        return CommonUtils.getSignature(context, apkFile);
    }

    public int versionCode(Context context) {
        return CommonUtils.getVersionCode(context, apkFile);
    }

    public String checksum() {
        return CrcUtils.getCrc(apkFile);
    }
}
