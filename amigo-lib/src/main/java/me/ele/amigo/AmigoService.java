package me.ele.amigo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import static me.ele.amigo.utils.ProcessUtils.isMainProcessRunning;


public class AmigoService extends Service {

    private static final String TAG = AmigoService.class.getSimpleName();
    public static final int WHAT = 0;
    public static final int DELAY = 200;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT:
                    Context context = AmigoService.this;
                    if (!isMainProcessRunning(context)) {
                        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(launchIntent);
                        Log.e(TAG, "start launchIntent");
                        stopSelf();
                        System.exit(0);
                        Process.killProcess(Process.myPid());
                        return;
                    }
                    sendEmptyMessageDelayed(WHAT, DELAY);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.sendEmptyMessage(WHAT);
        return super.onStartCommand(intent, flags, startId);
    }
}
