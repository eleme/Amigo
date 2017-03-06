package me.ele.amigo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wwm on 11/23/16.
 */

public class PatchInfoProvider {

    private static final String WORKING_PATCH_APK_CHECKSUM = "working_patch_apk_checksum";
    private static final String PARAM_KEY_CHECKSUM = "checksum";

    private Context context;

    public PatchInfoProvider(Context context) {
        this.context = context;
    }

    private static Map<String, String> parseQuery(String query) {
        LinkedHashMap<String, String> queryMap = new LinkedHashMap<>();

        if (TextUtils.isEmpty(query)) {
            return queryMap;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx != -1) {
                try {
                    queryMap.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return queryMap;
    }

    public Cursor query(Uri uri) {
        String command = uri.getLastPathSegment();
        if ("query_working_checksum".equals(command)) {
            String result = queryWorkingChecksum();
            MatrixCursor matrixCursor = new MatrixCursor(new String[] {PARAM_KEY_CHECKSUM}, 1);
            matrixCursor.addRow(new Object[] {result});
            return matrixCursor;
        }

        Map<String, String> params = parseQuery(uri.getQuery());
        String checksum = params.get(PARAM_KEY_CHECKSUM);
        if ("is_dex_optimized".equals(command)) {
            boolean optimized = isDexFileOptimized(checksum);
            MatrixCursor matrixCursor = new MatrixCursor(new String[] {"optimized"}, 1);
            matrixCursor.addRow(new Object[] {optimized ? 1 : 0});
            return matrixCursor;
        }

        if ("query_patch_file_checksum_map".equals(command)) {
            String json = queryPatchChecksumMap(checksum);
            MatrixCursor matrixCursor = new MatrixCursor(new String[] {"json_map"}, 1);
            matrixCursor.addRow(new Object[] {json});
            return matrixCursor;
        }

        return null;
    }

    public int update(Uri uri, ContentValues values) {
        String command = uri.getLastPathSegment();
        Map<String, String> params = parseQuery(uri.getEncodedQuery());
        if ("clear_all".equals(command)) {
            clearLocked();
            return 0;
        }

        String checksum = params.get(PARAM_KEY_CHECKSUM);
        if ("set_working_checksum".equals(command)) {
            return updateWorkingChecksum(checksum) ? 1 : 0;
        }

        if ("set_dex_optimized".equals(command)) {
            boolean optimized = Boolean.parseBoolean(params.get("optimized"));
            return setDexFileOptimized(checksum, optimized) ? 1 : 0;
        }

        if ("update_patch_file_checksum_map".equals(command)) {
            String map = values.getAsString("checkSumMap");
            return updatePatchChecksumMap(checksum, map) ? 1 : 0;
        }

        return 0;
    }

    private boolean updateWorkingChecksum(String newChecksum) {
        writeLocked(WORKING_PATCH_APK_CHECKSUM, newChecksum);
        return true;
    }

    private String queryWorkingChecksum() {
        return String.valueOf(readLocked(WORKING_PATCH_APK_CHECKSUM));
    }

    private boolean isDexFileOptimized(String checksum) {
        return Boolean.valueOf(String.valueOf(readLocked(checksum)));
    }

    private boolean setDexFileOptimized(String checksum, boolean optimized) {
        if (TextUtils.isEmpty(checksum)) {
            return false;
        }
        return writeLocked(checksum, optimized);
    }

    private String queryPatchChecksumMap(String checksum) {
        return String.valueOf(readLocked("patch_checksum_map_" + checksum));
    }

    private boolean updatePatchChecksumMap(String newChecksum, String jsonMap) {
        if (TextUtils.isEmpty(newChecksum)) {
            return false;
        }

        return writeLocked("patch_checksum_map_" + newChecksum, jsonMap);
    }

    /*
     in order to sharing data between the main process and load patch process, we have to make sure
     the two of them don't modify the underlying file concurrently and ensure the changes made by
     any of them are visible to the other
    */
    private void clearLocked() {
        synchronized (PatchInfoProvider.class) {
            FileLock lock = null;
            RandomAccessFile raf = null;
            try {
                File file = getPatchInfoFile();
                if (!file.exists()) {
                    return;
                }
                raf = new RandomAccessFile(file, "rw");
                FileChannel channel = raf.getChannel();
                lock = channel.lock();
                raf.setLength(0);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (lock != null) {
                        lock.release();
                    }
                    if (raf != null) {
                        raf.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean writeLocked(String key, Object value) {
        synchronized (PatchInfoProvider.class) {
            FileLock lock = null;
            RandomAccessFile raf = null;
            try {
                File file = getPatchInfoFile();
                raf = new RandomAccessFile(file, "rw");
                FileChannel channel = raf.getChannel();
                lock = channel.lock();
                byte[] content = new byte[(int) raf.length()];
                raf.read(content);
                JSONObject object = content.length > 0 ? new JSONObject(new String(content))
                        : new JSONObject();
                object.put(key, value);
                raf.setLength(0);
                raf.write(object.toString().getBytes("UTF-8"));
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (lock != null) {
                        lock.release();
                    }
                    if (raf != null) {
                        raf.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }

    private Object readLocked(String key) {
        synchronized (PatchInfoProvider.class) {
            FileLock lock = null;
            RandomAccessFile raf = null;
            try {
                File file = getPatchInfoFile();
                raf = new RandomAccessFile(file, "rw");
                FileChannel channel = raf.getChannel();
                lock = channel.lock();
                int contentLength = (int) raf.length();
                if (contentLength == 0) {
                    return null;
                }
                byte[] content = new byte[contentLength];
                raf.read(content);
                return new JSONObject(new String(content)).optString(key);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (lock != null) {
                        lock.release();
                    }
                    if (raf != null) {
                        raf.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private File getPatchInfoFile() {
        return AmigoDirs.getInstance(context).patchInfoFile();
    }
}
