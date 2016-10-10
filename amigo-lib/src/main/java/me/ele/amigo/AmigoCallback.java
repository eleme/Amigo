
package me.ele.amigo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.utils.component.ActivityFinder;

import static me.ele.amigo.AmigoInstrumentation.EXTRA_TARGET_INFO;
import static me.ele.amigo.AmigoInstrumentation.EXTRA_TARGET_INTENT;

public class AmigoCallback implements Handler.Callback {

    private static final String TAG = AmigoCallback.class.getSimpleName();

    public static final int LAUNCH_ACTIVITY = 100;


    private Handler.Callback mCallback = null;
    private Context context;
    private AmigoClassLoader classLoader;


    public AmigoCallback(Context context, AmigoClassLoader amigoClassLoader, Handler.Callback callback) {
        this.mCallback = callback;
        this.context = context;
        this.classLoader = amigoClassLoader;
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
            Intent stubIntent = (Intent) FieldUtils.readField(msg.obj, "intent");
            stubIntent.setExtrasClassLoader(classLoader);
            Intent targetIntent = stubIntent.getParcelableExtra(EXTRA_TARGET_INTENT);
            if (targetIntent != null) {
                ComponentName targetComponentName = targetIntent.resolveActivity(context.getPackageManager());
                Log.e(TAG, "targetComponentName--->" + targetComponentName);
                ActivityInfo targetActivityInfo = ActivityFinder.getActivityInfoInNewApp(context, targetComponentName.getClassName());
                if (targetActivityInfo != null) {
                    targetIntent.setExtrasClassLoader(classLoader);
                    targetIntent.putExtra(EXTRA_TARGET_INFO, targetActivityInfo);
                    FieldUtils.writeDeclaredField(msg.obj, "intent", targetIntent);
                    FieldUtils.writeDeclaredField(msg.obj, "activityInfo", targetActivityInfo);

                    Log.e(TAG, "handleLaunchActivity OK");
                } else {
                    Log.e(TAG, "handleLaunchActivity oldInfo==null");
                }
            } else {
                Log.e(TAG, "handleLaunchActivity targetIntent==null");
            }
        } catch (Exception e) {
            Log.e(TAG, "handleLaunchActivity FAIL", e);
        }

        if (mCallback != null) {
            return mCallback.handleMessage(msg);
        }
        return false;
    }

}