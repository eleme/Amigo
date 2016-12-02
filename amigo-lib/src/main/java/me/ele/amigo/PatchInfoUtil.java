package me.ele.amigo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by wwm on 11/25/16.
 */

public class PatchInfoUtil {

    private static String URI_PREFIX = "";

    public static boolean setWorkingChecksum(Context context, String checksum) {
        ensureProviderAuthroties(context);

        return context.getContentResolver().update(Uri.parse(URI_PREFIX +
                        "set_working_checksum?checksum=" + checksum),
                new ContentValues(0), null, null) > 0;
    }

    private static void ensureProviderAuthroties(Context context) {
        if (TextUtils.isEmpty(URI_PREFIX)) {
            URI_PREFIX = String.format("content://%s.amigo_patch_info_provider/",
                    context.getPackageName());
        }
    }

    public static String getWorkingChecksum(Context context) {
        ensureProviderAuthroties(context);
        Cursor cursor = context.getContentResolver().query(Uri.parse
                        (URI_PREFIX + "query_working_checksum"), null,
                null, null, "");

        String checksum = "";
        if (cursor != null && cursor.moveToFirst()) {
            checksum = cursor.getString(0);
            cursor.close();
        }

        return checksum;
    }

    public static boolean isDexFileOptimized(Context context, String checksum) {
        ensureProviderAuthroties(context);
        Cursor cursor = context.getContentResolver().query(Uri.parse
                        (URI_PREFIX + "is_dex_optimized?checksum=" + checksum), null,
                null, null, "");
        boolean optimized = false;
        if (cursor != null && cursor.moveToFirst()) {
            optimized = cursor.getInt(0) == 1;
            cursor.close();
        }

        return optimized;
    }

    public static int updateDexFileOptStatus(Context context, String checksum, boolean optimized) {
        ensureProviderAuthroties(context);
        return context.getContentResolver().update(Uri.parse(URI_PREFIX +
                        "set_dex_optimized?checksum=" + checksum + "&optimized=" + optimized),
                new ContentValues(0),
                null, null);
    }

    public static int updatePatchFileChecksum(Context context, String checksum, Map<String,
            String> checksumMap) {
        ensureProviderAuthroties(context);
        return context.getContentResolver().update(Uri.parse
                        (URI_PREFIX + "update_patch_file_checksum_map?checksum=" + checksum +
                                "&map=" + toJson(checksumMap)),
                new ContentValues(0), null, null);
    }

    public static void clear(Context context) {
        ensureProviderAuthroties(context);
        context.getContentResolver().update(Uri.parse
                        (URI_PREFIX + "clear_all"), new ContentValues(0), null,
                null);
    }

    public static Map<String, String> getPatchFileChecksum(Context context, String checksum) {
        ensureProviderAuthroties(context);
        Cursor cursor = context.getContentResolver().query(Uri.parse
                        (URI_PREFIX + "query_patch_file_checksum_map?checksum=" + checksum),
                null, null, null, null);

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
