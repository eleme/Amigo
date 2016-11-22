package me.ele.amigo.utils.component;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import me.ele.amigo.utils.ArrayUtil;

public class ReceiverFinder extends ComponentFinder {

    private static final String TAG = ReceiverFinder.class.getSimpleName();


    public static ActivityInfo[] getAppReceivers(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager
                    .GET_RECEIVERS);
            return info.receivers;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void registerNewReceivers(Context context, ClassLoader classLoader) {
        parsePackage(context);
        ActivityInfo[] receiverInHost = getAppReceivers(context);
        boolean findNew = false;
        try {
            for (int i = 0, size = sReceivers.size(); i < size; i++) {
                Activity receiver = sReceivers.get(i);
                List<IntentFilter> filters = receiver.filters;
                if (filters == null
                        || filters.isEmpty()
                        || !isNewReceiver(receiverInHost, receiver)) {
                    continue;
                }

                registerOneReceiver(context, classLoader, receiver, filters);
                findNew = true;
            }

            if (!findNew) {
                Log.d(TAG, "registerNewReceivers: there is no any new receiver");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void registerOneReceiver(Context context, ClassLoader classLoader,
                                            Activity receiver, List<IntentFilter> filters) throws
            InstantiationException, IllegalAccessException, ClassNotFoundException {
        BroadcastReceiver receiverInstance = (BroadcastReceiver) classLoader.loadClass
                (receiver.activityInfo.name).newInstance();
        for (IntentFilter filter : filters) {
            context.registerReceiver(receiverInstance, filter);
            registeredReceivers.add(receiverInstance);
        }
        Log.d(TAG, "registerOneReceiver: " + receiver.activityInfo);
    }

    private static boolean isNewReceiver(ActivityInfo[] receiverInHost, Activity patchReceiver) {
        if (ArrayUtil.isNotEmpty(receiverInHost)) {
            for (ActivityInfo activityInfo : receiverInHost) {
                if (patchReceiver.activityInfo.name.equals(activityInfo.name)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static final List<BroadcastReceiver> registeredReceivers = new ArrayList<>();

    public static void unregisterNewReceivers(Context context) {
        if (!registeredReceivers.isEmpty()) {
            for (BroadcastReceiver registeredReceiver : registeredReceivers) {
                context.unregisterReceiver(registeredReceiver);
            }
            registeredReceivers.clear();
        }
    }

}
