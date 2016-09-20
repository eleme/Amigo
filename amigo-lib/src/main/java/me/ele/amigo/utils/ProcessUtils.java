package me.ele.amigo.utils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;

import java.util.List;

public class ProcessUtils {

    private ProcessUtils() {

    }

    public static boolean isMainProcess(Context context) {
        return context.getPackageName().equals(getCurrentProcessName(context));
    }

    private static String getCurrentProcessName(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
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
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
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

}
