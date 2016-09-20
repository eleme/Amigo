package me.ele.amigo;

import android.app.Activity;
import android.app.Fragment;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.utils.ComponentUtils;

import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_INSTANCE;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_TASK;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_TOP;

public class AmigoInstrumentation extends Instrumentation implements IInstrumentation {

    public static final String EXTRA_TARGET_INTENT = "me.ele.amigo.OldIntent";
    public static final String EXTRA_TARGET_INFO = "me.ele.amigo.OldInfo";

    private static final String TAG = AmigoInstrumentation.class.getSimpleName();

    private Instrumentation oldInstrumentation;

    public AmigoInstrumentation(Instrumentation oldInstrumentation) {
        this.oldInstrumentation = oldInstrumentation;
    }

    private void startStubActivity(Context who, Intent intent) {
        ComponentName componentName = intent.getComponent();
        String targetClassName = getDelegateActivityName(who, componentName.getClassName());
        Intent newIntent = new Intent();
        newIntent.setComponent(new ComponentName(componentName.getPackageName(), targetClassName));
        newIntent.putExtra(EXTRA_TARGET_INTENT, intent);
        newIntent.setFlags(intent.getFlags());
        who.startActivity(newIntent);
    }

    @Override
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target,
                                            Intent intent, int requestCode, Bundle options) {
        try {
            Method method = oldInstrumentation.getClass().getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class);
            return (ActivityResult) method.invoke(oldInstrumentation, who, contextThread, token, target, intent, requestCode, options);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        startStubActivity(who, intent);
        return null;
    }

    @Override
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target,
                                            Intent intent, int requestCode) {
        try {
            Method method = oldInstrumentation.getClass().getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class);
            return (ActivityResult) method.invoke(oldInstrumentation, who, contextThread, token, target, intent, requestCode);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        startStubActivity(who, intent);
        return null;
    }

    @Override
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Fragment target, Intent intent, int requestCode) {
        try {
            Method method = oldInstrumentation.getClass().getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class, Fragment.class, Intent.class, int.class, Bundle.class);
            return (ActivityResult) method.invoke(oldInstrumentation, who, contextThread, token, target, intent, requestCode);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        startStubActivity(who, intent);
        return null;
    }

    @Override
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Fragment target, Intent intent, int requestCode, Bundle options) {
        try {
            Method method = oldInstrumentation.getClass().getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class, Fragment.class, Intent.class, int.class, Bundle.class);
            return (ActivityResult) method.invoke(oldInstrumentation, who, contextThread, token, target, intent, requestCode, options);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        startStubActivity(who, intent);
        return null;
    }

    @Override
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode, Bundle options, UserHandle user) {
        try {
            Method method = oldInstrumentation.getClass().getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class, UserHandle.class);
            return (ActivityResult) method.invoke(oldInstrumentation, who, contextThread, token, target, intent, requestCode, options, user);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        startStubActivity(who, intent);
        return null;
    }

    @Override
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, String target, Intent intent, int requestCode, Bundle options) {
        try {
            Method method = oldInstrumentation.getClass().getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class, String.class, Intent.class, int.class, Bundle.class);
            return (ActivityResult) method.invoke(oldInstrumentation, who, contextThread, token, target, intent, requestCode, options);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        startStubActivity(who, intent);
        return null;
    }

    private String getDelegateActivityName(Context context, String targetClassName) {
        if (targetClassName.startsWith(".")) {
            targetClassName = context.getPackageName() + targetClassName;
        }

        ActivityInfo info = ComponentUtils.getActivityInfoInNewApp(context, targetClassName);
        if (info == null) {
            throw new RuntimeException(String.format("cannot find %s in apk", targetClassName));
        }

        String clazz;
        switch (info.launchMode) {
            case LAUNCH_SINGLE_TOP:
                clazz = ActivityStub.SingleTopStub.class.getName();
                break;
            case LAUNCH_SINGLE_TASK:
                clazz = ActivityStub.SingleTaskStub.class.getName();
                break;
            case LAUNCH_SINGLE_INSTANCE:
                clazz = ActivityStub.SingleInstanceStub.class.getName();
                break;
            default:
                clazz = ActivityStub.StandardStub.class.getName();
                break;
        }

        return clazz;
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        try {
            Intent targetIntent = activity.getIntent();
            if (targetIntent != null) {
                ActivityInfo targetInfo = targetIntent.getParcelableExtra(EXTRA_TARGET_INFO);
                if (targetInfo != null) {
                    activity.setRequestedOrientation(targetInfo.screenOrientation);
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "onActivityCreated fail", e);
        }

        try {
            ComponentName componentName = new ComponentName(activity, getDelegateActivityName(activity, activity.getClass().getName()));
            FieldUtils.writeField(activity, "mComponent", componentName);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (oldInstrumentation != null) {
            oldInstrumentation.callActivityOnCreate(activity, icicle);
        } else {
            super.callActivityOnCreate(activity, icicle);
        }
    }

}
