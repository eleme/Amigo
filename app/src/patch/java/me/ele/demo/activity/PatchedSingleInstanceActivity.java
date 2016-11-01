package me.ele.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import me.ele.app.amigo.BaseActivity;

/**
 * Created by wwm on 9/20/16.
 */
public class PatchedSingleInstanceActivity extends BaseActivity {

    private static final String TAG = PatchedSingleInstanceActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: " + hashCode());

//        startActivity(new Intent(this, PatchedSingleInstanceActivity2.class));
//        startActivity(new Intent(this, PatchedSingleInstanceActivity2.class));

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: " + intent.getStringExtra("extra"));
    }
}
