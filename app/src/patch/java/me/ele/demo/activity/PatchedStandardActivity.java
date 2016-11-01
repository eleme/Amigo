package me.ele.demo.activity;

import android.content.Intent;
import android.os.Bundle;

import me.ele.app.amigo.BaseActivity;

import android.util.Log;

/**
 * Created by wwm on 9/21/16.
 */

public class PatchedStandardActivity extends BaseActivity {
    private static final String TAG = PatchedStandardActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: " + hashCode());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: " + intent);
    }
}
