package me.ele.amigo.utils.component;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.List;

public class ActivityFinder extends ComponentFinder {

    private static ComponentName newLauncherComponent;
    private static ActivityInfo[] sHostActivities;

    public static ActivityInfo[] getAppActivities(Context context) {
        if (sHostActivities != null) {
            return sHostActivities;
        }

        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info =
                    pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES |
                            PackageManager.GET_META_DATA);
            return sHostActivities = info.activities;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isThereNewActivityInPatch(Context context) {
        parsePackage(context);
        ActivityInfo[] hostActivities = getAppActivities(context);
        boolean newActivity = false;
        out:
        for (int i = sActivities.size() - 1; i >= 0; i--) {
            ActivityInfo patchActivityInfo = sActivities.get(i).activityInfo;
            for (int i1 = hostActivities.length - 1; i1 >= 0; i1--) {
                if (hostActivities[i1].name.equals(patchActivityInfo.name)) {
                    break out;
                }
            }
            newActivity = true;
            break;
        }
        // check any changes in activity's metadata ?
        return newActivity;
    }

    public static ComponentName getNewLauncherComponent(Context context) {
        if (newLauncherComponent != null) {
            return newLauncherComponent;
        }
        parsePackage(context);
        for (Activity activity : sActivities) {
            List<IntentFilter> intents = activity.filters;
            if (intents == null || intents.isEmpty()) {
                continue;
            }

            for (IntentFilter intentFilter : intents) {
                if (intentFilter.hasAction(Intent.ACTION_MAIN)
                        && intentFilter.hasCategory(Intent.CATEGORY_LAUNCHER)) {
                    ActivityInfo info = activity.activityInfo;
                    newLauncherComponent = new ComponentName(context.getPackageName(),
                            info.targetActivity != null ? info.targetActivity : info.name);
                    return newLauncherComponent;
                }
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
        parsePackage(context);
        if (sActivities.isEmpty()) {
            return null;
        }

        for (Activity activity : sActivities) {
            ActivityInfo activityInfo = activity.activityInfo;
            if (activityInfo.name.equals(activityClassName)) {
                return activityInfo;
            }
        }
        return null;
    }
}
