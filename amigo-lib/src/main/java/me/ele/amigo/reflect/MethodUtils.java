package me.ele.amigo.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodUtils {

    public static Object invokeMethod(Object object, String methodName, Object[] args, Class<?>[] parameterTypes)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        parameterTypes = Utils.nullToEmpty(parameterTypes);
        args = Utils.nullToEmpty(args);
        Method method = getDeclaredMethod(object.getClass(), methodName, parameterTypes);
        method.setAccessible(true);
        if (method == null) {
            throw new NoSuchMethodException("No such accessible method: "
                    + methodName + "() on object: "
                    + object.getClass().getName());
        }
        return method.invoke(object, args);
    }

    public static Object invokeStaticMethod(Class clazz, String methodName, Object[] args, Class<?>[] parameterTypes)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        parameterTypes = Utils.nullToEmpty(parameterTypes);
        args = Utils.nullToEmpty(args);
        Method method = getDeclaredMethod(clazz, methodName, parameterTypes);
        method.setAccessible(true);
        if (method == null) {
            throw new NoSuchMethodException("No such accessible method: "
                    + methodName + "() on object: "
                    + clazz.getName());
        }
        return method.invoke(null, args);
    }

    public static Object invokeStaticMethod(Class clazz, String methodName, Object... args)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        args = Utils.nullToEmpty(args);
        Class<?>[] parameterTypes = Utils.toClass(args);
        return invokeStaticMethod(clazz, methodName, args, parameterTypes);
    }

    public static Object invokeMethod(Object object, String methodName, Object... args)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        args = Utils.nullToEmpty(args);
        Class<?>[] parameterTypes = Utils.toClass(args);
        return invokeMethod(object, methodName, args, parameterTypes);
    }


    public static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {

        Method method = null;

        NoSuchMethodException exception = null;

        while (clazz != null) {
            try {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
                exception = null;
                break;
            } catch (NoSuchMethodException e) {
                exception = e;
                clazz = clazz.getSuperclass();
            }
        }

        if (exception != null) {
            throw exception;
        }

        return method;
    }

}
