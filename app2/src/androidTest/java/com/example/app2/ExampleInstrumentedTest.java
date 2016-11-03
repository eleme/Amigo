package com.example.app2;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private static final String TAG = "MainActivity";

    @Test
    public void testRemoteProvider() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        Cursor cursor = appContext.getContentResolver().query(
                Uri.parse("content://me.ele.app.amigo.provider/student?id=0"),
                null, null, null, null);
        Log.d(TAG, "testPatchedProvider: patched provider loaded ? " + (cursor != null));

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String gender = cursor.getInt(2) == 0 ? "male" : "female";
                Log.d(TAG, "testPatchedProvider: student[id="
                        + id
                        + ", name="
                        + name
                        + ", gender="
                        + gender
                        + "]");
            }
            cursor.close();
        }

        assertTrue(cursor !=null);
    }
}
