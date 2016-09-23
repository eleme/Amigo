package me.ele.app.amigo.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class PatchedSingleTaskActivity extends AppCompatActivity {

    static int count = 0;
    public static final String TAG = PatchedSingleTaskActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "resources--->" + getResources());

        if (count++ == 0) {
            startActivity(new Intent(this, PatchedSingleTaskActivity.class).putExtra("extra", "extra2"));
        }

        Log.d(TAG, "onCreate: " + hashCode());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: " + intent.getStringExtra("extra"));
    }
}
