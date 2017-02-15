
package me.ele.amigo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;

import me.ele.amigo.reflect.FieldUtils;

import static me.ele.amigo.AmigoInstrumentation.EXTRA_TARGET_INFO;
import static me.ele.amigo.AmigoInstrumentation.EXTRA_TARGET_INTENT;
import static me.ele.amigo.utils.component.ActivityFinder.getActivityInfoInNewApp;
import static me.ele.amigo.utils.component.ActivityFinder.getLauncherComponent;
import static me.ele.amigo.utils.component.ActivityFinder.getNewLauncherComponent;

public class AmigoCallback implements Handler.Callback {

    public static final int LAUNCH_ACTIVITY = 100;
    private static final String TAG = AmigoCallback.class.getSimpleName();
    private Handler.Callback mCallback = null;
    private Context context;

    public AmigoCallback(Context context, Handler.Callback callback) {
        this.mCallback = callback;
        this.context = context;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == LAUNCH_ACTIVITY) {
            return handleLaunchActivity(msg);
        }
        if (mCallback != null) {
            return mCallback.handleMessage(msg);
        }
        return false;
    }

    private boolean handleLaunchActivity(Message msg) {
        try {
            Amigo.rollAmigoBack(context);
            ClassLoader classLoader = context.getClassLoader();
            Intent intent = (Intent) FieldUtils.readField(msg.obj, "intent");
            intent.setExtrasClassLoader(classLoader);
            Intent targetIntent = intent.getParcelableExtra(EXTRA_TARGET_INTENT);
            if (targetIntent != null) {
                ComponentName targetComponentName = targetIntent.resolveActivity(context.getPackageManager());
                ActivityInfo targetActivityInfo = getActivityInfoInNewApp(context, targetComponentName.getClassName());
                if (targetActivityInfo != null) {
                    targetIntent.setExtrasClassLoader(classLoader);
                    targetIntent.putExtra(EXTRA_TARGET_INFO, targetActivityInfo);
                    FieldUtils.writeDeclaredField(msg.obj, "intent", targetIntent);
                    FieldUtils.writeDeclaredField(msg.obj, "activityInfo", targetActivityInfo);
                }
            } else {
                ActivityInfo activityInfo = (ActivityInfo) FieldUtils.readField(msg.obj, "activityInfo");
                String activityName = activityInfo.targetActivity != null ? activityInfo.targetActivity : activityInfo.name;
                ActivityInfo newActivityInfo = null;
                if (isLaunchActivity(intent, activityName)) {
                    activityInfo.targetActivity = getNewLauncherComponent(context).getClassName();
                    newActivityInfo = getActivityInfoInNewApp(context, activityInfo.targetActivity);
                    newActivityInfo.targetActivity = activityInfo.targetActivity;
                    FieldUtils.writeDeclaredField(msg.obj, "activityInfo", newActivityInfo);
                } else if ((newActivityInfo = getActivityInfoInNewApp(context, activityName)) != null) {
                    // TODO check intent filter ?
                    FieldUtils.writeDeclaredField(msg.obj, "activityInfo", newActivityInfo);
                } else {
                    // TODO this host activity was launched by using a scheme url,
                    // we can navigate to a matched patch activity instead.
                    Intent toPatchLauncherIntent = new Intent().setComponent(
                            getNewLauncherComponent(context))
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    context.startActivity(toPatchLauncherIntent);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mCallback != null) {
            return mCallback.handleMessage(msg);
        }
        return false;
    }

    private boolean isLaunchActivity(Intent intent, String targetActivity) {
        return (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_LAUNCHER))
                || targetActivity.equals(getLauncherComponent(context).getClassName());
    }
}