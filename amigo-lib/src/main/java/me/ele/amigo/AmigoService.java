package me.ele.amigo;

import android.app.Activity;
import android.app.IAmigoService;
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
import android.util.SparseArray;

import java.util.Map;

import me.ele.amigo.release.ApkReleaser;
import me.ele.amigo.utils.ProcessUtils;

import static me.ele.amigo.compat.ActivityThreadCompat.instance;
import static me.ele.amigo.reflect.FieldUtils.readField;


public class AmigoService extends Service {

    public static final int MSG_ID_PULL_UP_MAIN_PROCESS = 0;
    public static final int MSG_ID_DEX_OPT_FINISHED = 1;
    private static final String TAG = AmigoService.class.getSimpleName();
    private static final String ACTION_RELEASE_DEX = "release_dex";
    private static final String ACTION_RESTART_MANI_PROCESS = "restart_main_process";
    private static final String EXTRA_APK_CHECKSUM = "apk_checksum";
    private Handler handler = null;

    private SparseArray<IBinder> clients = new SparseArray<>();

    private IAmigoService iAmigoService = new IAmigoService.Stub() {
        @Override
        public void join(IBinder token, int msg) throws RemoteException {
            clients.put(msg, token);
        }

        @Override
        public void leave(IBinder token, int msg) throws RemoteException {
            if (clients.get(msg) == token) {
                clients.remove(msg);
            }
        }
    };
    private ApkReleaser apkRelease;

    public static void restartMainProcess(Context context) {
        context.startService(new Intent(context, AmigoService.class)
                .setAction(ACTION_RESTART_MANI_PROCESS));
        try {
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
                pullUpMainProcess(this);
                return true;
            case MSG_ID_DEX_OPT_FINISHED:
                for (int i = 0; i < clients.size(); i++) {
                    int code = clients.keyAt(i);
                    // get the object by the key.
                    IBinder client = clients.get(code);
                    Messenger messenger = new Messenger(client);
                    Log.d(TAG, "handleMsg: send message out to client[" + code + "]");
                    try {
                        Message dexDoneMsg = Message.obtain();
                        dexDoneMsg.what = code;
                        messenger.send(dexDoneMsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                clients.clear();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopSelf();
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
        clients.clear();
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
        if (ProcessUtils.isMainProcessRunning(context)) {
            return;
        }
        ProcessUtils.startLauncherIntent(context);
    }
}
