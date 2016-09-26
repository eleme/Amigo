package me.ele.amigo.sdk.utils;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

public class DeviceId {

    private static String deviceId;

    public static String getDeviceId(Context context) {
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = generateDeviceId(context);
        }
        return deviceId;
    }

    private static String generateDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
