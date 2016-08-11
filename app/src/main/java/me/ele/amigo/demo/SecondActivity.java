package me.ele.amigo.demo;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.File;

import me.ele.amigo.Amigo;

public class SecondActivity extends AppCompatActivity {

    public static final String TAG = SecondActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "resources--->" + getResources());

        setContentView(R.layout.activity_main);

//        Amigo.clear(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Amigo.work(SecondActivity.this, new File(Environment.getExternalStorageDirectory(), "demo2.apk"));
            }
        });
    }
}
