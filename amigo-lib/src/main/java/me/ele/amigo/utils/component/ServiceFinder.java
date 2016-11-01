package me.ele.amigo.utils.component;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.ele.amigo.reflect.FieldUtils;

public class ServiceFinder extends ComponentFinder {

    private static final String TAG = ServiceFinder.class.getSimpleName();

    private static Map<String, ServiceInfo> cache = new HashMap<>();

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

    public static ServiceInfo[] getNewAppServices(Context context) {
        if (!isHotfixApkValid(context)) {
            return null;
        }
        File file = getHotFixApk(context);
        PackageManager pm = context.getPackageManager();
        String archiveFilePath = file.getAbsolutePath();
        PackageInfo info = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_SERVICES);
        return info.services;
    }


    public static List<ServiceInfo> getNewAddedServices(Context context) {
        ServiceInfo[] newAddedServices = getNewAppServices(context);
        ServiceInfo[] appServices = getAppServices(context);
        List<ServiceInfo> addedServices = new ArrayList<>();
        if (newAddedServices != null && newAddedServices.length > 0) {
            for (ServiceInfo newAppService : newAddedServices) {
                boolean isNew = true;
                if (appServices != null && appServices.length > 0) {
                    for (ServiceInfo appService : appServices) {
                        if (newAppService.name.equals(appService.name)) {
                            isNew = false;
                            break;
                        }
                    }
                }

                if (isNew) {
                    android.util.Log.e(TAG, "new Service-->" + newAppService);
                    addedServices.add(newAppService);
                }
            }
        }
        return addedServices;
    }

    public static List<IntentFilter> getIntentFilter(Context context, ServiceInfo serviceInfo) {
        parsePackage(context);

        for (Object service : services) {
            try {
                ServiceInfo info = (ServiceInfo) FieldUtils.readField(service, "info");
                if (info.name.equals(serviceInfo.name)) {
                    return (List<IntentFilter>) FieldUtils.readField(service, "intents");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static ServiceInfo resolveServiceInfo(Context context, Intent intent) {
        ComponentName componentName = intent.getComponent();

        if (componentName != null && cache.containsKey(componentName.getClassName())) {
            return cache.get(componentName.getClassName());
        }

        List<ServiceInfo> serviceInfos = ServiceFinder.getNewAddedServices(context);
        if (serviceInfos != null && serviceInfos.size() >= 0) {
            for (ServiceInfo serviceInfo : serviceInfos) {
                List<IntentFilter> intentFilters = ServiceFinder.getIntentFilter(context,
                        serviceInfo);
                if (intentFilters != null && intentFilters.size() > 0) {
                    for (IntentFilter intentFilter : intentFilters) {
                        int match = intentFilter.match(context.getContentResolver(), intent,
                                true, "");
                        if (match >= 0) {
                            return serviceInfo;
                        }
                    }
                } else {
                    if (componentName != null && serviceInfo.name.equals(componentName
                            .getClassName())) {
                        return serviceInfo;
                    }
                }
            }
        }
        return null;
    }
}
