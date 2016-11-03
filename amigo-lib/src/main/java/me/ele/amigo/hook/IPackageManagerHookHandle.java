package me.ele.amigo.hook;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Method;

import me.ele.amigo.Amigo;
import me.ele.amigo.PatchApks;

public class IPackageManagerHookHandle extends BaseHookHandle {

    private static final String TAG = IPackageManagerHookHandle.class.getSimpleName();

    private static Bundle metaData;

    public IPackageManagerHookHandle(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        sHookedMethodHandlers.put("getApplicationInfo", new getApplicationInfo(context));
    }

    private static class getApplicationInfo extends HookedMethodHandler {


        public getApplicationInfo(Context context) {
            super(context);
        }


        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object
                invokeResult) throws Throwable {
            if (args == null || args.length == 0) {
                return;
            }
            if (!args[0].equals(context.getPackageName())) {
                return;
            }
            for (Object arg : args) {
                if ((arg.getClass() == int.class || arg.getClass() == Integer.class)
                        && ((int) arg) == PackageManager.GET_META_DATA) {
                    try {
                        if (metaData == null) {
                            String checksum = Amigo.getWorkingPatchApkChecksum(context);
                            String workingPatchApk = PatchApks.getInstance(context).patchPath
                                    (checksum);
                            metaData = context.getPackageManager().getPackageArchiveInfo
                                    (workingPatchApk, PackageManager.GET_META_DATA)
                                    .applicationInfo.metaData;
                        }
                        ((ApplicationInfo) invokeResult).metaData = metaData;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "add metaData fails");
                    }

                }
            }
            super.afterInvoke(receiver, method, args, invokeResult);
        }
    }
}
