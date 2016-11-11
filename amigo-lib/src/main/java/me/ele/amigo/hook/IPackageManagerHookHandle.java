package me.ele.amigo.hook;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import me.ele.amigo.Amigo;
import me.ele.amigo.PatchApks;
import me.ele.amigo.utils.CommonUtils;

import static me.ele.amigo.reflect.FieldUtils.readField;
import static me.ele.amigo.reflect.FieldUtils.writeField;

public class IPackageManagerHookHandle extends BaseHookHandle {

    private static final String TAG = IPackageManagerHookHandle.class.getSimpleName();

    private static Bundle metaData;

    public IPackageManagerHookHandle(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        hookedMethodHandlers.put("getApplicationInfo", new getApplicationInfo(context));
        hookedMethodHandlers.put("getInstalledApplications", new getInstalledApplications(context));
    }

    private class getApplicationInfo extends HookedMethodHandler {


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
            fixAppInfo(((ApplicationInfo) invokeResult));
            super.afterInvoke(receiver, method, args, invokeResult);
        }
    }

    private class getInstalledApplications extends HookedMethodHandler {
        public getInstalledApplications(Context context) {
            super(context);
        }

        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object
                invokeResult) throws Throwable {
            try {
                List<ApplicationInfo> list = null;
                if (invokeResult instanceof List) {
                    list = (List<ApplicationInfo>) invokeResult;
                } else {
                    try {
                        list = (List<ApplicationInfo>) readField(invokeResult, "mList", true);
                    } catch (Exception e1) {
                        Parcel mParcel = (Parcel) readField(invokeResult, "mParcel", true);
                        int mNumItems = (int) readField(invokeResult, "mNumItems", true);
                        mParcel.setDataPosition(0);
                        List<ApplicationInfo> ll = new ArrayList<>();
                        for (int i = 0; i < mNumItems; i++) {
                            ApplicationInfo item = ApplicationInfo.CREATOR.createFromParcel
                                    (mParcel);
                            ll.add(item);
                        }
                        mParcel.recycle();

                        mParcel = Parcel.obtain();

                        for (ApplicationInfo info : ll) {
                            if (info.packageName.equals(context.getPackageName())) {
                                fixAppInfo(info);
                            }
                            info.writeToParcel(mParcel, 0);
                        }
                        writeField(invokeResult, "mParcel", mParcel);
                        return;
                    }
                }
                if (list == null || list.size() == 0) {
                    return;
                }

                for (ApplicationInfo info : list) {
                    if (info.packageName.equals(context.getPackageName())) {
                        fixAppInfo(info);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            super.afterInvoke(receiver, method, args, invokeResult);
        }
    }

    private void fixAppInfo(ApplicationInfo info) {
        String workingPatch = PatchApks.getWorkingPatchPath(context);
        ApplicationInfo patchAppInfo = context.getPackageManager()
                .getPackageArchiveInfo(workingPatch, 0).applicationInfo;
        String patchPath = PatchApks.getWorkingPatchPath(context);
        info.sourceDir = patchPath;
        info.publicSourceDir = patchPath;
        info.uid = context.getApplicationInfo().uid;
        info.icon = patchAppInfo.icon;
        info.theme = patchAppInfo.theme;
        info.labelRes = patchAppInfo.labelRes;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            info.logo = patchAppInfo.logo;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            info.banner = patchAppInfo.banner;
        }
    }

}
