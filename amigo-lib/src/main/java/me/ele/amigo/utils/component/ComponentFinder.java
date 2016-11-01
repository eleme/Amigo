package me.ele.amigo.utils.component;

import android.content.Context;
import android.util.DisplayMetrics;

import java.io.File;
import java.util.Collections;
import java.util.List;

import me.ele.amigo.PatchApks;
import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.reflect.MethodUtils;

import static android.content.Context.MODE_MULTI_PROCESS;
import static me.ele.amigo.Amigo.SP_NAME;
import static me.ele.amigo.Amigo.WORKING_PATCH_APK_CHECKSUM;

public class ComponentFinder {

    protected static List<Object> receivers;
    protected static List<Object> services;
    protected static List<Object> activities;
    private static boolean hasParsePackage = false;

    protected static File getHotFixApk(Context context) {
        String workingPatchApkChecksum = context.getSharedPreferences(SP_NAME,
                MODE_MULTI_PROCESS).getString(WORKING_PATCH_APK_CHECKSUM, "");
        return PatchApks.getInstance(context).patchFile(workingPatchApkChecksum);
    }

    protected static boolean isHotfixApkValid(Context context) {
        File file = getHotFixApk(context);
        if (file == null || !file.exists()) {
            return false;
        }

        return true;
    }

    protected static void parsePackage(Context context) {
        if (hasParsePackage) {
            return;
        }
        if (!isHotfixApkValid(context)) {
            return;
        }

        Object mPackageParser;
        Object mPackage;

        try {
            File file = ActivityFinder.getHotFixApk(context);
            Class sPackageParserClass = Class.forName("android.content.pm.PackageParser");
            try {
                mPackageParser = sPackageParserClass.newInstance();
                mPackage = MethodUtils.invokeMethod(mPackageParser, "parsePackage", file, 0);
            } catch (Exception e) {
                e.printStackTrace();

                mPackageParser = sPackageParserClass.getDeclaredConstructor(String.class)
                        .newInstance(file.getPath());
                DisplayMetrics metrics = new DisplayMetrics();
                metrics.setToDefaults();
                mPackage = MethodUtils.invokeMethod(mPackageParser, "parsePackage", file, file
                        .getPath(), metrics, 0);
            }

            receivers = (List<Object>) FieldUtils.readField(mPackage, "receivers");
            services = (List<Object>) FieldUtils.readField(mPackage, "services");
            activities = (List<Object>) FieldUtils.readField(mPackage, "activities");

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (receivers == null) {
            receivers = Collections.EMPTY_LIST;
        }

        if (services == null) {
            services = Collections.EMPTY_LIST;
        }

        if (activities == null) {
            activities = Collections.EMPTY_LIST;
        }

        hasParsePackage = true;
    }
}
