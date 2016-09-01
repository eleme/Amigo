package me.ele.amigo.demo;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.support.multidex.MultiDex;
import android.util.Log;

import java.io.File;

import me.ele.amigo.Amigo;
import me.ele.amigo.utils.ProcessUtils;

public class ApplicationContext extends Application {

    private static final String TAG = ApplicationContext.class.getSimpleName();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.e(TAG, "attachBaseContext: " + base);
        MultiDex.install(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate: " + this);

        if (ProcessUtils.isMainProcess(this)) {
            File fixedApkFile = new File(Environment.getExternalStorageDirectory(), "demo.apk");
            File amigoApkFile = Amigo.getHotfixApk(this);
            if (fixedApkFile.exists() && !amigoApkFile.exists()) {
                Amigo.work(this, fixedApkFile);
//                Amigo.workLater(this, fixedApkFile);
            }
        }
    }


}
