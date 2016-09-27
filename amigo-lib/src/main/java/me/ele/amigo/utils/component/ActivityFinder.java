package me.ele.amigo.utils.component;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.File;

public class ActivityFinder extends ComponentFinder {

    public static ActivityInfo[] getAppActivities(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
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
        File file = getHotFixApk(context);
        PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
        return info.activities;
    }


    public static ActivityInfo getActivityInfoInNewApp(Context context, String activityClassName) {
        ActivityInfo[] infos = getNewAppActivities(context);
        for (ActivityInfo info : infos) {
            if (info.name.equals(activityClassName)) {
                return info;
            }
        }
        return null;
    }
}
