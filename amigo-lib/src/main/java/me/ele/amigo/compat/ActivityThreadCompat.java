package me.ele.amigo.compat;

import java.lang.reflect.InvocationTargetException;

import me.ele.amigo.reflect.MethodUtils;

public class ActivityThreadCompat {

    private static Object sActivityThread;

    private static Class sClass;

    public synchronized static final Object instance() throws ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (sActivityThread == null) {
            sActivityThread = MethodUtils.invokeStaticMethod(clazz(), "currentActivityThread");
        }
        return sActivityThread;
    }

    public static final Class clazz() throws ClassNotFoundException {
        if (sClass == null) {
            sClass = Class.forName("android.app.ActivityThread");
        }
        return sClass;
    }
}
