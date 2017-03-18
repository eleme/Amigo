package me.ele.amigo.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import me.ele.amigo.AmigoDirs;
import me.ele.amigo.PatchApks;

public class DexExtractor {

    private static final String TAG = "DexExtractor";
    private Context context;
    private String checksum;
    private byte[] buffer;

    public DexExtractor(Context context, String checkSum) {
        this.context = context;
        this.checksum = checkSum;
    }

    private static boolean verifyZipFile(File file) {
        try {
            ZipFile ex = new ZipFile(file);
            try {
                ex.close();
                return true;
            } catch (IOException var3) {
                Log.w(TAG, "Failed to close zip file: " + file.getAbsolutePath());
            }
        } catch (ZipException var4) {
            Log.w(TAG, "File " + file.getAbsolutePath() + " is not a valid zip file.",
                    var4);
        } catch (IOException var5) {
            Log.w(TAG,
                    "Got an IOException trying to open zip file: " + file.getAbsolutePath(), var5);
        }

        return false;
    }

    private static boolean equals(InputStream newDex, InputStream oldDex) {
        try {
            byte[] hash1 = DigestUtils.md5(newDex);
            byte[] hash2 = DigestUtils.md5(oldDex);
            return Arrays.equals(hash1, hash2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void closeSilently(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void closeSilently(ZipFile zipFile) {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean extractDexFiles() {
        if (Build.VERSION.SDK_INT >= 21) {
            return true; // art supports multi-dex natively
        }

        return performExtractions(PatchApks.getInstance(context).patchFile(checksum),
                AmigoDirs.getInstance(context).dexDir(checksum));
    }

    private boolean performExtractions(File patchApk, File dexDir) {
        ZipFile apk = null;
        try {
            apk = new ZipFile(patchApk);
            int dexNum = 0;
            ZipEntry dexFile = apk.getEntry("classes.dex");
            for (; dexFile != null; dexFile = apk.getEntry("classes" + dexNum + ".dex")) {
                String fileName = dexFile.getName().replace("dex", "zip");
                File extractedFile = new File(dexDir, fileName);
                extract(apk, dexFile, extractedFile);
                verifyZipFile(extractedFile);
                if (dexNum == 0) ++dexNum;
                ++dexNum;
            }
            return dexNum > 0;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        } finally {
            try {
                apk.close();
            } catch (IOException var16) {
                Log.w("DexExtractor", "Failed to close resource", var16);
            }
        }
    }

    private String findMatchedSecondaryDex(final String dexName) {
        File secondaryDexDir =
                new File(context.getApplicationInfo().dataDir, "code_cache/secondary-dexes");
        File[] secondaryDexes = secondaryDexDir.listFiles(new FileFilter() {
            @Override public boolean accept(File pathname) {
                return pathname.getName().endsWith(dexName);
            }
        });
        return secondaryDexes != null && secondaryDexes.length == 1
                ? secondaryDexes[0].getAbsolutePath() : null;
    }

    private void extract(ZipFile patchApk, ZipEntry dexFile, File extractTo) throws IOException {
        boolean reused = reusePreExistedODex(patchApk, dexFile);
        Log.d(TAG, "extracted: "
                + dexFile.getName() + " success ? "
                + reused
                + ", by reusing pre-existed secondary dex");

        if (reused) {
            return;
        }

        InputStream in = null;
        File tmp = null;
        ZipOutputStream out = null;
        try {
            in = patchApk.getInputStream(dexFile);
            tmp = File.createTempFile(extractTo.getName(), ".tmp", extractTo.getParentFile());
            try {
                out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
                ZipEntry classesDex = new ZipEntry("classes.dex");
                classesDex.setTime(dexFile.getTime());
                out.putNextEntry(classesDex);
                if (buffer == null) {
                    buffer = new byte[16384];
                }
                for (int length = in.read(buffer); length != -1; length = in.read(buffer)) {
                    out.write(buffer, 0, length);
                }
            } finally {
                if (out != null) {
                    out.closeEntry();
                    out.close();
                }
            }

            if (!tmp.renameTo(extractTo)) {
                throw new IOException("Failed to rename \""
                        + tmp.getAbsolutePath()
                        + "\" to \""
                        + extractTo.getAbsolutePath()
                        + "\"");
            }
        } finally {
            closeSilently(in);
            if (tmp != null) tmp.delete();
        }
    }

    private boolean reusePreExistedODex(ZipFile apk, ZipEntry dexFile) {
        String preExistedDexPath;
        File preExistedOdex;
        boolean isMainDex = dexFile.getName().equals("classes.dex");
        if (isMainDex) {
            File baseApk = new File(context.getApplicationInfo().sourceDir);
            preExistedDexPath = baseApk.getAbsolutePath();
            preExistedOdex =
                    new File("/data/dalvik-cache/data@app@" + baseApk.getName() + "@classes.dex");
        } else {
            preExistedDexPath = findMatchedSecondaryDex(dexFile.getName().replace("dex", "zip"));
            if (preExistedDexPath == null) {
                return false;
            }
            preExistedOdex = new File(preExistedDexPath.replace("zip", "dex"));
        }

        if (!preExistedOdex.exists() || !preExistedOdex.canRead()) {
            return false;
        }

        boolean canReuse;
        ZipFile baseApk = null;
        InputStream baseDexInputStream = null;
        try {
            InputStream patchDexInputStream = apk.getInputStream(dexFile);
            baseApk = new ZipFile(preExistedDexPath);
            baseDexInputStream = baseApk.getInputStream(new ZipEntry("classes.dex"));
            canReuse = equals(patchDexInputStream, baseDexInputStream);
        } catch (IOException e) {
            e.printStackTrace();
            canReuse = false;
        } finally {
            closeSilently(baseDexInputStream);
            closeSilently(baseApk);
            closeSilently(baseDexInputStream);
        }

        if (!canReuse) {
            return false;
        }

        File extractTo = new File(AmigoDirs.getInstance(context).dexDir(checksum),
                dexFile.getName().replace("dex", "zip"));
        File extractToOdex = new File(AmigoDirs.getInstance(context).dexOptDir(checksum),
                dexFile.getName());
        if (isMainDex) {
            try {
                // make a link directing to the base apk instead of the patch apk here,
                // because dalvik uses the modification time of the dex entry to
                // verify the odex file
                SymbolicLinkUtil.createLink(new File(context.getApplicationInfo().sourceDir),
                        extractTo);
            } catch (IOException e) {
                Log.e(TAG, "reusePreExistedODex: create first dex symlink failed", e);
                return false;
            }
        } else {
            // make a existed secondary dex copy in case of system update, we don't want
            // the dex file to be deleted like the odex file under that
            if (!FileUtils.copyFile(new File(preExistedDexPath), extractTo)) {
                return false;
            }
        }
        try {
            // try to create a symlink to save space
            SymbolicLinkUtil.createLink(preExistedOdex, extractToOdex);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "reusePreExistedODex: create odex symlink failed", e);
        }
        // fall back to make a copy of the odex file
        return FileUtils.copyFile(preExistedOdex, extractToOdex);
    }
}
