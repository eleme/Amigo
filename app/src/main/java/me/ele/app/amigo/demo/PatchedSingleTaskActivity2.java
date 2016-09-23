package me.ele.app.amigo.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class PatchedSingleTaskActivity2 extends AppCompatActivity {

    public static final String TAG = PatchedSingleTaskActivity2.class.getSimpleName();

    static int count = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "resources--->" + getResources());

        if(count++ == 0) {
            startActivity(new Intent(this, PatchedSingleTaskActivity2.class).putExtra("extra", "extra2"));
        }

//        startActivity(new Intent(this, PatchedSingleInstanceActivity.class));
//        startActivity(new Intent(this, PatchedSingleInstanceActivity.class));
//        startActivity(new Intent(this, PatchedSingleInstanceActivity.class));

        Log.d(TAG, "onCreate: " + hashCode());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: " + intent.getStringExtra("extra"));
    }
}
