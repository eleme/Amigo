package me.ele.amigo.utils.component;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.reflect.MethodUtils;
import me.ele.amigo.utils.ArrayUtil;

import static me.ele.amigo.compat.ActivityThreadCompat.instance;

public class ContentProviderFinder extends ComponentFinder {

    private static final String TAG = ContentProviderFinder.class.getSimpleName();

    public static ProviderInfo[] getAppContentProvider(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getApplicationContext().getPackageName(), PackageManager.GET_PROVIDERS);
            return packageInfo.providers;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ProviderInfo[] getNewContentProvider(Context context) {
        parsePackage(context);
        ProviderInfo[] providersInPatch = getProviderInfo(context, sProviders);
        ProviderInfo[] providersInHost = getAppContentProvider(context);

        if (ArrayUtil.isEmpty(providersInPatch)) {
            return null;
        }

        if (ArrayUtil.isEmpty(providersInHost)) {
            return providersInPatch;
        }

        ArrayList<ProviderInfo> newProviders = new ArrayList<>();
        for (ProviderInfo patchProvider : providersInPatch) {
            boolean isNew = true;
            for (ProviderInfo hostProvider : providersInHost) {
                if (hostProvider.name.equals(patchProvider.name)) {
                    isNew = false;
                    break;
                }
            }
            if (isNew) newProviders.add(patchProvider);
        }
        return newProviders.toArray(new ProviderInfo[newProviders.size()]);
    }

    private static ProviderInfo[] getProviderInfo(Context context, List<Object> components) {
        int size = components == null ? 0 : components.size();
        if (size == 0) {
            return null;
        }

        final ProviderInfo[] providerInfos = new ProviderInfo[size];
        try {
            for (int i = 0; i < size; i++) {
                providerInfos[i] = (ProviderInfo) FieldUtils.readField(components.get(i),
                        "info");
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return providerInfos;
    }

    public static void installPatchContentProviders(Context context) {
        ProviderInfo[] providers = getNewContentProvider(context);
        if (ArrayUtil.isEmpty(providers)) {
            Log.d(TAG, "installPatchContentProviders: there is no any new provider");
            return;
        }

        Log.d(TAG, "installPatchContentProviders: " + Arrays.toString(providers));
        try {
            MethodUtils.invokeMethod(instance(), "installContentProviders",
                    new Object[]{context, Arrays.asList(providers)},
                    new Class<?>[]{Context.class, List.class});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
