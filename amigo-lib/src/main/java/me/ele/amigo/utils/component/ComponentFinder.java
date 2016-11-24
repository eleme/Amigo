package me.ele.amigo.utils.component;

import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.ele.amigo.Amigo;
import me.ele.amigo.PatchApks;
import me.ele.amigo.PatchInfoUtil;
import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.reflect.MethodUtils;

class ComponentFinder {

    static List<Activity> sReceivers = Collections.EMPTY_LIST;
    static List<Service> sServices = Collections.EMPTY_LIST;
    static List<Activity> sActivities = Collections.EMPTY_LIST;
    static List<Object> sProviders = Collections.EMPTY_LIST;
    private static boolean hasParsePackage = false;

    static File getHotFixApk(Context context) {
        String workingPatchApkChecksum = PatchInfoUtil.getWorkingChecksum(context);
        return PatchApks.getInstance(context).patchFile(workingPatchApkChecksum);
    }

    static boolean isHotfixApkValid(Context context) {
        File file = getHotFixApk(context);
        return file != null && file.exists();
    }

    static void parsePackage(Context context) {
        if (hasParsePackage) {
            return;
        }
        if (!isHotfixApkValid(context)) {
            return;
        }

        Object mPackageParser;
        Object mPackage;
        try {
            File file = ActivityFinder.getHotFixApk(context);
            Class sPackageParserClass = Class.forName("android.content.pm.PackageParser");
            try {
                mPackageParser = sPackageParserClass.newInstance();
                mPackage = MethodUtils.invokeMethod(mPackageParser, "parsePackage", file, 0);
            } catch (Exception e) {
                mPackageParser = sPackageParserClass.getDeclaredConstructor(String.class)
                        .newInstance(file.getPath());
                DisplayMetrics metrics = new DisplayMetrics();
                metrics.setToDefaults();
                mPackage = MethodUtils.invokeMethod(mPackageParser, "parsePackage", file, file
                        .getPath(), metrics, 0);
            }

            List<Object> tempReceivers = (List<Object>) FieldUtils.readField(mPackage,
                    "receivers");
            if (tempReceivers != null) {
                final ArrayList<Activity> receivers = new ArrayList<>();
                for (int i = tempReceivers.size() - 1; i >= 0; i--) {
                    Object obj = tempReceivers.get(i);
                    ActivityInfo activityInfo = (ActivityInfo) FieldUtils.readField(obj, "info");
                    List<IntentFilter> intentFilters = (List<IntentFilter>) FieldUtils.readField
                            (obj, "intents");
                    Bundle meta = (Bundle) FieldUtils.readField(obj, "metaData");
                    receivers.add(new Activity(meta, intentFilters, activityInfo));
                }
                sReceivers = receivers;
            }

            List<Object> tempProviders = (List<Object>) FieldUtils.readField(mPackage,
                    "providers");
            if (tempProviders != null) {
                sProviders = tempProviders;
            }

            List<Object> tempServices = (List<Object>) FieldUtils.readField(mPackage, "services");
            if (tempServices != null) {
                final ArrayList<Service> services = new ArrayList<>();
                for (Object obj : tempServices) {
                    ServiceInfo serviceInfo = (ServiceInfo) FieldUtils.readField(obj, "info");
                    List<IntentFilter> intentFilters = (List<IntentFilter>) FieldUtils.readField
                            (obj, "intents");
                    Bundle meta = (Bundle) FieldUtils.readField(obj, "metaData");
                    services.add(new Service(meta, intentFilters, serviceInfo));
                }
                sServices = services;
            }

            List<Object> tempActivities = (List<Object>) FieldUtils.readField(mPackage,
                    "activities");
            if (tempActivities != null) {
                final ArrayList<Activity> activities = new ArrayList<>();
                for (Object obj : tempActivities) {
                    ActivityInfo activityInfo = (ActivityInfo) FieldUtils.readField(obj, "info");
                    List<IntentFilter> intentFilters = (List<IntentFilter>) FieldUtils.readField
                            (obj, "intents");
                    Bundle meta = (Bundle) FieldUtils.readField(obj, "metaData");
                    activities.add(new Activity(meta, intentFilters, activityInfo));
                }
                sActivities = activities;
                fillApplicationInfo(context, activities.get(0).activityInfo.applicationInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        hasParsePackage = true;
    }

    static void fillApplicationInfo(Context context, ApplicationInfo applicationInfo) {
        String patchPath = PatchApks.getInstance(context).patchPath(Amigo
                .getWorkingPatchApkChecksum(context));
        applicationInfo.sourceDir = patchPath;
        applicationInfo.publicSourceDir = patchPath;
        applicationInfo.uid = context.getApplicationInfo().uid;
//        applicationInfo.packageName = context.getApplicationContext().getPackageName();
    }

    static class Component {
        List<IntentFilter> filters;
        Bundle metaData;

        Component(Bundle metaData, List<IntentFilter> filters) {
            this.metaData = metaData;
            this.filters = filters;
        }
    }

    static class Service extends Component {
        ServiceInfo serviceInfo;

        Service(Bundle metaData, List<IntentFilter> filters, ServiceInfo serviceInfo) {
            super(metaData, filters);
            this.serviceInfo = serviceInfo;
        }
    }

    static class Activity extends Component {
        ActivityInfo activityInfo;

        Activity(Bundle metaData, List<IntentFilter> filters, ActivityInfo activityInfo) {
            super(metaData, filters);
            this.activityInfo = activityInfo;
        }
    }
}
