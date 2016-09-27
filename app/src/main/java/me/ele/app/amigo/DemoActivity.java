package me.ele.app.amigo;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
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
import me.ele.amigo.compat.RCompat;
import me.ele.amigo.utils.CommonUtils;
import me.ele.app.amigo.activity.PatchedSingleInstanceActivity;
import me.ele.app.amigo.activity.PatchedSingleInstanceActivity2;
import me.ele.app.amigo.activity.PatchedSingleTaskActivity;
import me.ele.app.amigo.activity.PatchedSingleTaskActivity2;
import me.ele.app.amigo.activity.PatchedSingleTopActivity;
import me.ele.app.amigo.activity.PatchedSingleTopActivity2;
import me.ele.app.amigo.receiver.DemoReceiver;
import me.ele.app.amigo.service.BindService;
import me.ele.app.amigo.service.StartService;

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

    @OnClick(R.id.start_patched_activity)
    public void testStartPatchedActivity() {
        startActivity(new Intent(this, PatchedSingleTopActivity.class));
        startActivity(new Intent(this, PatchedSingleTopActivity.class).putExtra("extra", "extra1"));

        startActivity(new Intent(this, PatchedSingleTopActivity2.class));
        startActivity(new Intent(this, PatchedSingleTopActivity2.class).putExtra("extra", "extra1"));

        startActivity(new Intent(this, PatchedSingleTaskActivity.class));
        startActivity(new Intent(this, PatchedSingleTaskActivity.class).putExtra("extra", "extra1"));

        startActivity(new Intent(this, PatchedSingleTaskActivity2.class));
        startActivity(new Intent(this, PatchedSingleTaskActivity2.class).putExtra("extra", "extra1"));

        startActivity(new Intent(this, PatchedSingleInstanceActivity.class));
        startActivity(new Intent(this, PatchedSingleInstanceActivity.class).putExtra("extra", "extra1"));

        startActivity(new Intent(this, PatchedSingleInstanceActivity2.class));
        startActivity(new Intent(this, PatchedSingleInstanceActivity2.class).putExtra("extra", "extra1"));
    }

    @OnClick(R.id.clear_patch)
    public void clearPatchApk() {
        Amigo.clear(getApplication());
        Toast.makeText(this, "Kill this app, restart the app and check the running apk", Toast.LENGTH_LONG).show();
    }

    @OnClick(R.id.start_new_service)
    public void startNewService() {
        startService(new Intent(this, StartService.class).putExtra(StartService.TAG, "1"));
    }

    @OnClick(R.id.stop_new_service)
    public void stopNewService() {
        stopService(new Intent(this, StartService.class));
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG, "onServiceConnected");
            BindService.LocalBinder binder = (BindService.LocalBinder) service;
            BindService s = binder.getService();
            Log.e(TAG, "random number from service" + s.getRandomNumber());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceConnected");
        }
    };

    @OnClick(R.id.bind_new_service)
    public void bindNewService() {
        bindService(new Intent(this, BindService.class).putExtra(BindService.TAG, "1"), connection, 0);
    }

    @OnClick(R.id.unbind_new_service)
    public void unbindNewService() {
        try {
            unbindService(connection);
        } catch (IllegalArgumentException e) {
            Toast.makeText(DemoActivity.this, "Service not registered", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.trigger_receiver_action)
    public void triggerReceiverAction() {
        sendBroadcast(new Intent("me.ele.test").putExtra(DemoReceiver.TAG, "1"));
    }
}
