package me.ele.amigo.release;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dalvik.system.DexFile;
import me.ele.amigo.Amigo;
import me.ele.amigo.AmigoDirs;
import me.ele.amigo.PatchApk;
import me.ele.amigo.compat.NativeLibraryHelperCompat;
import me.ele.amigo.utils.DexReleaser;
import me.ele.amigo.utils.ProcessUtils;

import static me.ele.amigo.Amigo.SP_NAME;
import static me.ele.amigo.utils.CrcUtils.getCrc;

public class ApkReleaser {
    private static final String TAG = ApkReleaser.class.getSimpleName();

    private static final int SLEEP_DURATION = 200;

    static final int WHAT_DEX_OPT_DONE = 1;
    static final int WHAT_FINISH = 2;

    static final int DELAY_FINISH_TIME = 4000;

    private Context context;
    private ExecutorService service;

    private static boolean isReleasing = false;
    private static ApkReleaser releaser;

    private AmigoDirs amigoDirs;
    private PatchApk patchApk;

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

    private ApkReleaser(Context context) {
        this.context = context;
        this.service = Executors.newFixedThreadPool(3);
        this.amigoDirs = AmigoDirs.getInstance();
        this.patchApk = PatchApk.getInstance();
    }

    public void release() {
        if (isReleasing) {
            return;
        }
        Log.e(TAG, "release doing--->" + isReleasing);
        service.submit(new Runnable() {
            @Override
            public void run() {
                isReleasing = true;
                DexReleaser.releaseDexes(patchApk.patchFile(), amigoDirs.dexDir());
                NativeLibraryHelperCompat.copyNativeBinaries(patchApk.patchFile(), amigoDirs.libDir());
                dexOptimization();
            }
        });
    }


    private void dexOptimization() {
        Log.e(TAG, "dexOptimization");
        File[] listFiles = amigoDirs.dexDir().listFiles();

        final List<File> validDexes = new ArrayList<>();
        for (File listFile : listFiles) {
            if (listFile.getName().endsWith(".dex")) {
                validDexes.add(listFile);
            }
        }

        final CountDownLatch countDownLatch = new CountDownLatch(validDexes.size());

        for (final File dex : validDexes) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    long startTime = System.currentTimeMillis();
                    String optimizedPath = optimizedPathFor(dex, amigoDirs.dexOptDir());
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
                    Log.e(TAG, String.format("dex %s consume %d ms", dex.getAbsolutePath(), System.currentTimeMillis() - startTime));
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
        handler.sendEmptyMessage(WHAT_DEX_OPT_DONE);
    }

    private static final String DEX_SUFFIX = ".dex";

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

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT_DEX_OPT_DONE:
                    isReleasing = false;
                    doneDexOpt();
                    saveDexAndSoChecksum();
                    SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS);
                    sp.edit().putString(Amigo.NEW_APK_SIG, patchApk.checksum()).commit();
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

    private void saveDexAndSoChecksum() {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS);
        File[] dexFiles = amigoDirs.dexDir().listFiles();
        for (File dexFile : dexFiles) {
            String checksum = getCrc(dexFile);
            sp.edit().putString(dexFile.getAbsolutePath(), checksum).commit();
        }

        File[] dexOptFiles = amigoDirs.dexOptDir().listFiles();
        for (File dexOptFile : dexOptFiles) {
            String checksum = getCrc(dexOptFile);
            sp.edit().putString(dexOptFile.getAbsolutePath(), checksum).commit();
        }

        File[] nativeFiles = amigoDirs.libDir().listFiles();
        if (nativeFiles != null && nativeFiles.length > 0) {
            for (File nativeFile : nativeFiles) {
                String checksum = getCrc(nativeFile);
                sp.edit().putString(nativeFile.getAbsolutePath(), checksum).commit();
            }
        }
    }

    private void doneDexOpt() {
        context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS)
                .edit()
                .putBoolean(patchApk.checksum(), true)
                .commit();
    }

    private boolean isDexOptDone() {
        return context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS)
                .getBoolean(patchApk.checksum(), false);
    }

    public void work(int layoutId, int themeId) {
        if (!ProcessUtils.isLoadDexProcess(context)) {
            if (!isDexOptDone()) {
                waitDexOptDone(layoutId, themeId);
            }
        }
    }

    private void waitDexOptDone(int layoutId, int themeId) {
        new Launcher(context).layoutId(layoutId).themeId(themeId).launch();

        while (!isDexOptDone()) {
            try {
                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
