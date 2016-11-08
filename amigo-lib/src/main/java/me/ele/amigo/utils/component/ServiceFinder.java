package me.ele.amigo.utils.component;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import java.util.List;


public class ServiceFinder extends ComponentFinder {

    private static final String TAG = ServiceFinder.class.getSimpleName();

    public static ServiceInfo[] getAppServices(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager
                    .GET_SERVICES);
            return info.services;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ServiceInfo resolveServiceInfo(Context context, Intent intent) {
        parsePackage(context);
        for (Service service : sServices) {
            List<IntentFilter> intentFilters = service.filters;
            if (intentFilters != null && intentFilters.size() > 0) {
                for (IntentFilter intentFilter : intentFilters) {
                    if (intentFilter.match(context.getContentResolver(), intent,
                            true, "") >= 0) {
                        return service.serviceInfo;
                    }
                }
            } else {
                ComponentName componentName = intent.getComponent();
                if (componentName != null && service.serviceInfo.name.equals(componentName
                        .getClassName())) {
                    return service.serviceInfo;
                }
            }
        }

        return null;
    }
}
