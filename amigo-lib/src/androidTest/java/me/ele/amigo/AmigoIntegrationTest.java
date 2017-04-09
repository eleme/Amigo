package me.ele.amigo;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import android.util.Log;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipFile;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

import dalvik.system.PathClassLoader;
import me.ele.amigo.reflect.MethodUtils;
import me.ele.amigo.release.ApkReleaser;
import me.ele.amigo.utils.CrcUtils;
import me.ele.amigo.utils.FileUtils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static me.ele.amigo.AmigoService.MSG_ID_DEX_OPT_FAILURE;
import static me.ele.amigo.AmigoService.MSG_ID_DEX_OPT_SUCCESS;
import static me.ele.amigo.utils.FileUtils.copyFile;

@RunWith(AndroidJUnit4.class)
public class AmigoIntegrationTest {

    public static int getDexCount(File jar) {
        ZipFile file = null;
        try {
            file = new ZipFile(jar);
            int count = 0;
            while (file.getEntry("classes" + (count == 0 ? "" : String.valueOf(count + 1)) + ".dex")
                    != null) {
                count++;
            }

            return count;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
    }

    public static File preparePatchFile(Context appContext) throws IOException {
        AmigoDirs.getInstance(appContext).clear();
        PatchInfoUtil.clear(appContext);

        InputStream inputStream = appContext.getAssets().open("app_patch.apk");
        assertNotNull(inputStream);
        File tempFile = new File(AmigoDirs.getInstance(appContext).amigoDir(), "temp.apk");
        FileOutputStream fos = new FileOutputStream(tempFile);
        copyFile(inputStream, fos);
        fos.close();
        inputStream.close();
        return tempFile;
    }

    @Test
    public void testRunOriginalApplication() throws Exception {

        final Context appContext = InstrumentationRegistry.getTargetContext();

        runOnMainThreadAndWait(new Task() {
            @Override public void onRun() throws Exception {
                Amigo.clear(appContext);
                AmigoDirs.getInstance(appContext).clear();

                Amigo amigo = new Amigo();
                MethodUtils.invokeMethod(amigo, "attach", appContext);
                amigo.onCreate();
            }
        });

        Assert.assertEquals(Application.class,
                appContext.getApplicationContext().getClass());
        Assert.assertEquals(PathClassLoader.class,
                appContext.getApplicationContext().getClassLoader().getClass());
        Assert.assertEquals(false, Amigo.hasWorked());
        Assert.assertNull(Amigo.getLoadPatchError());
        Assert.assertEquals("", Amigo.getWorkingPatchApkChecksum(appContext));
    }

    @Test
    public void testRunPatchApplication() throws Exception {
        final Context appContext = InstrumentationRegistry.getTargetContext();
        final String checksum = releasePatchFile(appContext);

        runOnMainThreadAndWait(new Task() {
            @Override public void onRun() throws Exception {
                // setup a valid patch apk
                Amigo amigo = new Amigo();
                MethodUtils.invokeMethod(amigo, "attach", appContext);
                amigo.onCreate();
            }
        });

        Assert.assertEquals("me.ele.demo.ApplicationContext",
                appContext.getApplicationContext().getClass().getName());
        Assert.assertEquals(AmigoClassLoader.class,
                appContext.getApplicationContext().getClassLoader().getClass());
        Assert.assertEquals(true, Amigo.hasWorked());
        Assert.assertNull(Amigo.getLoadPatchError());
        Assert.assertEquals(checksum, PatchInfoUtil.getWorkingChecksum(appContext));
        Assert.assertEquals(checksum, Amigo.getWorkingPatchApkChecksum(appContext));
    }

    @Test
    public void testRunRawPatchApk() throws Exception {
        final Context appContext = InstrumentationRegistry.getTargetContext();

        // setup a valid patch apk
        String checksum = releasePatchFile(appContext);
        // delete optimized apk
        FileUtils.removeFile(AmigoDirs.getInstance(appContext).dexOptDir(checksum));

        runOnMainThreadAndWait(new Task() {
            @Override public void onRun() throws Exception {
                Amigo amigo = new Amigo();
                MethodUtils.invokeMethod(amigo, "attach", appContext);
                amigo.onCreate();
            }
        });

        Assert.assertEquals("me.ele.demo.ApplicationContext",
                appContext.getApplicationContext().getClass()
                        .getName());
        Assert.assertEquals(AmigoClassLoader.class,
                appContext.getApplicationContext().getClassLoader().getClass());
        Assert.assertEquals(true, Amigo.hasWorked());
        Assert.assertNull(Amigo.getLoadPatchError());
        Assert.assertEquals(checksum, PatchInfoUtil.getWorkingChecksum(appContext));
        Assert.assertEquals(checksum, Amigo.getWorkingPatchApkChecksum(appContext));
    }

    @NonNull
    private String releasePatchFile(Context appContext)
            throws IOException, InterruptedException {
        File patchFile = preparePatchFile(appContext);
        assertEquals(true, patchFile.exists());
        String patchChecksum = CrcUtils.getCrc(patchFile);
        assertEquals(false, PatchApks.getInstance(appContext).exists(patchChecksum));
        File dstFile = PatchApks.getInstance(appContext).patchFile(patchChecksum);
        patchFile.renameTo(dstFile); //copying to working dir
        int dexCount = getDexCount(dstFile);

        final Object lock = new Object();
        final AtomicBoolean success = new AtomicBoolean();
        new ApkReleaser(appContext).release(patchChecksum, new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                success.set(true);
                switch (msg.what) {
                    case MSG_ID_DEX_OPT_FAILURE:
                        success.set(false);
                    case MSG_ID_DEX_OPT_SUCCESS:
                        synchronized (lock) {
                            lock.notify();
                        }
                }
            }
        });

