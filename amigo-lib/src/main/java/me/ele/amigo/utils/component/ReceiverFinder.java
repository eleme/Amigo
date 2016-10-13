package me.ele.amigo.utils.component;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.ele.amigo.reflect.FieldUtils;

public class ReceiverFinder extends ComponentFinder {

    private static final String TAG = ReceiverFinder.class.getSimpleName();


    public static ActivityInfo[] getAppReceivers(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_RECEIVERS);
            return info.receivers;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ActivityInfo[] getNewAppReceivers(Context context) {
        if (!isHotfixApkValid(context)) {
            return null;
        }
        File file = getHotFixApk(context);
        PackageManager pm = context.getPackageManager();
        String archiveFilePath = file.getAbsolutePath();
        PackageInfo info = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_RECEIVERS);
        return info.receivers;
    }

    private static List<ActivityInfo> getNewAddedReceivers(Context context) {
        ActivityInfo[] newAppReceivers = getNewAppReceivers(context);
        ActivityInfo[] appReceivers = getAppReceivers(context);
        List<ActivityInfo> addedReceivers = new ArrayList<>();
        if (newAppReceivers != null && newAppReceivers.length > 0) {
            for (ActivityInfo newAppReceiver : newAppReceivers) {
                boolean isNew = true;
                if (appReceivers != null && appReceivers.length > 0) {
                    for (ActivityInfo appReceiver : appReceivers) {
                        if (newAppReceiver.name.equals(appReceiver.name)) {
                            isNew = false;
                            break;
                        }
                    }
                }

                if (isNew) {
                    android.util.Log.e(TAG, "newReceiver-->" + newAppReceiver);
                    addedReceivers.add(newAppReceiver);
                }
            }
        }
        return addedReceivers;
    }

    public static void registerNewReceivers(Context context, ClassLoader classLoader) {
        try {
            List<ActivityInfo> addedReceivers = getNewAddedReceivers(context);
            for (ActivityInfo addedReceiver : addedReceivers) {
                List<IntentFilter> filters = getReceiverIntentFilter(context, addedReceiver);
                for (IntentFilter filter : filters) {
                    BroadcastReceiver receiver = (BroadcastReceiver) classLoader.loadClass(addedReceiver.name).newInstance();
                    context.registerReceiver(receiver, filter);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static List<IntentFilter> getReceiverIntentFilter(Context context, ActivityInfo receiverInfo) {
        parsePackage(context);
        Object data = null;
        for (Object receiver : receivers) {
            try {
                ActivityInfo info = (ActivityInfo) FieldUtils.readField(receiver, "info");
                if (info.name.equals(receiverInfo.name)) {
                    data = receiver;
                    break;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }

        try {
            return (List<IntentFilter>) FieldUtils.readField(data, "intents");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

}
