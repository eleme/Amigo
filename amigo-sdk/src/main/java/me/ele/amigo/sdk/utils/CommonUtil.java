package me.ele.amigo.sdk.utils;


public class CommonUtil {

    public static String byteArray2String(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
