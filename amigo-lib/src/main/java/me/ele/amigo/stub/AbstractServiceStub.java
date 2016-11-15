package me.ele.amigo.stub;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import me.ele.amigo.hook.ServiceManager;
import me.ele.amigo.utils.Log;

public abstract class AbstractServiceStub extends Service {
    private static final String TAG = "AbstractServiceStub";

    private static ServiceManager mCreator = ServiceManager.getDefault();

    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        isRunning = true;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        try {
            mCreator.onDestroy();
        } catch (Exception e) {
            handleException(e);
        }
        super.onDestroy();
        isRunning = false;
        try {
            synchronized (sLock) {
                sLock.notifyAll();
            }
        } catch (Exception e) {
        }
    }

    public static void startKillService(Context context, Intent service) {
        service.putExtra("ActionKillSelf", true);
        context.startService(service);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "onStart");
        if (intent == null) {
            super.onStart(intent, startId);
            return;
        }

        if (intent.getBooleanExtra("ActionKillSelf", false)) {
            startKillSelf();
            if (!ServiceManager.getDefault().hasServiceRunning()) {
                stopSelf(startId);
                boolean stopService = getApplication().stopService(intent);
                Log.i(TAG, "doGc Kill Process(pid=%s,uid=%s has exit) for %s onStart=%s " +
                        "intent=%s", android.os.Process.myPid(), android.os.Process.myUid
                        (), getClass().getSimpleName(), stopService, intent);
            } else {
                Log.i(TAG, "doGc Kill Process(pid=%s,uid=%s has exit) for %s onStart " +
                                "intent=%s skip,has service running", android.os.Process
                                .myPid(),
                        android.os.Process.myUid(), getClass().getSimpleName(), intent);
            }
        } else {
            try {
                mCreator.onStart(this, intent, 0, startId);
            } catch (Throwable e) {
                handleException(e);
            }
        }
        super.onStart(intent, startId);
    }

    private final Object sLock = new Object();

    private void startKillSelf() {
        if (!isRunning) {
            return;
        }

        try {
            new Thread() {
                @Override
                public void run() {
                    synchronized (sLock) {
                        try {
                            sLock.wait();
                        } catch (Exception e) {
                        }
                    }
                    Log.i(TAG, "doGc Kill Process(pid=%s,uid=%s has exit) for %s 2", android
                            .os.Process.myPid(), android.os.Process.myUid(), getClass()
                            .getSimpleName());
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleException(Throwable e) {
        Log.e(TAG, "handleException: ", e);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "onTaskRemoved");
        try {
            if (rootIntent != null) {
                mCreator.onTaskRemoved(this, rootIntent);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        try {
            if (intent != null) {
                return mCreator.onBind(this, intent);
            }
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "onRebind");
        try {
            if (intent != null) {
                mCreator.onRebind(this, intent);
            }
        } catch (Exception e) {
            handleException(e);
        }
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        try {
            if (intent != null) {
                return mCreator.onUnbind(this, intent);
            }
        } catch (Exception e) {
            handleException(e);
        }
        return false;
    }

}
