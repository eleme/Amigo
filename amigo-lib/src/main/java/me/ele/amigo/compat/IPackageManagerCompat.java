package me.ele.amigo.compat;

import me.ele.amigo.reflect.MethodUtils;

public class IPackageManagerCompat {


    private static Class sClass;

    public static Class Class() throws ClassNotFoundException {
        if (sClass == null) {
            sClass = Class.forName("android.content.pm.IPackageManager");
        }
        return sClass;
    }

    public static Object instance() throws Exception {
        return MethodUtils.invokeStaticMethod(ActivityThreadCompat.clazz(), "getPackageManager");
    }

}
