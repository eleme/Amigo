package me.ele.amigo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wwm on 11/25/16.
 */

public class PatchInfoUtil {

    public static boolean setWorkingChecksum(Context application, String checksum) {
        return getPatchInfoProvider(application).update(Uri.parse(
                "amigo://patch_info_provider/set_working_checksum?checksum=" + checksum), null) > 0;
    }

    public static String getWorkingChecksum(Context application) {
        Cursor cursor =
                getPatchInfoProvider(application).query(Uri.parse("amigo://patch_info_provider/query_working_checksum"));

        String checksum = "";
        if (cursor != null && cursor.moveToFirst()) {
            checksum = cursor.getString(0);
            cursor.close();
        }
        return checksum;
    }

    public static boolean isDexFileOptimized(Context application, String checksum) {
        Cursor cursor = getPatchInfoProvider(application).query(
                Uri.parse("amigo://patch_info_provider/is_dex_optimized?checksum=" + checksum));
        boolean optimized = false;
        if (cursor != null && cursor.moveToFirst()) {
            optimized = cursor.getInt(0) == 1;
            cursor.close();
        }
        return optimized;
    }

    private static PatchInfoProvider getPatchInfoProvider(Context application) {
        PatchInfoProvider provider = new PatchInfoProvider(application);
        return provider;
    }

    /*actually flag optimized just means whether the dex opt job is finished or not*/
    public static int updateDexFileOptStatus(Context application, String checksum, boolean optimized) {
        return getPatchInfoProvider(application).update(Uri.parse(
                "amigo://patch_info_provider/set_dex_optimized?checksum=" + checksum + "&optimized=" + optimized), null);
    }

    public static int updatePatchFileChecksum(Context application, String checksum, Map<String,
            String> checksumMap) {
        ContentValues values = new ContentValues(1);
        values.put("map", toJson(checksumMap));
        return getPatchInfoProvider(application).update(Uri.parse
                ("amigo://patch_info_provider/update_patch_file_checksum_map?checksum=" + checksum), values);
    }

    public static void clear(Context application) {
        getPatchInfoProvider(application).update(Uri.parse("amigo://patch_info_provider/clear_all"), null);
    }

    public static Map<String, String> getPatchFileChecksum(Context application, String checksum) {
        Cursor cursor = getPatchInfoProvider(application).query(
                Uri.parse("amigo://patch_info_provider/query_patch_file_checksum_map?checksum=" + checksum));

        if (cursor == null || !cursor.moveToFirst()) {
            return new HashMap<>(0);
        }

        HashMap<String, String> map = new HashMap<>();
        String json = cursor.getString(0);
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (jsonObject != null) {
            Iterator<String> iterable = jsonObject.keys();
            while (iterable.hasNext()) {
                String key = iterable.next();
                map.put(key, jsonObject.optString(key));
            }
        }
        cursor.close();
        return map;
    }

    private static String toJson(Map<String, String> checksumMap) {
        if (checksumMap.isEmpty()) {
            return "";
        }

        JSONObject jsonObject = new JSONObject(checksumMap);
        return jsonObject.toString();
    }
}
