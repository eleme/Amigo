package me.ele.amigo.utils;

import android.os.Build;
import android.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    public static boolean copyFile(File sourceFile, File dstFile) {
        FileOutputStream outputStream = null;
        FileInputStream inputStream = null;
        try {
            if (!dstFile.exists()) {
                dstFile.getParentFile().mkdirs();
            }
            outputStream = new FileOutputStream(dstFile);
            inputStream = new FileInputStream(sourceFile);
            copyFile(inputStream, outputStream);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static void removeFile(File file) {
        removeFile(file, true);
    }

    public static void removeFile(File file, File excludeSubFile) {
        if (file == null || !file.exists() || file.equals(excludeSubFile)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            file.setWritable(true);
        }

        if (file.isFile()) {
            file.delete();
            return;
        }

        if (!file.isDirectory()) {
            return;
        }

        File[] listFiles = file.listFiles();
        if (listFiles != null && listFiles.length > 0) {
            for (File f : listFiles) {
                removeFile(f, excludeSubFile);
            }
        }
        file.delete();
    }

    public static void removeFile(File file, boolean isDirRemovable) {
        if (file == null || !file.exists()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            file.setWritable(true);
        }

        if (file.isFile()) {
            file.delete();
            return;
        }

        if (file.isDirectory()) {

            File[] listFiles = file.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                for (File f : listFiles) {
                    removeFile(f);
                }
            }

            if (isDirRemovable) {
                file.delete();
            }
        }
    }

    public static void mkdirChecked(File dir) throws IOException {
        dir.mkdir();
        if (!dir.isDirectory()) {
            throw new IOException("Failed to create directory " + dir.getPath());
        }
    }
}