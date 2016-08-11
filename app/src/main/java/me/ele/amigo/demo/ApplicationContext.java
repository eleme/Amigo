package me.ele.amigo.demo;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.support.multidex.MultiDex;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.ele.amigo.Amigo;

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
//        copyAsset("demo.apk");
//        Amigo.work(this);
    }

    private void copyAsset(String assetName) {
        InputStream in = null;
        OutputStream out = null;
        try {
            File outFile = Amigo.getHotfixApk(this);
            if (!outFile.exists()) {
                AssetManager assetManager = getAssets();
                in = assetManager.open(assetName);
                out = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


}
