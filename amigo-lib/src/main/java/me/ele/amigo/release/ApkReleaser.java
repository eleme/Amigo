package me.ele.amigo.release;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import dalvik.system.DexFile;
import me.ele.amigo.Amigo;
import me.ele.amigo.compat.NativeLibraryHelperCompat;
import me.ele.amigo.utils.CrcUtils;
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
    private static AtomicBoolean isReleasing = new AtomicBoolean(false);
    private static ApkReleaser releaser;
    private Context context;
    private File directory;
    private File demoAPk;
    private File optimizedDir;
    private File dexDir;
    private File nativeLibraryDir;
    private ExecutorService service;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT_DEX_OPT_DONE:
                    isReleasing.set(false);
                    ApkReleaser.doneDexOpt(context);
                    saveDexAndSoChecksum();
                    SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS);
                    String demoApkChecksum = getCrc(demoAPk);
                    sp.edit().putString(Amigo.NEW_APK_SIG, demoApkChecksum).commit();
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

        directory = new File(context.getFilesDir(), "amigo");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        demoAPk = new File(directory, "demo.apk");

        optimizedDir = new File(directory, "dex_opt");
        if (!optimizedDir.exists()) {
            optimizedDir.mkdir();
        }
        dexDir = new File(directory, "dex");
        if (!dexDir.exists()) {
            dexDir.mkdir();
        }
        nativeLibraryDir = new File(directory, "lib");
        if (!nativeLibraryDir.exists()) {
            nativeLibraryDir.mkdir();
        }

        service = Executors.newFixedThreadPool(3);
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

    public static void doneDexOpt(Context context) {
        context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS)
                .edit()
                .putBoolean(getUniqueKey(context), true)
                .apply();
    }

    private static boolean isDexOptDone(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS)
                .getBoolean(getUniqueKey(context),
                        false);
    }

    private static String getUniqueKey(Context context) {
        return CrcUtils.getCrc(Amigo.getHotfixApk(context));
    }

    public static void work(Context context, int layoutId, int themeId) {
        if (!ProcessUtils.isLoadDexProcess(context)) {
            if (!isDexOptDone(context)) {
                waitDexOptDone(context, layoutId, themeId);
            }
        }
    }

    private static void waitDexOptDone(Context context, int layoutId, int themeId) {

        new Launcher(context, layoutId).themeId(themeId).launch();

        while (!isDexOptDone(context)) {
            try {
                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void release() {
        if (isReleasing.get()) {
            return;
        }

        Log.e(TAG, "release doing--->" + isReleasing);
        service.submit(new Runnable() {
            @Override
            public void run() {
                isReleasing.set(true);

                long t1 = SystemClock.elapsedRealtime();
                DexReleaser.releaseDexes(demoAPk.getAbsolutePath(), dexDir.getAbsolutePath());
                long t2 = SystemClock.elapsedRealtime();
                Log.d(TAG, "releaseDexes: consumed " + (t2 - t1) + " millis");
                NativeLibraryHelperCompat.copyNativeBinaries(demoAPk, nativeLibraryDir);
                long t3 = SystemClock.elapsedRealtime();
                Log.d(TAG, "releaseDexes: consumed " + (t3 - t2) + " millis");
                dexOptimization();
                long t4 = SystemClock.elapsedRealtime();
                Log.d(TAG, "releaseDexes: consumed " + (t4 - t3) + " millis");
            }
        });
    }

    private void dexOptimization() {
        Log.e(TAG, "dexOptimization");
        File[] listFiles = dexDir.listFiles();

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
                    String optimizedPath = optimizedPathFor(dex, optimizedDir);
                    DexFile dexFile = null;
                    try {
                        dex.delete();
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

    private void saveDexAndSoChecksum() {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_MULTI_PROCESS);
        File[] dexFiles = dexDir.listFiles();
        for (File dexFile : dexFiles) {
            String checksum = getCrc(dexFile);
            sp.edit().putString(dexFile.getAbsolutePath(), checksum).commit();
        }

        File[] dexOptFiles = optimizedDir.listFiles();
        for (File dexFile : dexOptFiles) {
            String checksum = getCrc(dexFile);
            sp.edit().putString(dexFile.getAbsolutePath(), checksum).commit();
        }

        File[] nativeFiles = nativeLibraryDir.listFiles();
        if (nativeFiles != null && nativeFiles.length > 0) {
            for (File nativeFile : nativeFiles) {
                String checksum = getCrc(nativeFile);
                sp.edit().putString(nativeFile.getAbsolutePath(), checksum).commit();
            }
        }
    }

}
