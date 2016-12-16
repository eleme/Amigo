package me.ele.amigo.hook;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

import me.ele.amigo.Amigo;
import me.ele.amigo.PatchApks;
import me.ele.amigo.utils.CommonUtils;

import static me.ele.amigo.Amigo.getWorkingPatchApkChecksum;

public class IPackageManagerHookHandle extends BaseHookHandle {

    private static final String TAG = IPackageManagerHookHandle.class.getSimpleName();

    private static Bundle metaData;

    public IPackageManagerHookHandle(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        hookedMethodHandlers.put("getApplicationInfo", new getApplicationInfo(context));
        hookedMethodHandlers.put("getPackageInfo", new getPackageInfo(context));
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
                            if (metaData == null) {
                                metaData = CommonUtils.getPackageArchiveInfo(workingPatchApk,
                                        PackageManager.GET_META_DATA).applicationInfo.metaData;
                            }
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

    private class getPackageInfo extends HookedMethodHandler {
        private int patchVersionCode = 0;
        private String patchVersionName = "0.0.0";

        public getPackageInfo(Context context) {
            super(context);
        }

        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object
                invokeResult) throws Throwable {
            super.afterInvoke(receiver, method, args, invokeResult);
            PackageInfo result = (PackageInfo) invokeResult;

            if (!context.getPackageName().equals(result.packageName)) {
                return;
            }

            if ("0.0.0".equals(patchVersionName)) {
                String checksum = getWorkingPatchApkChecksum(context);
                File patchFile = PatchApks.getInstance(context).patchFile(checksum);
                PackageInfo workingPatchInfo = CommonUtils.getPackageInfo(context, patchFile, 0);
                patchVersionCode = workingPatchInfo.versionCode;
                patchVersionName = workingPatchInfo.versionName;
            }

            result.versionCode = patchVersionCode;
            result.versionName = patchVersionName;
        }
    }
}
