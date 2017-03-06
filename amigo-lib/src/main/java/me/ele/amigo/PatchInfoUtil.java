package me.ele.amigo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wwm on 11/25/16.
 */

public class PatchInfoUtil {

    public static boolean setWorkingChecksum(Context context, String checksum) {
        return getProvider(context).update(
                Uri.parse(
                        "content://patch_info_provider/set_working_checksum?checksum=" + checksum),
                null) > 0;
    }

    private static PatchInfoProvider getProvider(Context context) {
        return new PatchInfoProvider(context);
    }

    public static String getWorkingChecksum(Context context) {
        Cursor cursor =
                getProvider(context).query(
                        Uri.parse("amigo://patch_info_provider/query_working_checksum"));

        String checksum = "";
        if (cursor != null && cursor.moveToFirst()) {
            checksum = cursor.getString(0);
            cursor.close();
        }

        return checksum;
    }

    public static boolean isDexFileOptimized(Context context, String checksum) {
        Cursor cursor = getProvider(context).query(Uri.parse
                ("amigo://patch_info_provider/is_dex_optimized?checksum=" + checksum));
        boolean optimized = false;
        if (cursor != null && cursor.moveToFirst()) {
            optimized = cursor.getInt(0) == 1;
            cursor.close();
        }

        return optimized;
    }

    public static int updateDexFileOptStatus(Context context, String checksum, boolean optimized) {
        return getProvider(context).update(Uri.parse(
                "amigo://patch_info_provider/set_dex_optimized?checksum="
                        + checksum
                        + "&optimized="
                        + optimized),
                new ContentValues(0));
    }

    public static int updatePatchFileChecksum(Context context, String checksum, Map<String,
            String> checksumMap) {
        final ContentValues contentValues = new ContentValues(0);
        contentValues.put("checkSumMap", toJson(checksumMap));
        return getProvider(context)
                .update(Uri.parse(
                        "amigo://patch_info_provider/update_patch_file_checksum_map?checksum="
                                + checksum),
                        contentValues);
    }

    public static void clear(Context context) {
        getProvider(context).update(Uri.parse("amigo://patch_info_provider/clear_all"), null);
    }

    public static Map<String, String> getPatchFileChecksum(Context context, String checksum) {
        Cursor cursor = getProvider(context).query(
                Uri.parse("amigo://patch_info_provider/query_patch_file_checksum_map?checksum="
                        + checksum));

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
