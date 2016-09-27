package me.ele.amigo.compat;

import me.ele.amigo.reflect.MethodUtils;

public class QueuedWorkCompat {

    private static Class sClass;

    private static Class Class() throws ClassNotFoundException {
        if (sClass == null) {
            sClass = Class.forName("android.app.QueuedWork");
        }
        return sClass;
    }

    public static void waitToFinish() {
        try {
            MethodUtils.invokeStaticMethod(Class(), "waitToFinish");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
