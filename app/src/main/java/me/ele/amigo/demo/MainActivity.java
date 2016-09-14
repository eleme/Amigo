package me.ele.amigo.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.io.File;

import me.ele.amigo.Amigo;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.e(TAG, "onCreate");
        Log.e(TAG, "getApplication from MainActivity-->" + getApplication());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
//                crash();
            }
        });

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Main2Activity.class));
            }
        });

        findViewById(R.id.apply_patch_apk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPatchApk();
            }
        });

//        findViewById(R.id.show_notification).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showNotification();
//            }
//        });
    }

    private void showNotification() {
////        NotificationManagerCompat.from(this).cancel(100);
////        Log.d(TAG, "showNotification: cancel notification 100");
//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
//                .setSmallIcon(R.drawable.bk_icon_arrow)
//                .setContentTitle("My notification")
//                .setContent(new MyRemoteViews(getApplication().getPackageName(), R.layout.notification_layout))
//                .setAutoCancel(true);
//        NotificationManagerCompat.from(this).notify(100, mBuilder.build());
//
//        View v = getLayoutInflater().inflate(R.layout.notification_layout, null);
//        Log.d(TAG, "showNotification: " + v.getTag());
    }

    private void crash() {
        throw new RuntimeException("mock crash");
    }

    public void applyPatchApk() {
        File fixedApkFile = new File(Environment.getExternalStorageDirectory(), "demo.apk");
        if (fixedApkFile.exists()) {
            Amigo.work(this, fixedApkFile);
//             Amigo.workLater(this, fixedApkFile);
        }
    }

}
