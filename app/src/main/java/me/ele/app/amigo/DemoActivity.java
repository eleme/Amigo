package me.ele.app.amigo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;

import me.ele.amigo.Amigo;
import me.ele.amigo.compat.RCompat;

public class DemoActivity extends BaseActivity {

    public static final String TAG = DemoActivity.class.getSimpleName();

    public static final String APK_NAME = "amigo_patch.apk";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
    }

    public void applyPatchApkImmediately(View v) {
        File dir = Environment.getExternalStorageDirectory();
        File patchApkFile = new File(dir, APK_NAME);
        if (!patchApkFile.exists()) {
            Toast.makeText(this,
                    "No amigo_patch.apk found in the directory: " + dir.getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        boolean patchWorked = Amigo.hasWorked();
        Amigo.work(this, patchApkFile);
    }

    public void applyPatchApkLater(View v) {
        File dir = Environment.getExternalStorageDirectory();
        File patchApkFile = new File(dir, APK_NAME);
        if (!patchApkFile.exists()) {
            Toast.makeText(this,
                    "No amigo_patch.apk found in the directory: " + dir.getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Amigo.workLater(this, patchApkFile, new Amigo.WorkLaterCallback() {
            @Override
            public void onPatchApkReleased(boolean success) {
                Toast.makeText(DemoActivity.this, "dex-opt finished : " + success , Toast.LENGTH_SHORT).show();
            }
        });
        Toast.makeText(this,
                "waiting for seconds, and kill this app and relaunch the app to check result",
                Toast.LENGTH_SHORT).show();
    }

    public void clearPatchApk(View v) {
        Amigo.clear(getApplication());
        Toast.makeText(this, "Kill this app, restart the app and check the running apk",
                Toast.LENGTH_SHORT).show();
    }

    public void testNotification(View v) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(RCompat.getHostIdentifier(this, R.drawable.ic_ac_grade_lv0));

        Notification notification = mBuilder.build();

        Log.e(TAG, "custom_notification id--->" + R.layout.custom_notification);
        Log.e(TAG, "custom_notification id--->" + RCompat.getHostIdentifier(this,
                R.layout.custom_notification));
        RemoteViews contentView = new RemoteViews(getPackageName(),
                RCompat.getHostIdentifier(this, R.layout.custom_notification));
        contentView.setImageViewResource(RCompat.getHostIdentifier(this, R.id.n_image),
                RCompat.getHostIdentifier(this, R.drawable.ic_account_mobile));
        contentView.setTextViewText(RCompat.getHostIdentifier(this, R.id.n_text),
                getResources().getString(R.string.added_string1));
        contentView.setTextColor(RCompat.getHostIdentifier(this, R.id.n_text),
                getResources().getColor(R.color.blue));
        notification.contentView = contentView;
        notification.defaults =
                Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification
                        .DEFAULT_LIGHTS;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.when = System.currentTimeMillis();

        Intent intent = new Intent(this, HomeActivity.class);
        PendingIntent pi =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.contentIntent = pi;
        notificationManager.notify(0, notification);
    }

    public void testPatchedActivities(View v) {
        startActivity(new Intent(this, TestPatchedActivities.class).putExtra("test", new
                ParcelBean("jack", 1)));
    }

    public void testPatchedServices(View v) {
        startActivity(new Intent(this, TestPatchedServices.class));
    }

    public void triggerReceiverAction(View v) {
        sendBroadcast(new Intent("me.ele.test").putExtra("DemoReceiver", "1"));
    }

    public void testPatchedProvider(View v) {
        Cursor cursor = getContentResolver().query(
                Uri.parse("content://me.ele.app.amigo.provider.student?id=0"),
                null, null, null, null);
        Log.d(TAG, "testPatchedProvider: patched provider loaded ? " + (cursor != null));
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String gender = cursor.getInt(2) == 0 ? "male" : "female";
                Log.d(TAG, "testPatchedProvider: student[id="
                        + id
                        + ", name="
                        + name
                        + ", gender="
                        + gender
                        + "]");
            }
            cursor.close();
        }
    }

    public void openWebActivity(View view) {
        startActivity(new Intent(this, WebActivity.class));
    }
}
