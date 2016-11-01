package me.ele.amigo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.Signature;
import android.util.Log;

import java.io.File;

import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.CrcUtils;
import me.ele.amigo.utils.PermissionChecker;

import static android.content.Context.MODE_MULTI_PROCESS;
import static me.ele.amigo.Amigo.SP_NAME;
import static me.ele.amigo.Amigo.VERSION_CODE;
import static me.ele.amigo.utils.CrcUtils.getCrc;
import static me.ele.amigo.utils.FileUtils.copyFile;

class PatchChecker {

    private static final String TAG = PatchChecker.class.getName();

    static boolean checkUpgrade(Context context) {
        boolean result = false;
        SharedPreferences sharedPref = context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS);
        int recordVersion = sharedPref.getInt(VERSION_CODE, 0);
        int currentVersion = CommonUtils.getVersionCode(context);
        if (currentVersion > recordVersion) {
            result = true;
        }
        sharedPref.edit().putInt(VERSION_CODE, currentVersion).commit();
        return result;
    }

    static String checkPatchAndCopy(Context context, File patchFile) {
        PatchChecker.checkPatchApk(context, patchFile);
        String patchChecksum = CrcUtils.getCrc(patchFile);
        if (!PatchApks.getInstance(context).exists(patchChecksum)) {
            copyFile(patchFile, PatchApks.getInstance(context).patchFile(patchChecksum));
        }
        return patchChecksum;
    }

    private static void checkPatchApk(Context context, File patchFile) {
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

        if (!checkSignature(context, patchFile)) {
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
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS);
        AmigoDirs amigoDirs = AmigoDirs.getInstance(context);
        File[] dexFiles = amigoDirs.dexDir(apkChecksum).listFiles();

        for (File dexFile : dexFiles) {
            String savedChecksum = sp.getString(dexFile.getAbsolutePath(), "");
            String checksum = getCrc(dexFile);
            Log.e(TAG, "dexFile-->" + dexFile);
            Log.e(TAG, "savedChecksum-->" + savedChecksum + ", checksum--->" + checksum);
            if (!savedChecksum.equals(checksum)) {
                throw new IllegalStateException("wrong dex check sum");
            }
        }

        File[] dexOptFiles = amigoDirs.dexOptDir(apkChecksum).listFiles();
        for (File dexOptFile : dexOptFiles) {
            String savedChecksum = sp.getString(dexOptFile.getAbsolutePath(), "");
            String checksum = getCrc(dexOptFile);
            Log.e(TAG, "opt dexFile-->" + dexOptFile);
            Log.e(TAG, "savedChecksum-->" + savedChecksum + ", checksum--->" + checksum);
            if (!savedChecksum.equals(checksum)) {
                throw new IllegalStateException("wrong opt dex check sum");
            }
        }

        File[] nativeFiles = amigoDirs.libDir(apkChecksum).listFiles();
        if (nativeFiles != null && nativeFiles.length > 0) {
            for (File nativeFile : nativeFiles) {
                String savedChecksum = sp.getString(nativeFile.getAbsolutePath(), "");
                String checksum = getCrc(nativeFile);
                Log.e(TAG, "native lib -->" + nativeFile);
                Log.e(TAG, "savedChecksum-->" + savedChecksum + ", checksum--->" + checksum);
                if (!savedChecksum.equals(checksum)) {
                    throw new IllegalStateException("wrong native lib check sum");
                }
            }
        }
    }
}
