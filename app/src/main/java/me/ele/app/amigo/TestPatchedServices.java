package me.ele.app.amigo;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ele.amigo.reflect.MethodUtils;

/**
 * Created by wwm on 9/30/16.
 */
public class TestPatchedServices extends BaseActivity {

    private static final String TAG = "TestPatchedServices";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_patched_services);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.start_new_service)
    public void startNewService() {
        try {
            startService(new Intent().setClassName(this, "me.ele.demo.service.StartService")
                    .putExtra("StartService", "1"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.stop_new_service)
    public void stopNewService() {
        try {
            stopService(new Intent().setClassName(this, "me.ele.demo.service.StartService"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG, "onServiceConnected");
            try {
                Class LocalBinderClazz = Class.forName("me.ele.demo.service" +
                        ".BindService$LocalBinder");
                Object binder = service;
                Method getServiceMethod = LocalBinderClazz.getDeclaredMethod("getService");
                Object s = getServiceMethod.invoke(binder);
                Log.e(TAG, "random number from service" + MethodUtils.invokeMethod(s,
                        "getRandomNumber"));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceConnected");
        }
    };

    @OnClick(R.id.bind_new_service)
    public void bindNewService() {
        try {
            bindService(new Intent().setClassName(this, "me.ele.demo.service.BindService")
                    .putExtra("BindService", "1"), connection, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
