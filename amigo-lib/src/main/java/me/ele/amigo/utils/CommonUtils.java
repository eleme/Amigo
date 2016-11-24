package me.ele.amigo.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import me.ele.amigo.reflect.FieldUtils;

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

    public static String getVersionName(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static PackageInfo getPackageInfo(Context context, File patchApk, int flags) {
        PackageManager pm = context.getPackageManager();
        return pm.getPackageArchiveInfo(patchApk.getAbsolutePath(), flags);
    }

    public static Signature getSignature(Context context) throws NameNotFoundException {
        return context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_SIGNATURES).signatures[0];
    }

    public static Signature getSignature(Context context, File patchApk) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return context.getPackageManager().getPackageArchiveInfo(patchApk.getAbsolutePath(),
                    PackageManager.GET_SIGNATURES).signatures[0];
        } else {
            return getPackageArchiveInfo(patchApk.getAbsolutePath(),
                    PackageManager.GET_SIGNATURES).signatures[0];
        }

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
        return pm.getApplicationInfo(packageName, 0);
    }


    public static PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) {
        // Workaround for https://code.google.com/p/android/issues/detail?id=9151#c8
        try {
            Class packageParserClass = Class.forName("android.content.pm.PackageParser");
            Class packageParserPackageClass = Class.forName("android.content.pm" +
                    ".PackageParser$Package");
            Constructor packageParserConstructor = packageParserClass.getConstructor(String.class);
            Method parsePackageMethod = packageParserClass.getDeclaredMethod(
                    "parsePackage", File.class, String.class, DisplayMetrics.class, int.class);
            Method collectCertificatesMethod = packageParserClass.getDeclaredMethod(
                    "collectCertificates", packageParserPackageClass, int.class);
            Method generatePackageInfoMethod;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                generatePackageInfoMethod = packageParserClass.getDeclaredMethod(
                        "generatePackageInfo", packageParserPackageClass, int[].class, int.class,
                        long.class, long.class);
            } else {
                generatePackageInfoMethod = packageParserClass.getDeclaredMethod(
                        "generatePackageInfo", packageParserPackageClass, int[].class, int.class);
            }
            packageParserConstructor.setAccessible(true);
            parsePackageMethod.setAccessible(true);
            collectCertificatesMethod.setAccessible(true);
            generatePackageInfoMethod.setAccessible(true);

            Object packageParser = packageParserConstructor.newInstance(archiveFilePath);

            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();

            final File sourceFile = new File(archiveFilePath);

            Object pkg = parsePackageMethod.invoke(
                    packageParser,
                    sourceFile,
                    archiveFilePath,
                    metrics,
                    0);
            if (pkg == null) {
                return null;
            }

            if ((flags & android.content.pm.PackageManager.GET_SIGNATURES) != 0) {
                collectCertificatesMethod.invoke(packageParser, pkg, 0);
            }

            PackageInfo packageInfo;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                packageInfo = (PackageInfo) generatePackageInfoMethod.invoke(null, pkg, null,
                        flags, 0, 0);
            } else {
                packageInfo = (PackageInfo) generatePackageInfoMethod.invoke(null, pkg, null,
                        flags);
            }

            if ((flags & PackageManager.GET_META_DATA) != 0) {
                try {
                    Bundle mAppMetaData = (Bundle) FieldUtils.readField(pkg, "mAppMetaData", true);
                    packageInfo.applicationInfo.metaData = mAppMetaData;
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            return packageInfo;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Signature Monitor", "android.content.pm.PackageParser reflection failed: " + e
                    .toString());
        }
        return null;
    }
}
