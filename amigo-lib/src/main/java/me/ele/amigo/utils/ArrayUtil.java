package me.ele.amigo.utils;

public class ArrayUtil {

    public static <T> boolean isEmpty(T[] array) {
        return !isNotEmpty(array);
    }

    public static <T> boolean isNotEmpty(T[] array) {
        return array != null && array.length > 0;
    }

}
