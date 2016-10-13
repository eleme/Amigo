package me.ele.demo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class DemoReceiver extends BroadcastReceiver {

    public static final String TAG = "DemoReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive");
        Toast.makeText(context, "action received with data " + intent.getStringExtra(TAG), Toast.LENGTH_SHORT).show();
    }
}
