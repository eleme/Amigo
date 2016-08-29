package me.ele.amigo.release;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import dalvik.system.DexClassLoader;
import me.ele.amigo.compat.NativeLibraryHelperCompat;
import me.ele.amigo.utils.DexReleaser;
import me.ele.amigo.utils.DexUtils;

public class ApkReleaseActivity extends Activity {

    private static final String TAG = ApkReleaseActivity.class.getSimpleName();

    static final int WHAT_DEX_OPT_DONE = 1;
    static final int WHAT_FINISH = 2;

    static final int DELAY_FINISH_TIME = 4000;
    static final String LAYOUT_ID = "layout_id";
    static final String THEME_ID = "theme_id";

    private int layoutId;
    private int themeId;

    private File directory;
    private File demoAPk;
    private File optimizedDir;
    private File dexDir;
    private File nativeLibraryDir;

    private AtomicInteger counter = new AtomicInteger();
    private ExecutorService service = Executors.newFixedThreadPool(3);

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT_DEX_OPT_DONE:
                    ApkReleaser.doneDexOpt(ApkReleaseActivity.this);
                    handler.sendEmptyMessageDelayed(WHAT_FINISH, DELAY_FINISH_TIME);
                    break;
                case WHAT_FINISH:
                    finish();
                    overridePendingTransition(0, 0);
                    System.exit(0);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        layoutId = getIntent().getIntExtra(LAYOUT_ID, 0);
        themeId = getIntent().getIntExtra(THEME_ID, 0);
        if (themeId != 0) {
            setTheme(themeId);
        }
        if (layoutId != 0) {
            setContentView(layoutId);
        }

        directory = new File(getFilesDir(), "amigo");
        demoAPk = new File(directory, "demo.apk");

        optimizedDir = new File(directory, "dex_opt");
        dexDir = new File(directory, "dex");
        nativeLibraryDir = new File(directory, "lib");

        new Thread() {
            @Override
            public void run() {
                super.run();
                DexReleaser.releaseDexes(demoAPk.getAbsolutePath(), dexDir.getAbsolutePath());
                NativeLibraryHelperCompat.copyNativeBinaries(demoAPk, nativeLibraryDir);
                dexOptimization();
            }
        }.start();
    }

    private void dexOptimization() {
        File[] listFiles = dexDir.listFiles();

        final List<File> validDexes = new ArrayList<>();
        for (File listFile : listFiles) {
            if (listFile.getName().endsWith(".dex")) {
                validDexes.add(listFile);
            }
        }

        for (final File dex : validDexes) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    long startTime = System.currentTimeMillis();
                    new DexClassLoader(dex.getAbsolutePath(), optimizedDir.getAbsolutePath(), null, DexUtils.getPathClassLoader());
                    Log.e(TAG, String.format("dex %s consume %d ms", dex.getAbsolutePath(), System.currentTimeMillis() - startTime));
                    int num = counter.incrementAndGet();
                    if (num == validDexes.size()) {
                        handler.sendEmptyMessage(WHAT_DEX_OPT_DONE);
                    }
                }
            });
        }
    }


    @Override
    public void onBackPressed() {
        //do nothing
    }
}
