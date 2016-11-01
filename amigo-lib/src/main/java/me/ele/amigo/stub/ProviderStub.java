package me.ele.amigo.stub;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;

import me.ele.amigo.Amigo;
import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.reflect.MethodUtils;

public class ProviderStub extends ContentProvider {

    private static final String TAG = ProviderStub.class.getSimpleName();

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate");
        return false;
    }

    private Uri mapUri(Uri uri) {
        final StringBuilder targetUrl = new StringBuilder(uri.toString());
        String prefix = "content://" + getContext().getPackageName() + ".provider";
        if (targetUrl.charAt(prefix.length()) != '/') {
            throw new RuntimeException("invalid uri format"); // invalid format
        }
        targetUrl.setCharAt(prefix.length(), '.');
        return Uri.parse(targetUrl.toString());
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.d(TAG, "#query: received query uri [" + uri + "]");
        uri = mapUri(uri);
        Log.d(TAG, "#query: dispatching mapped query uri [" + uri + "]");
        return getContext().getContentResolver().query(uri, projection, selection, selectionArgs,
                sortOrder);
    }

    @Override
    public String getType(Uri uri) {
        Log.d(TAG, "#getType: received query uri [" + uri + "]");
        uri = mapUri(uri);
        Log.d(TAG, "#getType: dispatching mapped query uri [" + uri + "]");
        if (uri == null) {
            return null;
        }
        return getContext().getContentResolver().getType(uri);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "#insert: received query uri [" + uri + "]");
        uri = mapUri(uri);
        Log.d(TAG, "#insert: dispatching mapped query uri [" + uri + "]");
        if (uri == null) {
            return null;
        }
        return getContext().getContentResolver().insert(uri, values);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "#delete: received query uri [" + uri + "]");
        uri = mapUri(uri);
        Log.d(TAG, "#delete: dispatching mapped query uri [" + uri + "]");
        if (uri == null) {
            return 0;
        }
        return getContext().getContentResolver().delete(uri, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.d(TAG, "#update: received query uri [" + uri + "]");
        uri = mapUri(uri);
        Log.d(TAG, "#update: dispatching mapped query uri [" + uri + "]");
        if (uri == null) {
            return 0;
        }
        return getContext().getContentResolver().update(uri, values, selection, selectionArgs);
    }
}
