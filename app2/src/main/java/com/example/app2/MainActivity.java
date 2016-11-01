package com.example.app2;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.test_provider)
    public void testProvider() {
        Cursor cursor = getContentResolver().query(
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
    }

}
