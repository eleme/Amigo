package me.ele.amigo;

import android.content.Context;
import android.util.Log;

import me.ele.amigo.utils.ProcessUtils;

class PatchCleaner {

    private static final String TAG = "amigo_patch_cleaner";

    static void clearPatchIfInMainProcess(Context context) {
        // clear is a dangerous operation, only need to be operated by main process
        Log.e(TAG, "clear");
        if (!ProcessUtils.isMainProcess(context)) {
            Log.d(TAG, "not running in the main process, aborting clear.");
            return;
        }

        AmigoDirs.getInstance(context).clear();
    }

    static void clearWithoutPatchApk(Context context, String checksum) {
        Log.e(TAG, "clear without patch");
        AmigoDirs.getInstance(context).deletePatchExceptApk(checksum);
    }

    static void clearOldPatches(Context context, String exclude) {
        Log.e(TAG, "clear old patches");
        AmigoDirs.getInstance(context).deleteAllPatches(exclude);
    }
}
