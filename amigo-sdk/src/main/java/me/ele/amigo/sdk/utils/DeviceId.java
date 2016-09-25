package me.ele.amigo.sdk.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.UUID;

import static me.ele.amigo.sdk.AmigoSdk.SP_NAME;

public class DeviceId {

    private static final String KEY = "amigo_sdk_device_id";
    private static final File DIR = new File(Environment.getExternalStorageDirectory(), ".amigo-sdk");
    private static final File FILE = new File(DIR, ".device_id");

    private static String deviceId;

    public static String getDeviceId(Context context) {
        if (deviceId == null) {
            SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            deviceId = sp.getString(KEY, "");
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = readFromSetting(context);
                if (TextUtils.isEmpty(deviceId)) {
                    deviceId = readFromSdCard(context);
                    if (TextUtils.isEmpty(deviceId)) {
                        deviceId = generateDeviceId(context);
                        sp.edit().putString(KEY, deviceId).apply();
                        writeToSetting(context, deviceId);
                        writeToSdCard(context, deviceId);
                    }
                }
            }
        }
        return deviceId;
    }

    private static String generateDeviceId(Context context) {
        return new StringBuilder()
                .append(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID))
                .append("-")
                .append(UUID.randomUUID().toString())
                .toString();
    }

    private static String readFromSetting(Context context) {
        try {
            return Settings.System.getString(context.getContentResolver(), KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static void writeToSetting(Context context, String id) {
        try {
            Settings.System.putString(context.getContentResolver(), KEY, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readFromSdCard(Context context) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FILE)));
            String id = reader.readLine();
            reader.close();
            return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static void writeToSdCard(Context context, String id) {
        try {
            if (!DIR.exists()) DIR.mkdir();
            if (FILE.exists()) FILE.delete();
            FILE.createNewFile();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FILE)));
            writer.write(id);
            writer.close();
        } catch (Exception e) {

        }
    }
}
