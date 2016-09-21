package me.ele.app.amigo.demo;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ele.amigo.Amigo;
import me.ele.app.amigo.HomeActivity;
import me.ele.amigo.compat.RCompat;
import me.ele.amigo.utils.CommonUtils;
import me.ele.app.amigo.R;

public class DemoActivity extends AppCompatActivity {

    public static final String TAG = DemoActivity.class.getSimpleName();

    public static final String APK_NAME = "amigo_patch.apk";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        setTitle("Demo");

        ButterKnife.bind(this);
    }

    @OnClick(R.id.apply_patch_apk_immediately)
    public void applyPatchApkImmediately(View v) {
        File dir = Environment.getExternalStorageDirectory();
        File patchApkFile = new File(dir, APK_NAME);
        if (!patchApkFile.exists()) {
            Toast.makeText(this, "No amigo_patch.apk found in the directory: " + dir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return;
        }
        boolean patchWorked = Amigo.hasWorked();
        if (!patchWorked) {
            Amigo.work(this, patchApkFile);
            return;
        }
        int workingPatchVersion = Amigo.workingPatchVersion(getApplication());
        if (workingPatchVersion >= CommonUtils.getVersionCode(getApplication(), patchApkFile)) {
            Toast.makeText(this, patchApkFile.getAbsolutePath() + " version must be newer than current working patch", Toast.LENGTH_LONG).show();
            return;
        }
        Amigo.work(this, patchApkFile);
        return;
    }

    @OnClick(R.id.apply_patch_apk_later)
    public void applyPatchApkLater(View v) {
        File dir = Environment.getExternalStorageDirectory();
        File patchApkFile = new File(dir, APK_NAME);
        if (!patchApkFile.exists()) {
            Toast.makeText(this, "No amigo_patch.apk found in the directory: " + dir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return;
        }
        boolean patchWorked = Amigo.hasWorked();
        if (!patchWorked) {
            Amigo.workLater(this, patchApkFile);
            Toast.makeText(this, "waiting for seconds, and kill this app and relaunch the app to check result", Toast.LENGTH_LONG).show();
            return;
        }
        int workingPatchVersion = Amigo.workingPatchVersion(getApplication());
        if (workingPatchVersion >= CommonUtils.getVersionCode(getApplication(), patchApkFile)) {
            Toast.makeText(this, patchApkFile.getAbsolutePath() + " version must be newer than current working patch", Toast.LENGTH_LONG).show();
            return;
        }
        Amigo.workLater(this, patchApkFile);
        Toast.makeText(this, "waiting for seconds, and kill this app and relaunch the app to check result", Toast.LENGTH_LONG).show();
        return;
    }

    @OnClick(R.id.notification)
    void testNotification(View v) {
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

        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.contentIntent = pi;
        notificationManager.notify(0, notification);
    }
}
