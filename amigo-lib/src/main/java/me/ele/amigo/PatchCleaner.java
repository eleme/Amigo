package me.ele.amigo;

import android.content.Context;
import android.util.Log;

import java.io.File;

import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.ProcessUtils;

import static android.content.Context.MODE_MULTI_PROCESS;
import static me.ele.amigo.Amigo.SP_NAME;
import static me.ele.amigo.Amigo.VERSION_CODE;
import static me.ele.amigo.utils.FileUtils.removeFile;

class PatchCleaner {

    private static final String TAG = "amigo_patch_cleaner";

    static void clearPatchIfInMainProcess(Context context) {
        // clear is a dangerous operation, only need to be operated by main process
        Log.e(TAG, "clear");
        if (!ProcessUtils.isMainProcess(context)) {
            Log.d(TAG, "not running in the main process, aborting clear.");
            return;
        }

        removeFile(AmigoDirs.getInstance(context).amigoDir());
        context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS)
                .edit()
                .clear()
                .putInt(VERSION_CODE, CommonUtils.getVersionCode(context))
                .commit();
    }

    static void clearWithoutPatchApk(Context context, String checksum) {
        Log.e(TAG, "clear without patch");
        AmigoDirs amigoDirs = AmigoDirs.getInstance(context);
        PatchApks patchApks = PatchApks.getInstance(context);
        File[] patchDirs = amigoDirs.amigoDir().listFiles();
        if (patchDirs != null && patchDirs.length > 0) {
            for (File patchDir : patchDirs) {
                if (patchDir.getName().equals(checksum)) {
                    File[] files = patchDir.listFiles();
                    for (File file : files) {
                        if (!file.getAbsolutePath().equals(patchApks.patchPath(checksum))) {
                            Log.e(TAG, "remove file: " + file.getAbsolutePath());
                            removeFile(file, false);
                        }
                    }
                }
            }
        }
    }

    static void clearOldPatches(Context context, String exclude) {
        Log.e(TAG, "clear old patches");
        AmigoDirs amigoDirs = AmigoDirs.getInstance(context);
        File[] patchDirs = amigoDirs.amigoDir().listFiles();
        if (patchDirs == null || patchDirs.length == 0) {
            return;
        }
        for (File patchDir : patchDirs) {
            if (!patchDir.getName().equals(exclude)) {
                removeFile(patchDir, true);
            }
        }
    }
}
