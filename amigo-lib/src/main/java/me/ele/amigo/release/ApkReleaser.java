package me.ele.amigo.release;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import dalvik.system.DexFile;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import me.ele.amigo.AmigoDirs;
import me.ele.amigo.AmigoService;
import me.ele.amigo.PatchApks;
import me.ele.amigo.PatchInfoUtil;
import me.ele.amigo.compat.NativeLibraryHelperCompat;
import me.ele.amigo.utils.DexExtractor;

import static me.ele.amigo.utils.CrcUtils.getCrc;

public class ApkReleaser {
    private static final String TAG = ApkReleaser.class.getSimpleName();
    private static final String DEX_SUFFIX = ".dex";

    private volatile boolean isReleasing = false;
    private Context context;
    private ExecutorService service;
    private AmigoDirs amigoDirs;
    private PatchApks patchApks;

    public ApkReleaser(final Context appContext) {
        this.context = appContext;
        int processorCount = Runtime.getRuntime().availableProcessors();
        final int dexCountInApkCommonly = 3;
        this.service =
                Executors.newFixedThreadPool(Math.min(dexCountInApkCommonly, processorCount));
    }

    private void handleDexOptSuccess(String checksum, Handler msgHandler) {
        saveDexAndSoChecksum(checksum);
        PatchInfoUtil.updateDexFileOptStatus(context, checksum, true);
        PatchInfoUtil.setWorkingChecksum(context, checksum);
        if (msgHandler != null) {
            msgHandler.sendEmptyMessage(AmigoService.MSG_ID_DEX_OPT_SUCCESS);
        }
    }

    private void handleDexOptFailure(String checksum, Handler msgHandler) {
        PatchInfoUtil.setWorkingChecksum(context, "");
        PatchInfoUtil.updateDexFileOptStatus(context, checksum, true);
        if (msgHandler != null) {
            msgHandler.sendEmptyMessage(AmigoService.MSG_ID_DEX_OPT_FAILURE);
        }
    }

    public void release(final String checksum, final Handler msgHandler) {
        if (isReleasing) {
            Log.w(TAG, "release : been busy now, skip release " + checksum);
            return;
        }

        Log.d(TAG, "release: start release " + checksum);
        try {
            this.amigoDirs = AmigoDirs.getInstance(context);
            this.patchApks = PatchApks.getInstance(context);
        } catch (Exception e) {
            Log.e(TAG,
                    "release: unable to create amigo dir and patch apk dir, abort release dex files",
                    e);
            handleDexOptFailure(checksum, msgHandler);
            return;
        }
        isReleasing = true;
        service.submit(new Runnable() {
            @Override
            public void run() {
                if (!new DexExtractor(context, checksum).extractDexFiles()) {
                    Log.e(TAG, "releasing dex failed");
                    handleDexOptFailure(checksum, msgHandler);
                    isReleasing = false;
                    return;
                }
                int errorCode;
                if ((errorCode =
                        NativeLibraryHelperCompat.copyNativeBinaries(patchApks.patchFile(checksum),
                                amigoDirs.libDir(checksum))) < 0) {
                    Log.e(TAG, "coping native binaries failed, errorCode = " + errorCode);
                    handleDexOptFailure(checksum, msgHandler);
                    isReleasing = false;
                    return;
                }

                if (Build.VERSION.SDK_INT >= 21 ? dexOptimizationOnArt(checksum)
                        : dexOptimizationOnDalvik(checksum)) {
                    Log.e(TAG, "optimize dex succeed");
                    handleDexOptSuccess(checksum, msgHandler);
                    isReleasing = false;
                    return;
                }

                Log.e(TAG, "optimize dex failed");
                handleDexOptFailure(checksum, msgHandler);
                isReleasing = false;
            }
        });
    }

    private boolean dexOptimizationOnArt(final String checksum) {
        Log.e(TAG, "dexOptimizationOnArt");
        String apk = patchApks.patchPath(checksum);
        String optimizedPath = optimizedPathFor(apk, amigoDirs.dexOptDir(checksum));
        DexFile dexFile = null;
        boolean success = true;
        try {
            dexFile = DexFile.loadDex(apk, optimizedPath, 0);
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        } finally {
            if (dexFile != null) {
                try {
                    dexFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return success;
    }

    private boolean dexOptimizationOnDalvik(final String checksum) {
        Log.e(TAG, "dexOptimizationOnDalvik");
        final ArrayList<String> validDexPaths = new ArrayList<String>();
        amigoDirs.dexDir(checksum).listFiles(new FileFilter() {
            @Override public boolean accept(File pathname) {
                if (pathname.getName().endsWith(".zip")) {
                    validDexPaths.add(pathname.getAbsolutePath());
                }
                return false;
            }
        });

        if (validDexPaths.isEmpty()) {
            Log.e(TAG, "dexOptimizationOnDalvik: no dex to optmize");
            return false;
        }

        final CountDownLatch countDownLatch = new CountDownLatch(validDexPaths.size());
        final AtomicBoolean allDexOptimized = new AtomicBoolean(true);
        final AtomicInteger optimizedDexCount = new AtomicInteger(validDexPaths.size());
        for (final String dexPath : validDexPaths) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    if (!allDexOptimized.get()) {
                        Log.e(TAG, "abort optimizing dex["
                                + dexPath
                                + "] because an error occurred before");
                        countDownLatch.countDown();
                        return;
                    }
                    String optimizedPath = optimizedPathFor(dexPath, amigoDirs.dexOptDir(checksum));
                    boolean success = doOptimizeDex(dexPath, optimizedPath);
                    if (!success) {
                        allDexOptimized.compareAndSet(true, false);
                    } else {
                        optimizedDexCount.decrementAndGet();
                    }
                    countDownLatch.countDown();
                }
            });
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "dex opt job finished");
        return allDexOptimized.get() && optimizedDexCount.get() == 0;
    }

    private boolean doOptimizeDex(String dexPath, String odexPath) {
        long startTime = System.currentTimeMillis();
        DexFile dexFile = null;
        IOException exception = null;
        try {
            dexFile = DexFile.loadDex(dexPath, odexPath, 0);
        } catch (IOException e) {
            exception = e;
        } finally {
            if (dexFile != null) {
                try {
                    dexFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        boolean success = exception == null;
        Log.e(TAG, String.format("optimizing dex %s %s, consumed %d mills",
                dexPath, (success ? "succeed" : "failed"),
                System.currentTimeMillis() - startTime), exception);
        return success;
    }

    private String optimizedPathFor(String path, File optimizedDirectory) {
        File dexFile = new File(path);
        String fileName = dexFile.getName();
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