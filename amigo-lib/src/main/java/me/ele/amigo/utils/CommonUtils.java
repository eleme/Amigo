package me.ele.amigo.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;

import java.io.File;

public class CommonUtils {

    public static int getVersionCode(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static int getVersionCode(Context context, File patchApk) {
        PackageManager pm = context.getPackageManager();
        return pm.getPackageArchiveInfo(patchApk.getAbsolutePath(), 0).versionCode;
    }

    public static Signature getSignature(Context context) throws NameNotFoundException {
        return context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_SIGNATURES).signatures[0];
    }

    public static Signature getSignature(Context context, File patchApk) {
        return context.getPackageManager().getPackageArchiveInfo(
                patchApk.getAbsolutePath(), PackageManager.GET_SIGNATURES).signatures[0];
    }

    public static ApplicationInfo getApplicationInfo(Context context) throws NameNotFoundException {
        PackageManager pm;
        String packageName;
        try {
            pm = context.getPackageManager();
            packageName = context.getPackageName();
        } catch (RuntimeException e) {
            /* Ignore those exceptions so that we don't break tests replying on Context like
             * a android.text.MockContext or a android.content.ContextWrapper with a null
             * base Context.
             */
            return null;
        }
        if (pm == null || packageName == null) {
            // This is most likely a mock context, so just return without patching.
            return null;
        }
        ApplicationInfo applicationInfo =
                pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        return applicationInfo;
    }
}
