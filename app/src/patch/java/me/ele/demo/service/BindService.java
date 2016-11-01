package me.ele.demo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Random;

public class BindService extends Service {

    public static final String TAG = BindService.class.getSimpleName();

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    // Random number generator
    private final Random mGenerator = new Random();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public BindService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BindService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
        Toast.makeText(BindService.this, "bind service success with data" + intent.getStringExtra
                (TAG), Toast.LENGTH_SHORT).show();
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.e(TAG, "onBind");
        Toast.makeText(BindService.this, "rebind service success", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind");
        Toast.makeText(BindService.this, "unbind service success", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    /**
     * method for clients
     */
    public int getRandomNumber() {
        return mGenerator.nextInt(100);
    }
}
