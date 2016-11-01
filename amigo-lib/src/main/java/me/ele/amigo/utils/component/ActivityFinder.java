package me.ele.amigo.utils.component;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import me.ele.amigo.Amigo;
import me.ele.amigo.AmigoDirs;
import me.ele.amigo.PatchApks;
import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.utils.ArrayUtil;
import me.ele.amigo.utils.FileUtils;

public class ActivityFinder extends ComponentFinder {

    private static ActivityInfo[] activityInfosCache;
    private static ComponentName newLauncherComponent;

    public static ActivityInfo[] getAppActivities(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info =
                    pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            return info.activities;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ActivityInfo[] getNewAppActivities(Context context) {
        PackageManager pm = context.getPackageManager();
        if (!isHotfixApkValid(context)) {
            return null;
        }

        if (activityInfosCache == null) {
            File file = getHotFixApk(context);
            PackageInfo info =
                    pm.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
            String checkSum = PatchApks.getInstance(context).patchPath(Amigo
                    .getWorkingPatchApkChecksum(context));
            info.applicationInfo.sourceDir = checkSum;
            info.applicationInfo.publicSourceDir = checkSum;
            info.applicationInfo.uid = context.getApplicationInfo().uid;
            activityInfosCache = info.activities;
        }
        return activityInfosCache;
    }

    public static ComponentName getNewLauncherComponent(Context context) {
        if (newLauncherComponent != null) {
            return newLauncherComponent;
        }

        // see PackageParser.Activity
        for (Object activity : activities) {
            try {
                List<IntentFilter> intents =
                        (List<IntentFilter>) FieldUtils.readField(activity, "intents");
                for (IntentFilter intentFilter : intents) {
                    if (intentFilter.hasAction(Intent.ACTION_MAIN)
                            && intentFilter.hasCategory(Intent.CATEGORY_LAUNCHER)) {
                        ActivityInfo info = (ActivityInfo) FieldUtils.readField(activity, "info");
                        newLauncherComponent = new ComponentName(context.getPackageName(),
                                info.targetActivity != null ? info.targetActivity : info.name);
                        return newLauncherComponent;
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return newLauncherComponent;
    }

    public static ComponentName getLauncherComponent(Context context) {
        Intent launcherIntent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        ActivityInfo activityInfo =
                launcherIntent.resolveActivityInfo(context.getPackageManager(), 0);
        String className =
                activityInfo.targetActivity != null ? activityInfo.targetActivity
                        : activityInfo.name;
        return new ComponentName(context.getPackageName(), className);
    }

    public static ActivityInfo getActivityInfoInNewApp(Context context, String activityClassName) {
        ActivityInfo[] infos = getNewAppActivities(context);
        if (ArrayUtil.isEmpty(infos)) {
            return null;
        }

        for (ActivityInfo info : infos) {
            if (info.name.equals(activityClassName)) {
                Log.d("ActivityFinder", "getActivityInfoInNewApp: " + info.applicationInfo
                        .sourceDir);
                return info;
            }
        }
        return null;
    }
}
