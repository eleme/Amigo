package me.ele.amigo.release;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dalvik.system.DexFile;
import me.ele.amigo.Amigo;
import me.ele.amigo.AmigoDirs;
import me.ele.amigo.PatchApks;
import me.ele.amigo.compat.NativeLibraryHelperCompat;
import me.ele.amigo.utils.DexReleaser;
import me.ele.amigo.utils.ProcessUtils;

import static me.ele.amigo.Amigo.SP_NAME;
import static me.ele.amigo.utils.CrcUtils.getCrc;

public class ApkReleaser {
    static final int WHAT_DEX_OPT_DONE = 1;
    static final int WHAT_FINISH = 2;
    static final int DELAY_FINISH_TIME = 4000;
    private static final String TAG = ApkReleaser.class.getSimpleName();
    private static final int SLEEP_DURATION = 200;
    private static final String DEX_SUFFIX = ".dex";
    private static boolean isReleasing = false;
    private static ApkReleaser releaser;
    private Context context;
    private ExecutorService service;
    private AmigoDirs amigoDirs;
    private PatchApks patchApks;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT_DEX_OPT_DONE:
                    isReleasing = false;
                    String checksum = (String) msg.obj;
                    doneDexOpt(checksum);
                    saveDexAndSoChecksum(checksum);
                    context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS)
                            .edit()
                            .putString(Amigo.WORKING_PATCH_APK_CHECKSUM, checksum)
                            .commit();
                    handler.sendEmptyMessageDelayed(WHAT_FINISH, DELAY_FINISH_TIME);
                    break;
                case WHAT_FINISH:
                    System.exit(0);
                    Process.killProcess(Process.myPid());
                    break;
                default:
                    break;
            }
        }
    };

    private ApkReleaser(Context context) {
        this.context = context;
        this.service = Executors.newFixedThreadPool(3);
        this.amigoDirs = AmigoDirs.getInstance(context);
        this.patchApks = PatchApks.getInstance(context);
    }

    public static ApkReleaser getInstance(Context context) {
        if (releaser == null) {
            synchronized (ApkReleaser.class) {
                if (releaser == null) {
                    releaser = new ApkReleaser(context.getApplicationContext());
                }
            }
        }
        return releaser;
    }

    public void release(final String checksum) {
        if (isReleasing) {
            return;
        }
        Log.e(TAG, "release doing--->" + isReleasing + ", checksum: " + checksum);
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
                return pathname.getName().endsWith(".dex");
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
        handler.sendMessage(handler.obtainMessage(WHAT_DEX_OPT_DONE, checksum));
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
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS);
        File[] dexFiles = amigoDirs.dexDir(apkChecksum).listFiles();
        for (File dexFile : dexFiles) {
            String checksum = getCrc(dexFile);
            sp.edit().putString(dexFile.getAbsolutePath(), checksum).commit();
        }

        File[] dexOptFiles = amigoDirs.dexOptDir(apkChecksum).listFiles();
        for (File dexOptFile : dexOptFiles) {
            String checksum = getCrc(dexOptFile);
            sp.edit().putString(dexOptFile.getAbsolutePath(), checksum).commit();
        }

        File[] nativeFiles = amigoDirs.libDir(apkChecksum).listFiles();
        if (nativeFiles != null && nativeFiles.length > 0) {
            for (File nativeFile : nativeFiles) {
                String checksum = getCrc(nativeFile);
                sp.edit().putString(nativeFile.getAbsolutePath(), checksum).commit();
            }
        }
    }

    private void doneDexOpt(String checksum) {
        context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS)
                .edit()
                .putBoolean(checksum, true)
                .commit();
    }

    private boolean isDexOptDone(String checksum) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS)
                .getBoolean(checksum, false);
    }

    public void work(String checksum, int layoutId, int themeId) {
        if (!ProcessUtils.isLoadDexProcess(context)) {
            if (!isDexOptDone(checksum)) {
                waitDexOptDone(checksum, layoutId, themeId);
            }
        }
    }

    private void waitDexOptDone(String checksum, int layoutId, int themeId) {
        new Launcher(context).checksum(checksum).layoutId(layoutId).themeId(themeId).launch();

        while (!isDexOptDone(checksum)) {
            try {
                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
