package me.ele.amigo.release;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dalvik.system.DexFile;
import me.ele.amigo.AmigoDirs;
import me.ele.amigo.AmigoService;
import me.ele.amigo.PatchApks;
import me.ele.amigo.PatchInfoUtil;
import me.ele.amigo.compat.NativeLibraryHelperCompat;
import me.ele.amigo.utils.DexReleaser;

import static me.ele.amigo.utils.CrcUtils.getCrc;

public class ApkReleaser {
    private static final int MSG_ID_DEX_OPT_DONE = 1;
    private static final String TAG = ApkReleaser.class.getSimpleName();
    private static final String DEX_SUFFIX = ".dex";
    private static boolean isReleasing = false;
    private Context context;
    private ExecutorService service;
    private AmigoDirs amigoDirs;
    private PatchApks patchApks;
    private Handler handler;
    private Handler msgHandler;

    public ApkReleaser(final Context appContext) {
        this.context = appContext;
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return handleMsg(msg, context);
            }
        });
        this.service = Executors.newFixedThreadPool(3);
        this.amigoDirs = AmigoDirs.getInstance(context);
        this.patchApks = PatchApks.getInstance(context);
    }

    private boolean handleMsg(Message msg, Context context) {
        switch (msg.what) {
            case MSG_ID_DEX_OPT_DONE:
                isReleasing = false;
                String checksum = (String) msg.obj;
                saveDexAndSoChecksum(checksum);
                PatchInfoUtil.updateDexFileOptStatus(context, checksum, true);
                PatchInfoUtil.setWorkingChecksum(context, checksum);
                if (msgHandler != null)
                    msgHandler.sendEmptyMessage(AmigoService.MSG_ID_DEX_OPT_FINISHED);
                return true;
            default:
                break;
        }

        return false;
    }

    public void release(final String checksum, final Handler msgHandler) {
        Log.e(TAG, "release doing--->" + isReleasing + ", checksum: " + checksum);
        if (isReleasing) {
            return;
        }
        this.msgHandler = msgHandler;
        service.submit(new Runnable() {
            @Override
            public void run() {
                isReleasing = true;
                DexReleaser.releaseDexes(patchApks.patchFile(checksum), amigoDirs.dexDir(checksum));
                NativeLibraryHelperCompat.copyNativeBinaries(patchApks.patchFile(checksum),
                        amigoDirs.libDir(checksum));
                dexOptimization(checksum);
            }
        });
    }

    private void dexOptimization(final String checksum) {
        Log.e(TAG, "dexOptimization");
        File[] validDexes = amigoDirs.dexDir(checksum).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    return pathname.getName().endsWith(".apk");
                } else {
                    return pathname.getName().endsWith(".dex");
                }
            }
        });

        final CountDownLatch countDownLatch = new CountDownLatch(validDexes.length);

        for (final File dex : validDexes) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    long startTime = System.currentTimeMillis();
                    String optimizedPath = optimizedPathFor(dex, amigoDirs.dexOptDir(checksum));
                    DexFile dexFile = null;
                    try {
                        dexFile = DexFile.loadDex(dex.getPath(), optimizedPath, 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (dexFile != null) {
                            try {
                                dexFile.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    Log.e(TAG, String.format("dex %s consume %d ms", dex.getAbsolutePath(),
                            System.currentTimeMillis() - startTime));
                    countDownLatch.countDown();
                }
            });
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "dex opt done");
        handler.sendMessage(handler.obtainMessage(MSG_ID_DEX_OPT_DONE, checksum));
    }

    private String optimizedPathFor(File path, File optimizedDirectory) {
        String fileName = path.getName();
        if (!fileName.endsWith(DEX_SUFFIX)) {
            int lastDot = fileName.lastIndexOf(".");
            if (lastDot < 0) {
                fileName += DEX_SUFFIX;
            } else {
                StringBuilder sb = new StringBuilder(lastDot + 4);
                sb.append(fileName, 0, lastDot);
                sb.append(DEX_SUFFIX);
                fileName = sb.toString();
            }
        }
        File result = new File(optimizedDirectory, fileName);
        return result.getPath();
    }

    private void saveDexAndSoChecksum(String apkChecksum) {
        HashMap<String, String> checksumMap = new HashMap<>();
        File[] dexFiles = amigoDirs.dexDir(apkChecksum).listFiles();
        for (File dexFile : dexFiles) {
            String checksum = getCrc(dexFile);
            checksumMap.put(dexFile.getAbsolutePath(), checksum);
        }

        File[] dexOptFiles = amigoDirs.dexOptDir(apkChecksum).listFiles();
        for (File dexOptFile : dexOptFiles) {
            String checksum = getCrc(dexOptFile);
            checksumMap.put(dexOptFile.getAbsolutePath(), checksum);
        }

        File[] nativeFiles = amigoDirs.libDir(apkChecksum).listFiles();
        if (nativeFiles != null && nativeFiles.length > 0) {
            for (File nativeFile : nativeFiles) {
                String checksum = getCrc(nativeFile);
                checksumMap.put(nativeFile.getAbsolutePath(), checksum);
            }
        }
        PatchInfoUtil.updatePatchFileChecksum(context, apkChecksum, checksumMap);
    }

}
