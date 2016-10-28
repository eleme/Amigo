package me.ele.amigo;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.ArrayMap;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;

import static me.ele.amigo.compat.ActivityThreadCompat.instance;
import static me.ele.amigo.reflect.FieldUtils.getField;
import static me.ele.amigo.reflect.FieldUtils.readField;
import static me.ele.amigo.reflect.FieldUtils.writeField;
import static me.ele.amigo.reflect.MethodUtils.invokeMethod;
import static me.ele.amigo.reflect.MethodUtils.invokeStaticMethod;

class PatchResourceLoader {

    private static AssetManager originalAssetManager = null;

    static void loadPatchResources(Context context, String checksum) throws Exception {
        AssetManager newAssetManager = AssetManager.class.newInstance();
        invokeMethod(newAssetManager, "addAssetPath", PatchApks.getInstance(context).patchPath(checksum));
        invokeMethod(newAssetManager, "ensureStringBlocks");
        replaceAssetManager(context, newAssetManager);
    }

    private static void replaceAssetManager(Context context, AssetManager newAssetManager)
            throws Exception {
        Collection<WeakReference<Resources>> references;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Class<?> resourcesManagerClass = Class.forName("android.app.ResourcesManager");
            Object resourcesManager = invokeStaticMethod(resourcesManagerClass, "getInstance");

            if (getField(resourcesManagerClass, "mActiveResources") != null) {
                ArrayMap<?, WeakReference<Resources>> arrayMap =
                        (ArrayMap) readField(resourcesManager, "mActiveResources", true);
                references = arrayMap.values();
            } else {
                references = (Collection) readField(resourcesManager, "mResourceReferences", true);
            }
        } else {
            HashMap<?, WeakReference<Resources>> map =
                    (HashMap) readField(instance(), "mActiveResources", true);
            references = map.values();
        }

        AssetManager assetManager = context != null ? context.getAssets() : null;
        for (WeakReference<Resources> wr : references) {
            Resources resources = wr.get();
            if (resources == null) continue;

            try {
                writeField(resources, "mAssets", newAssetManager);
                originalAssetManager = assetManager;
            } catch (Throwable ignore) {
                Object resourceImpl = readField(resources, "mResourcesImpl", true);
                writeField(resourceImpl, "mAssets", newAssetManager);
            }

            resources.updateConfiguration(resources.getConfiguration(),
                    resources.getDisplayMetrics());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (WeakReference<Resources> wr : references) {
                Resources resources = wr.get();
                if (resources == null) continue;

                // android.util.Pools$SynchronizedPool<TypedArray>
                Object typedArrayPool = readField(resources, "mTypedArrayPool", true);

                // Clear all the pools
                while (invokeMethod(typedArrayPool, "acquire") != null) ;
            }
        }
    }

    static void revertLoadPatchResources() throws Exception {
        final AssetManager assetManager = originalAssetManager;
        originalAssetManager = null;
        if (assetManager != null) {
            replaceAssetManager(null, assetManager);
        }
    }
}