        synchronized (lock) {
            lock.wait();
        }
        assertEquals("faild to release apk", true, success.get());

        if (Build.VERSION.SDK_INT < 21) {
            assertEquals(dexCount,
                    AmigoDirs.getInstance(appContext).dexDir(patchChecksum).listFiles().length);
        }

        return patchChecksum;
    }

    @Test
    public void testWorkLaterNoCheck() throws IOException, InterruptedException {
        final Context appContext = InstrumentationRegistry.getTargetContext();
        final File file = preparePatchFile(appContext);

        Handler handler = new Handler(Looper.getMainLooper());
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicBoolean patchApkReleasedSuccess = new AtomicBoolean();
        handler.post(new Runnable() {
            @Override public void run() {
                Amigo.workLaterWithoutCheckingSignature(appContext, file, new Amigo.WorkLaterCallback() {
                    @Override public void onPatchApkReleased(boolean success) {
                        patchApkReleasedSuccess.set(success);
                        countDownLatch.countDown();
                    }
                });
            }
        });

        countDownLatch.await();
        assertEquals(true, patchApkReleasedSuccess.get());
        assertEquals(CrcUtils.getCrc(file), PatchInfoUtil.getWorkingChecksum(appContext));
    }

    @Test
    public void testWorkLater() throws IOException, InterruptedException {
        final Context appContext = InstrumentationRegistry.getTargetContext();
        final File file = preparePatchFile(appContext);

        Handler handler = new Handler(Looper.getMainLooper());
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicBoolean patchApkReleasedSuccess = new AtomicBoolean();
        handler.post(new Runnable() {
            @Override public void run() {
                Amigo.workLater(appContext, file, new Amigo.WorkLaterCallback() {
                    @Override public void onPatchApkReleased(boolean success) {
                        patchApkReleasedSuccess.set(success);
                        countDownLatch.countDown();
                    }
                });
            }
        });

        countDownLatch.await();
        assertEquals(true, patchApkReleasedSuccess.get());
        assertEquals(CrcUtils.getCrc(file), PatchInfoUtil.getWorkingChecksum(appContext));
    }

    public interface Task {
        void onRun() throws Exception;
    }

    public static void runOnMainThreadAndWait(final Task task) throws Exception {
        Handler handler = new Handler(Looper.getMainLooper());

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<Exception> possibleException = new AtomicReference<>();
        handler.post(new Runnable() {
            @Override public void run() {
                try {
                    task.onRun();
                } catch (Exception e) {
                    possibleException.set(e);
                } finally {
                    countDownLatch.countDown();
                }
            }
        });
        countDownLatch.await();
        if (possibleException.get() != null) {
            throw possibleException.get();
        }
    }

}
