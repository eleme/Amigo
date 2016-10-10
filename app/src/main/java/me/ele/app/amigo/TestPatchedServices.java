package me.ele.app.amigo;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ele.app.amigo.service.BindService;
import me.ele.app.amigo.service.StartService;

/**
 * Created by wwm on 9/30/16.
 */

public class TestPatchedServices extends AppCompatActivity {

    private static final String TAG = "TestPatchedServices";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_patched_services);
        ButterKnife.bind(this);
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
            Toast.makeText(this, "Service not registered", Toast.LENGTH_SHORT).show();
        }
    }

}
