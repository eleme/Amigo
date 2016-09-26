package me.ele.app.amigo.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DemoReceiver extends BroadcastReceiver {

    private static final String TAG = DemoReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive");
    }
}
