package me.ele.amigo.utils.component;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.util.List;

import static android.content.IntentFilter.NO_MATCH_ACTION;
import static android.content.IntentFilter.NO_MATCH_CATEGORY;
import static android.content.IntentFilter.NO_MATCH_DATA;
import static android.content.IntentFilter.NO_MATCH_TYPE;

public class ActivityFinder extends ComponentFinder {

    private static ComponentName newLauncherComponent;
    private static ActivityInfo[] sHostActivities;

    public static ActivityInfo[] getAppActivities(Context context) {
        if (sHostActivities != null) {
            return sHostActivities;
        }

        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
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
        Intent launcherIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        ActivityInfo activityInfo = launcherIntent.resolveActivityInfo(context.getPackageManager(), 0);
        String className = activityInfo.targetActivity != null ? activityInfo.targetActivity : activityInfo.name;
        return new ComponentName(context.getPackageName(), className);
    }

    public static ActivityInfo getActivityInfoInNewApp(Context context, String activityClassName) {
        parsePackage(context);
        if (sActivities.isEmpty()) {
            return null;
        }

        for (Activity activity : sActivities) {
            ActivityInfo activityInfo = activity.activityInfo;
            if (!activityInfo.name.equals(activityClassName)) {
                continue;
            }
            boolean isAlias = !TextUtils.isEmpty(activityInfo.targetActivity);
            if (!isAlias) {
                return activityInfo;
            } else {
                return getActivityInfoInNewApp(context, activityInfo.targetActivity);
            }
        }
        return null;
    }

    public static ActivityInfo getActivityInfoInNewApp(Context context, Intent intent) {
        if (intent == null) {
            return null;
        }
        if (intent.getComponent() != null) {
            return getActivityInfoInNewApp(context, intent.getComponent().getClassName());
        }

        parsePackage(context);
        if (sActivities.isEmpty()) {
            return null;
        }

        for (Activity activity : sActivities) {
            List<IntentFilter> filters = activity.filters;
            if (filters == null || filters.size() == 0) {
                continue;
            }
            for (IntentFilter filter : filters) {
                int match = filter.match(null, intent, false, "filter_match_tag");
                if (match != NO_MATCH_TYPE && match != NO_MATCH_DATA
                        && match != NO_MATCH_ACTION && match != NO_MATCH_CATEGORY) {
                    return getActivityInfoInNewApp(context, activity.activityInfo.name);
                }
            }
        }
        return null;

    }
}
