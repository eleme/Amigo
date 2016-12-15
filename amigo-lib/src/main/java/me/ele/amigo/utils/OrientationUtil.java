package me.ele.amigo.utils;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

public class OrientationUtil {

    private static final Map<String, Integer> map = new HashMap<>();

    static {
        map.put("unspecified", ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        map.put("behind", ActivityInfo.SCREEN_ORIENTATION_BEHIND);
        map.put("landscape", ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        map.put("portrait", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        map.put("reverseLandscape", ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        map.put("reversePortrait", ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        map.put("sensorLandscape", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        map.put("sensorPortrait", ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        map.put("userLandscape", ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        map.put("userPortrait", ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        map.put("sensor", ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        map.put("fullSensor", ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        map.put("nosensor", ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        map.put("user", ActivityInfo.SCREEN_ORIENTATION_USER);
        map.put("fullUser", ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        map.put("locked", ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    private static int parseOrientation(String str) {
        if (!map.containsKey(str)) {
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        return map.get(str);
    }

    public static int getReleaseActivityOrientation(Context context) {
        try {
            Bundle metaData = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA).applicationInfo.metaData;
            if (metaData != null) {
                return parseOrientation(metaData.getString("amigo_orientation"));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }
}
