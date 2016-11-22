package me.ele.amigo.utils.component;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.util.Log;

import java.util.List;

import me.ele.amigo.utils.ArrayUtil;


public class ServiceFinder extends ComponentFinder {

    private static final String TAG = ServiceFinder.class.getSimpleName();
    private static ServiceInfo[] sHostServices;

    public static ServiceInfo[] getAppServices(Context context) {
        if (sHostServices == null) {
            try {
                PackageManager pm = context.getPackageManager();
                PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager
                        .GET_SERVICES);
                sHostServices = info.services;
                if (sHostServices == null) {
                    sHostServices = new ServiceInfo[0];
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        return sHostServices;
    }

    public static boolean newServiceExistsInPatch(Context context) {
        parsePackage(context);
        getAppServices(context);
        for (int i = sServices.size() - 1; i >= 0; i--) {
            if (isNew(sServices.get(i).serviceInfo)) {
                return true;
            }
        }
        return false;
    }

    public static ServiceInfo resolveNewServiceInfo(Context context, Intent intent) {
        parsePackage(context);
        getAppServices(context);

        for (Service service : sServices) {
            if (!isNew(service.serviceInfo)) {
                continue;
            }

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

    private static boolean isNew(ServiceInfo serviceInfo) {
        // diff metadata ??
        for (int i = ArrayUtil.length(sHostServices) - 1; i >= 0; i--) {
            if (serviceInfo.name.equals(sHostServices[i].name)) {
                return false;
            }
        }
        return true;
    }
}
