package me.ele.amigo.compat;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import me.ele.amigo.reflect.MethodUtils;


public class RCompat {

    public static final String TAG = RCompat.class.getSimpleName();

    private static Resources hostResources;
    private static Resources patchResources;

    public static int getHostIdentifier(Context context, int id) {
        hostResources = getHostResources(context);
        patchResources = getPatchResources(context);
        return hostResources.getIdentifier(patchResources.getResourceEntryName(id),
                patchResources.getResourceTypeName(id), context.getPackageName());
    }

    public static int getPatchIdentifier(Context context, int id) {
        hostResources = getHostResources(context);
        patchResources = getPatchResources(context);
        return patchResources.getIdentifier(hostResources.getResourceEntryName(id), hostResources
                .getResourceTypeName(id), context.getPackageName());
    }

    private static Resources getPatchResources(Context context) {
        if (patchResources != null) {
            return patchResources;
        }

        return patchResources = context.getResources();
    }


    private static Resources getHostResources(Context context) {
        if (hostResources != null) {
            return hostResources;
        }

        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            MethodUtils.invokeMethod(assetManager, "addAssetPath", context.getApplicationInfo()
                    .sourceDir);
            return hostResources = new Resources(assetManager,
                    context.getResources().getDisplayMetrics(),
                    context.getResources().getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
