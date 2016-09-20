package me.ele.amigo.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
