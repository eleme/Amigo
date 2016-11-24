package me.ele.amigo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import me.ele.amigo.utils.CommonUtils;

/**
 * Created by wwm on 11/23/16.
 */

public class PatchInfoProvider extends ContentProvider {

    public static final String SP_NAME = "Amigo";
    public static final String WORKING_PATCH_APK_CHECKSUM = "working_patch_apk_checksum";
    public static final String WORKING_CURRENT_VERSION_CODE = "current_version_code";
    public static final String WORKING_CURRENT_VERSION_NAME = "current_version_name";

    SharedPreferences sharedPreferences = null;

    private static final String PARAM_KEY_CHECKSUM = "checksum";

    @Override
    public boolean onCreate() {
        sharedPreferences = getContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        String path = uri.getLastPathSegment();

        if ("query_working_checksum".equals(path)) {
            String result = queryWorkingChecksum();
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{PARAM_KEY_CHECKSUM}, 1);
            matrixCursor.addRow(new Object[]{result});
            return matrixCursor;
        }

        if ("query_host_version_when_patch_applied".equals(path)) {
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"version_name",
                    "version_code"}, 1);
            matrixCursor.addRow(new Object[]{getHostVersionNameWhenPatchApplied(),
                    getHostVersionCodeWhenPatchApplied()});
            return matrixCursor;
        }

        Map<String, String> params = parseQuery(uri.getEncodedQuery());
        String checksum = params.get(PARAM_KEY_CHECKSUM);
        if ("is_dex_optimized".equals(path)) {
            boolean optimized = isDexFileOptimized(checksum);
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"optimized"}, 1);
            matrixCursor.addRow(new Object[]{optimized ? 1 : 0});
            return matrixCursor;
        }

        if ("query_patch_file_checksum_map".equals(path)) {
            String json = queryPatchChecksumMap(checksum);
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"json_map"}, 1);
            matrixCursor.addRow(new Object[]{json});
            return matrixCursor;
        }

        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String lastPathSegment = uri.getLastPathSegment();
        Map<String, String> params = parseQuery(uri.getEncodedQuery());

        if ("clear_all".equals(lastPathSegment)) {
            sharedPreferences.edit().clear().commit();
            return 0;
        }

        String checksum = params.get(PARAM_KEY_CHECKSUM);
        if ("set_working_checksum".equals(lastPathSegment)) {
            return updateWorkingChecksum(checksum) ? 1 : 0;
        }

        if ("set_dex_optimized".equals(lastPathSegment)) {
            boolean optimized = Boolean.parseBoolean(params.get("optimized"));
            return setDexFileOptimized(checksum, optimized) ? 1 : 0;
        }

        if ("update_patch_file_checksum_map".equals(lastPathSegment)) {
            String map = params.get("map");
            return updatePatchChecksumMap(checksum, map) ? 1 : 0;
        }

        return 0;
    }

    private boolean updateWorkingChecksum(String newChecksum) {
        if (TextUtils.isEmpty(newChecksum))
            return false;

        sharedPreferences.edit()
                .putInt(WORKING_CURRENT_VERSION_CODE, CommonUtils.getVersionCode
                        (getContext()))
                .putString(WORKING_CURRENT_VERSION_NAME, CommonUtils
                        .getVersionName(getContext()))
                .putString(WORKING_PATCH_APK_CHECKSUM, newChecksum).commit();
        return true;
    }

    private int getHostVersionCodeWhenPatchApplied() {
        return sharedPreferences.getInt(WORKING_CURRENT_VERSION_CODE, -1);
    }

    private String getHostVersionNameWhenPatchApplied() {
        return sharedPreferences.getString(WORKING_CURRENT_VERSION_NAME, "");
    }

    private String queryWorkingChecksum() {
        return sharedPreferences.getString(WORKING_PATCH_APK_CHECKSUM, "");
    }

    private boolean isDexFileOptimized(String checksum) {
        return sharedPreferences.getBoolean(checksum, false);
    }

    private boolean setDexFileOptimized(String checksum, boolean optimized) {
        if (TextUtils.isEmpty(checksum)) {
            return false;
        }

        sharedPreferences.edit().putBoolean(checksum, optimized).commit();
        return true;
    }

    private String queryPatchChecksumMap(String checksum) {
        return sharedPreferences.getString("patch_checksum_map_" + checksum, "");
    }

    private boolean updatePatchChecksumMap(String checksum, String jsonMap) {
        if (TextUtils.isEmpty(checksum)) {
            return false;
        }

        sharedPreferences.edit().putString("patch_checksum_map_" + checksum, jsonMap).commit();
        return true;
    }

    private static Map<String, String> parseQuery(String query) {
        LinkedHashMap<String, String> queryMap = new LinkedHashMap<>();

        if (TextUtils.isEmpty(query)) {
            return queryMap;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx != -1)
                try {
                    queryMap.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder
                            .decode
                                    (pair.substring(idx + 1), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
        }
        return queryMap;
    }
}
