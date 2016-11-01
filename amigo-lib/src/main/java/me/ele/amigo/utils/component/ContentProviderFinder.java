package me.ele.amigo.utils.component;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import me.ele.amigo.PatchApks;
import me.ele.amigo.reflect.MethodUtils;
import me.ele.amigo.utils.ArrayUtil;

import static android.content.Context.MODE_MULTI_PROCESS;
import static me.ele.amigo.Amigo.SP_NAME;
import static me.ele.amigo.Amigo.WORKING_PATCH_APK_CHECKSUM;
import static me.ele.amigo.compat.ActivityThreadCompat.instance;

public class ContentProviderFinder extends ComponentFinder {

    private static final String TAG = ContentProviderFinder.class.getSimpleName();

    public static ProviderInfo[] getAppContentProvider(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getApplicationContext().getPackageName(), PackageManager.GET_PROVIDERS);
            return packageInfo.providers;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ProviderInfo[] getNewContentProvider(Context context) {
        if (!isHotfixApkValid(context)) {
            return null;
        }

        PackageManager pm = context.getPackageManager();
        String workingPatchApkChecksum = context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS)
                .getString(WORKING_PATCH_APK_CHECKSUM, "");
        File patchApkFile = PatchApks.getInstance(context).patchFile(workingPatchApkChecksum);
        if (patchApkFile == null) {
            return null;
        }

        String archiveFilePath = patchApkFile.getAbsolutePath();
        PackageInfo info = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_PROVIDERS);
        return info.providers;
    }

    public static void installPatchContentProviders(Context context) {
        ProviderInfo[] providers = getNewContentProvider(context);
        if (ArrayUtil.isEmpty(providers)) {
            Log.d(TAG, "installPatchContentProviders: there is no any new provider");
            return;
        }

        Log.d(TAG, "installPatchContentProviders: " + Arrays.toString(providers));
        try {
            MethodUtils.invokeMethod(instance(), "installContentProviders",
                    new Object[]{context, Arrays.asList(providers)},
                    new Class<?>[]{Context.class, List.class});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
