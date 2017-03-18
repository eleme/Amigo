package me.ele.amigo;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.util.Log;
import android.util.SparseArray;
import java.util.Map;
import me.ele.amigo.release.ApkReleaser;
import me.ele.amigo.utils.ProcessUtils;

import static me.ele.amigo.compat.ActivityThreadCompat.instance;
import static me.ele.amigo.reflect.FieldUtils.readField;

public class AmigoService extends Service {

    public static final int MSG_ID_PULL_UP_MAIN_PROCESS = 0;
    public static final int MSG_ID_DEX_OPT_SUCCESS = 1;
    public static final int MSG_ID_DEX_OPT_FAILURE = 2;
    private static final String TAG = "AmigoService";
    private static final String ACTION_RELEASE_DEX = "release_dex";
    private static final String ACTION_RESTART_MANI_PROCESS = "restart_main_process";
    private static final String EXTRA_APK_CHECKSUM = "apk_checksum";
    private static final String REMOTE_PROCESS_REQUEST_ID = "remote_process_request_id";
    private static final String REMOTE_PROCESS_MSG_RECEIVER = "remote_process_msg_receiver";

    private ApkReleaser apkReleaser;

    // used to receive messages sent from a remote process and the dex-opt background thread
    private Handler msgHandler = null;

    private SparseArray<Messenger> boundedClients = new SparseArray<>();

    // used to expose the binder inside it to the remote process who bound to this service
    private Messenger remoteProcessMsgReceiver;

    public static void restartMainProcess(Context context) {
        context.startService(new Intent(context, AmigoService.class)
                .setAction(ACTION_RESTART_MANI_PROCESS));
        try {
            @SuppressWarnings("unchecked")
            Map<Object, Object> mActivities = (Map<Object, Object>) readField(instance(),
                    "mActivities");
            for (Map.Entry entry : mActivities.entrySet()) {
                Activity activity = (Activity) readField(entry.getValue(), "activity");
                activity.finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Process.killProcess(Process.myPid());
    }

    public static void startReleaseDex(final Context context, String checksum,
            final Amigo.WorkLaterCallback callback) {
        final ServiceConnection[] serviceConnections = new ServiceConnection[1];
        final int requestId = 112233;
        final Messenger messenger = new Messenger(new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.d(TAG, "handleMessage: get msg from amigo service process : " + msg);
                if (msg.what == requestId) {
                    if (serviceConnections[0] != null) {
                        Log.d(TAG, "handleMessage: unbind connection");
                        try {
                            context.unbindService(serviceConnections[0]);
                        } catch (Exception e) {
                            Log.e(TAG, "handleMessage: failed to unbind amigo service", e);
                        }
                    }
                    if (callback != null) {
                        callback.onPatchApkReleased(msg.arg1 == 1);
                    }
                    return true;
                }
                return false;
            }
        }));
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onServiceConnected: ");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected: ");
            }
        };
        serviceConnections[0] = connection;
        context.bindService(new Intent(context, AmigoService.class)
                .putExtra(REMOTE_PROCESS_MSG_RECEIVER, messenger)
                .putExtra(REMOTE_PROCESS_REQUEST_ID, requestId)
                .putExtra(EXTRA_APK_CHECKSUM, checksum)
                .setAction(ACTION_RELEASE_DEX), connection, Context.BIND_AUTO_CREATE);
    }

    public static void startReleaseDex(Context context, String checksum) {
        context.startService(new Intent(context, AmigoService.class)
                .putExtra(EXTRA_APK_CHECKSUM, checksum)
                .setAction(ACTION_RELEASE_DEX));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        msgHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return handleMsg(msg);
            }
        });
        remoteProcessMsgReceiver = new Messenger(msgHandler);
    }

    private boolean handleMsg(Message msg) {
        int success = 1;
        switch (msg.what) {
            case MSG_ID_PULL_UP_MAIN_PROCESS:
                pullUpMainProcess(this);
                return true;
            case MSG_ID_DEX_OPT_FAILURE:
                success = 0;
            case MSG_ID_DEX_OPT_SUCCESS:
                notifyRemoteProcessDexOptTaskFinished(success);
                return true;
            default:
                Log.w(TAG, "handleMsg: unknown msg id: " + msg.what);
                break;
        }
        return false;
    }

    private void notifyRemoteProcessDexOptTaskFinished(int success) {
        for (int i = 0; i < boundedClients.size(); i++) {
            int requestId = boundedClients.keyAt(i);
            Messenger msgSender = boundedClients.valueAt(i);
            try {
                Message dexDoneMsg = Message.obtain();
                dexDoneMsg.what = requestId;
                dexDoneMsg.arg1 = success;
                msgSender.send(dexDoneMsg);
                Log.d(TAG, "handleMsg: notify the remote process["
                        + requestId
                        + "] that dex-opt task was finished succeed");
            } catch (Exception e) {
                Log.e(TAG, "handleMsg: notify the remote process["
                        + requestId
                        + "] that dex-opt task was finished failed", e);
            }
        }
        boundedClients.clear();
        msgHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "stop amigo service and shutdown the process...");
                stopSelf();
                Process.killProcess(Process.myPid());
            }
        }, 1000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: " + intent);
        onHandleIntent(intent);
        return remoteProcessMsgReceiver.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        boundedClients.clear();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onHandleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        Messenger msgReceiver = intent.getParcelableExtra(REMOTE_PROCESS_MSG_RECEIVER);
        int requestId = intent.getIntExtra(REMOTE_PROCESS_REQUEST_ID, Integer.MIN_VALUE);
        if (msgReceiver != null && requestId != Integer.MIN_VALUE) {
            boundedClients.put(requestId, msgReceiver);
        }

        Log.d(TAG, "onHandleIntent: " + intent);
        if (ACTION_RELEASE_DEX.equals(intent.getAction())) {
            handleReleaseDex(intent);
        } else if (ACTION_RESTART_MANI_PROCESS.equals(intent.getAction())) {
            msgHandler.sendMessage(msgHandler.obtainMessage(MSG_ID_PULL_UP_MAIN_PROCESS));
        }
    }

    private synchronized void handleReleaseDex(Intent intent) {
        String checksum = intent.getStringExtra(EXTRA_APK_CHECKSUM);
        if (apkReleaser == null) {
            apkReleaser = new ApkReleaser(getApplicationContext());
        }
        apkReleaser.release(checksum, msgHandler);
    }

    private void pullUpMainProcess(Context context) {
        if (ProcessUtils.isMainProcessRunning(context)) {
            return;
        }
        ProcessUtils.startLauncherIntent(context);
    }
}
