package me.ele.amigo;

import android.app.IAmigoService;
import android.app.IntentService;
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
import android.os.RemoteException;
import android.util.Log;

import me.ele.amigo.release.ApkReleaser;

import static me.ele.amigo.utils.ProcessUtils.isMainProcessRunning;


public class AmigoService extends Service {

    public static final int MSG_ID_PULL_UP_MAIN_PROCESS = 0;
    public static final int MSG_ID_DEX_OPT_FINISHED = 1;
    public static final int DELAY = 200;
    public static final int MAX_RETRY_TIMES = 10;
    private static final String TAG = AmigoService.class.getSimpleName();
    private static final String ACTION_RELEASE_DEX = "release_dex";
    private static final String ACTION_RESTART_MANI_PROCESS = "restart_main_process";
    private static final String EXTRA_APK_CHECKSUM = "apk_checksum";
    private Handler handler = null;
    private int retryCount = 0;

    private Messenger messenger; // don't support multiple client
    private int outMsg;

    private IAmigoService iAmigoService = new IAmigoService.Stub() {
        @Override
        public void join(IBinder token, int msg) throws RemoteException {
            messenger = new Messenger(token);
            outMsg = msg;
        }
    };
    private ApkReleaser apkRelease;

    public static void restartMainProcess(Context context) {
        context.startService(new Intent(context, AmigoService.class)
                .setAction(ACTION_RESTART_MANI_PROCESS));
        System.exit(0);
        Process.killProcess(Process.myPid());
    }

    public static void startReleaseDex(final Context context, String checksum, final Amigo
            .WorkLaterCallback callback) {
        final Object[] objects = new Object[1];
        final Messenger messenger = new Messenger(new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 112233) {
                    Log.d(TAG, "handleMessage: get msg from amigo service process : " + msg);
                    if (objects[0] != null) {
                        context.unbindService((ServiceConnection) objects[0]);
                        Log.d(TAG, "handleMessage: unbind connection");
                    }
                    if (callback != null) {
                        callback.onPatchApkReleased();
                    }
                    return true;
                }
                return false;
            }
        }));
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                IAmigoService amigoService = IAmigoService.Stub.asInterface(service);
                try {
                    amigoService.join(messenger.getBinder(), 112233);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        objects[0] = connection;
        context.bindService(new Intent(context, AmigoService.class)
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
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return handleMsg(msg);
            }
        });
    }

    private boolean handleMsg(Message msg) {
        switch (msg.what) {
            case MSG_ID_PULL_UP_MAIN_PROCESS:
                Context context = AmigoService.this;
                if (!isMainProcessRunning(context)) {
                    pullUpMainProcess(context);
                    return true;
                }

                if (retryCount++ < MAX_RETRY_TIMES) {
                    handler.sendMessageDelayed(Message.obtain(msg), DELAY);
                }
                return true;
            case MSG_ID_DEX_OPT_FINISHED:
                Log.d(TAG, "handleMsg: send message out to " + messenger);
                if (messenger != null) {
                    try {
                        Message dexDoneMsg = Message.obtain();
                        dexDoneMsg.what = outMsg;
                        messenger.send(dexDoneMsg);
                        messenger = null;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopSelf();
                        System.exit(0);
                        Process.killProcess(Process.myPid());
                    }
                }, 1000);
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: " + intent);
        onHandleIntent(intent);
        return iAmigoService.asBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
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

        Log.d(TAG, "onHandleIntent: " + intent);
        if (ACTION_RELEASE_DEX.equals(intent.getAction())) {
            handleReleaseDex(intent);
        } else if (ACTION_RESTART_MANI_PROCESS.equals(intent.getAction())) {
            handler.sendMessage(handler.obtainMessage(MSG_ID_PULL_UP_MAIN_PROCESS));
        }
    }

    private synchronized void handleReleaseDex(Intent intent) {
        String checksum = intent.getStringExtra(EXTRA_APK_CHECKSUM);
        if (apkRelease == null) {
            apkRelease = new ApkReleaser(getApplicationContext());
        }
        apkRelease.release(checksum, handler);
    }

    private void pullUpMainProcess(Context context) {
        Intent launchIntent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchIntent);
        Log.d(TAG, "start launchIntent");
        stopSelf();
        System.exit(0);
        Process.killProcess(Process.myPid());
    }
}
