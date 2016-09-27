package me.ele.demo;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import me.ele.app.amigo.BuildConfig;

public class A {

    public static String getDes() {
        return "Host, version code: " + BuildConfig.VERSION_CODE;
    }

    public static String loadAsset(Context context) {
        try {
            InputStream is = context.getAssets().open("a.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            is.close();
            return  line;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Loading Error";
    }
}
