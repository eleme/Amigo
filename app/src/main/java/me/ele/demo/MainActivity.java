package me.ele.demo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.File;

import me.ele.amigo.Amigo;
import me.ele.amigo.compat.RCompat;


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

        findViewById(R.id.notification_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hotfixNotification();
            }
        });
    }

    private void hotfixNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(RCompat.getIdentifier(this, R.drawable.ic_ac_grade_lv0));

        Notification notification = mBuilder.build();

        Log.e(TAG, "custom_notification id--->" + R.layout.custom_notification);
        Log.e(TAG, "custom_notification id--->" + RCompat.getIdentifier(this, R.layout.custom_notification));
        RemoteViews contentView = new RemoteViews(getPackageName(), RCompat.getIdentifier(this, R.layout.custom_notification));
        contentView.setImageViewResource(RCompat.getIdentifier(this, R.id.n_image), RCompat.getIdentifier(this, R.drawable.ic_account_mobile));
        contentView.setTextViewText(RCompat.getIdentifier(this, R.id.n_text), getResources().getString(R.string.added_string1));
        contentView.setTextColor(RCompat.getIdentifier(this, R.id.n_text), getResources().getColor(R.color.blue));
        notification.contentView = contentView;
        notification.defaults = Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.when = System.currentTimeMillis();

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.contentIntent = pi;
        notificationManager.notify(0, notification);
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
