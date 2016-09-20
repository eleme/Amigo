package me.ele.app.amigo.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class SingleTaskActivity extends AppCompatActivity {

    public static final String TAG = SingleTaskActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "resources--->" + getResources());

        startActivity(new Intent(this, SingleInstanceActivity.class));
        startActivity(new Intent(this, SingleInstanceActivity.class));

        startActivity(new Intent(this, SingleInstanceActivity2.class));
    }
}
