package me.ele.amigo.utils;

import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DexReleaser {

    public static void releaseDexes(File zipFile, File outputDir) {

        byte[] buffer = new byte[8 * 1024];

        try {
            FileUtils.mkdirChecked(outputDir);

            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                String fileName = ze.getName();
                if (!fileName.startsWith("classes") || !fileName.endsWith(".dex")) {
                    ze = zis.getNextEntry();
                    continue;
                }
                File newFile = new File(outputDir + File.separator + fileName);
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    makeDexToApk(newFile);
                    newFile.delete();
                }

                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void makeDexToApk(File srcFile) throws IOException {
        String dexName = srcFile.getName().replace(".dex", ".apk");
        File destZipFile = new File(srcFile.getParentFile(), dexName);
        FileOutputStream fileWriter = new FileOutputStream(destZipFile);
        ZipOutputStream zip = new ZipOutputStream(fileWriter);

        byte[] buf = new byte[1024];
        int len;
        FileInputStream in = new FileInputStream(srcFile);
        zip.putNextEntry(new ZipEntry(srcFile.getName()));
        while ((len = in.read(buf)) > 0) {
            zip.write(buf, 0, len);
        }

        in.close();

        zip.flush();
        zip.close();
    }
}
