package me.ele.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import me.ele.app.amigo.BaseActivity;

import android.util.Log;

import me.ele.app.amigo.R;

public class PatchedSingleTopActivity2 extends BaseActivity {

    public static final String TAG = PatchedSingleTopActivity2.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_second);
        Log.d(TAG, "onCreate: " + hashCode());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: " + intent.getStringExtra("extra"));
    }
}
