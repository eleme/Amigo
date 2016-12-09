package me.ele.amigo.utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class ProcessUtils {

    private static final String TAG = ProcessUtils.class.getSimpleName();

    private ProcessUtils() {

    }

    public static boolean isMainProcess(Context context) {
        return context.getPackageName().equals(getCurrentProcessName(context));
    }

    private static String getCurrentProcessName(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> processList = activityManager.getRunningAppProcesses();
        if (processList != null && processList.size() > 0) {
            for (RunningAppProcessInfo processInfo : processList) {
                if (processInfo.pid == android.os.Process.myPid()) {
                    return processInfo.processName;
                }
            }
        }
        return null;
    }

    public static boolean isMainProcessRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService
                (ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        for (int i = 0; i < processes.size(); i++) {
            if (processes.get(i).processName.equals(context.getPackageName())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isLoadDexProcess(Context context) {
        String currentProcessName = getCurrentProcessName(context);
        String loadDexProcessName = context.getPackageName() + ":amigo";
        return currentProcessName != null && currentProcessName.equals(loadDexProcessName);
    }

    public static void startLauncherIntent(Context context) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context
                .getPackageName());
        launchIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchIntent);
    }

}
