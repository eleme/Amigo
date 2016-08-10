package me.ele.amigo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {
    public static Object getField(Object obj, String fieldName) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = getField(obj.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    public static void setField(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = getField(obj.getClass(), fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        Field field = null;
        while (field == null) {
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
            if (clazz == Object.class) {
                break;
            }
        }

        return field;
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Method method = null;
        while (method == null) {
            try {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
                break;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }

            if (clazz == Object.class) {
                break;
            }
        }
        return method;
    }

}