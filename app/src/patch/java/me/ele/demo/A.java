package me.ele.demo;

import me.ele.app.amigo.BuildConfig;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class A {

    public static String getDes() {
        return "Patch, version code: " + BuildConfig.VERSION_CODE;
    }

    public static String loadAsset(Context context) {
        try {
            // load the same entry
//            InputStream is = context.getAssets().open("a.txt");
            // load new entry
            InputStream is = context.getAssets().open("b.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            is.close();
            return line;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Loading Error";
    }
}
