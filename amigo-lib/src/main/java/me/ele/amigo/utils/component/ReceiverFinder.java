package me.ele.amigo.utils.component;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

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
        try {
            for (Activity receiver : sReceivers) {
                List<IntentFilter> filters = receiver.filters;
                if (filters == null || filters.isEmpty()) {
                    continue;
                }

                BroadcastReceiver receiverInstance = (BroadcastReceiver) classLoader.loadClass
                        (receiver.activityInfo.name).newInstance();
                for (IntentFilter filter : filters) {
                    context.registerReceiver(receiverInstance, filter);
                    registeredReceivers.add(receiverInstance);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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
