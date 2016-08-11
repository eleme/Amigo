package me.ele.amigo.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import me.ele.amigo.Amigo;

public class SecondActivity extends AppCompatActivity {

    public static final String TAG = SecondActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "resources--->" + getResources());

        setContentView(R.layout.activity_main);

        Amigo.clear(this);
    }
}
