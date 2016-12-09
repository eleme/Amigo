package me.ele.amigo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.Map;

import me.ele.amigo.utils.ArrayUtil;
import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.CrcUtils;
import me.ele.amigo.utils.PermissionChecker;

import static me.ele.amigo.utils.CrcUtils.getCrc;
import static me.ele.amigo.utils.FileUtils.copyFile;

class PatchChecker {

    private static final String TAG = PatchChecker.class.getName();

    static String checkPatchAndCopy(Context context, File patchFile, boolean checkSignature) {
        PatchChecker.checkPatchApk(context, patchFile, checkSignature);
        String patchChecksum = CrcUtils.getCrc(patchFile);
        if (!PatchApks.getInstance(context).exists(patchChecksum)) {
            copyFile(patchFile, PatchApks.getInstance(context).patchFile(patchChecksum));
        }
        return patchChecksum;
    }

    private static void checkPatchApk(Context context, File patchFile, boolean checkSignature) {
        if (patchFile == null) {
            throw new NullPointerException("param apkFile cannot be null");
        }

        if (!patchFile.exists()) {
            throw new IllegalArgumentException("param apkFile doesn't exist");
        }

        if (!patchFile.canRead()) {
            throw new IllegalArgumentException("param apkFile cannot be read");
        }

        if (!PermissionChecker.checkPatchPermission(context, patchFile)) {
            throw new IllegalStateException("patch apk cannot request more permissions than host");
        }

        if (checkSignature && !checkSignature(context, patchFile)) {
            throw new IllegalStateException("patch apk's signature is different with host");
        }
    }

    private static boolean checkSignature(Context context, File patchFile) {
        try {
            Signature appSig = CommonUtils.getSignature(context);
            Signature patchSig = CommonUtils.getSignature(context, patchFile);
            return appSig.hashCode() == patchSig.hashCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static void checkDexAndSo(Context context, String apkChecksum) throws Exception {
        Map<String, String> checksumMap = PatchInfoUtil.getPatchFileChecksum(context, apkChecksum);
        AmigoDirs amigoDirs = AmigoDirs.getInstance(context);

        File[] dexFiles = amigoDirs.dexDir(apkChecksum).listFiles();
        assertChecksum(checksumMap, dexFiles, "dex");

        File[] dexOptFiles = amigoDirs.dexOptDir(apkChecksum).listFiles();
        assertChecksum(checksumMap, dexOptFiles, "opt dex");

        File[] nativeFiles = amigoDirs.libDir(apkChecksum).listFiles();
        assertChecksum(checksumMap, nativeFiles, "native lib");
    }

    private static void assertChecksum(Map<String, String> checksumMap, File[] files,
                                       String type) {
        if (ArrayUtil.isEmpty(files)) {
            return;
        }

        for (File nativeFile : files) {
            String savedChecksum = checksumMap.get(nativeFile.getAbsolutePath());
            String checksum = getCrc(nativeFile);
            Log.e(TAG, "savedChecksum-->" + savedChecksum + ", checksum--->" + checksum);
            if (!checksum.equals(savedChecksum)) {
                throw new IllegalStateException("wrong " + type + "  check sum");
            }
        }
    }

    static boolean checkUpgrade(Context context) {
        String workingChecksum = PatchInfoUtil.getWorkingChecksum(context);
        if (TextUtils.isEmpty(workingChecksum)) {
            return true;
        }
        String patchPath = PatchApks.getInstance(context).patchPath(workingChecksum);
        PackageInfo patchInfo = context.getPackageManager().getPackageArchiveInfo(patchPath, 0);
        int patchVersion = patchInfo.versionCode;
        int hostVersion = CommonUtils.getVersionCode(context);
        return hostVersion > patchVersion;
    }
}
