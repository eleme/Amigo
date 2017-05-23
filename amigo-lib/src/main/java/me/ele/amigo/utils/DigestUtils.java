package me.ele.amigo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {

    private static final int STREAM_BUFFER_LENGTH = 1024 * 8;

    static MessageDigest getDigest(String algorithm) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(algorithm);
    }

    private static byte[] digest(MessageDigest digest, InputStream data) throws IOException {
        byte[] buffer = new byte[STREAM_BUFFER_LENGTH];
        int read;
        while ((read = data.read(buffer, 0, STREAM_BUFFER_LENGTH)) > -1) {
            digest.update(buffer, 0, read);
        }

        return digest.digest();
    }

    public static byte[] digest(String algorithm, InputStream data)
            throws IOException, NoSuchAlgorithmException {
        return digest(getDigest(algorithm), data);
    }

    public static byte[] md5Digest(InputStream data) throws IOException, NoSuchAlgorithmException {
        return digest("MD5", data);
    }
}
