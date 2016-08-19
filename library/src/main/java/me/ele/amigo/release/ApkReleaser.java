package me.ele.amigo.release;

import android.content.Context;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import me.ele.amigo.Amigo;
import me.ele.amigo.utils.MD5;
import me.ele.amigo.utils.ProcessUtils;

public class ApkReleaser {

    private static final String SP_NAME = "apk_releaser";
    private static final int SLEEP_DURATION = 200;

    private ApkReleaser() {
    }

    public static void doneDexOpt(Context context) {
        context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS)
                .edit()
                .putBoolean(getUniqueKey(context), true)
                .apply();
    }

    private static boolean isDexOptDone(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS)
                .getBoolean(getUniqueKey(context),
                        false);
    }

    private static String getUniqueKey(Context context) {

        try {
            return MD5.checksum(Amigo.getHotfixApk(context));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static void work(Context context, int layoutId, int themeId) {
        if (!ProcessUtils.isLoadDexProcess(context)) {
            if (!isDexOptDone(context)) {
                waitDexOptDone(context, layoutId, themeId);
            }
        }
    }

    private static void waitDexOptDone(Context context, int layoutId, int themeId) {

        new Launcher(context, layoutId).themeId(themeId).launch();

        while (!isDexOptDone(context)) {
            try {
                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
