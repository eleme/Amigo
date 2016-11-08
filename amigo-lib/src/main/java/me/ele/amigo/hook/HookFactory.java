package me.ele.amigo.hook;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import me.ele.amigo.utils.Log;

public class HookFactory {

    private static final String TAG = HookFactory.class.getSimpleName();

    private static final List<Hook> mHookList = new ArrayList<>(2);

    public static void install(Context context, ClassLoader cl) {
        installHook(new IActivityManagerHook(context), cl);
        installHook(new IPackageManagerHook(context), cl);
    }

    private static void installHook(Hook hook, ClassLoader cl) {
        synchronized (mHookList) {
            try {
                hook.onInstall(cl);
                mHookList.add(hook);
            } catch (Throwable throwable) {
                Log.e(TAG, "installHook %s error", throwable, hook);
            }
        }
    }

    public static void uninstallAllHooks(ClassLoader cl) {
        if (cl == null) {
            Log.e(TAG, "uninstallAllHooks: null classloader");
            return;
        }
        synchronized (mHookList) {
            for (Hook hook : mHookList) {
                try {
                    hook.onUnInstall(cl);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
            mHookList.clear();
        }
    }

}
