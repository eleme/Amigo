package me.ele.amigo.release;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.KITKAT})
public class ApkReleaseActivityTest {

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

    @Test
    public void testOnCreate() {
        try {
            Robolectric.buildActivity(ApkReleaseActivity.class).get();
        } catch (RuntimeException e) {
            Assert.assertEquals(true,
                    e.getMessage().contains("patch apk checksum must not be empty"));
        }

        Robolectric.buildActivity(ApkReleaseActivity.class)
                .newIntent(new Intent().putExtra(ApkReleaseActivity.PATCH_CHECKSUM, "dummy")
                        .putExtra(ApkReleaseActivity.LAYOUT_ID,
                                android.R.layout.simple_list_item_1)
                        .putExtra(ApkReleaseActivity.THEME_ID, android.R.style.TextAppearance))
                .get();
    }

    public interface Task {
        void onRun() throws Exception;
    }
}
