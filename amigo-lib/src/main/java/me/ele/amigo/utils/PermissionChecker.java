package me.ele.amigo.utils;


import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PermissionChecker {

    private static final String TAG = PermissionChecker.class.getSimpleName();

    public static boolean checkPatchPermission(Context context, File patchApk) {
        try {
            List<String> extraPermissions = new ArrayList<>();
            PackageManager pm = context.getPackageManager();
            String[] hostPs = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
            String[] patchPs = pm.getPackageArchiveInfo(patchApk.getAbsolutePath(), PackageManager.GET_PERMISSIONS).requestedPermissions;

            if (patchPs == null) {
                return true;
            }
            if (hostPs == null) {
                extraPermissions.addAll(Arrays.asList(patchPs));
            } else {
                List<String> patchPsList = new LinkedList<>();
                for (String patchP : patchPs) {
                    patchPsList.add(patchP);
                }
                for (String hostP : hostPs) {
                    patchPsList.remove(hostP);
                }
                extraPermissions.addAll(patchPsList);
            }
            if (extraPermissions.size() == 0) {
                return true;
            } else {
                Log.e(TAG, "patch apk extra permission: " + TextUtils.join(", ", extraPermissions));
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }
}
