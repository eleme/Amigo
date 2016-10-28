package me.ele.app.amigo;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.util.Log;

//import com.facebook.drawee.backends.pipeline.Fresco;

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
        Log.e(TAG, "====onCreate: yeah fixed ====" + this);
//        Fresco.initialize(this);
    }

}
