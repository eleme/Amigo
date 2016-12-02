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
            /*
             * Notice :
             * will not return the activity-info of a component which is declared disabled in
             * manifest and has never been set to enabled state dynamically.
            */
            PackageInfo info =
                    pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES |
                            PackageManager.GET_META_DATA);
            return sHostActivities = info.activities;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean newActivityExistsInPatch(Context context) {
        parsePackage(context);
        getAppActivities(context);
        for (int i = sActivities.size() - 1; i >= 0; i--) {
            ActivityInfo patchActivityInfo = sActivities.get(i).activityInfo;
            if (isNew(patchActivityInfo)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNew(ActivityInfo patchActivityInfo) {
        // check any changes in activity's metadata ?
        for (int i1 = sHostActivities.length - 1; i1 >= 0; i1--) {
            if (sHostActivities[i1].name.equals(patchActivityInfo.name)) {
                return false;
            }
        }
        return true;
    }

    public static ComponentName getNewLauncherComponent(Context context) {
        if (newLauncherComponent != null) {
            return newLauncherComponent;
        }

        parsePackage(context);
        for (Activity activity : sActivities) {
            if (isNewLauncherActivity(activity)) {
                ActivityInfo info = activity.activityInfo;
                return newLauncherComponent = new ComponentName(context.getPackageName(),
                        info.targetActivity != null ? info.targetActivity : info.name);
            }
        }
        return newLauncherComponent;
    }

    private static boolean isNewLauncherActivity(Activity activity) {
        List<IntentFilter> intents = activity.filters;
        if (intents == null || intents.isEmpty()) {
            return false;
        }

        for (IntentFilter intentFilter : intents) {
            if (intentFilter.hasAction(Intent.ACTION_MAIN)
                    && intentFilter.hasCategory(Intent.CATEGORY_LAUNCHER)) {
                return true;
            }
        }
        return false;
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
