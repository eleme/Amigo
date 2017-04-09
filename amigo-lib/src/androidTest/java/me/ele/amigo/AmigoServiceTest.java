package me.ele.amigo;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import me.ele.amigo.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertNotNull;
import static me.ele.amigo.AmigoService.ACTION_RELEASE_DEX;
import static me.ele.amigo.AmigoService.EXTRA_APK_CHECKSUM;
import static me.ele.amigo.AmigoService.REMOTE_PROCESS_MSG_RECEIVER;
import static me.ele.amigo.AmigoService.REMOTE_PROCESS_REQUEST_ID;
import static me.ele.amigo.utils.FileUtils.copyFile;

@RunWith(AndroidJUnit4.class)
public class AmigoServiceTest {

    public static String setUpPatchFile(Context application) throws IOException {
        AmigoDirs.getInstance(application).clear();
        PatchInfoUtil.clear(application);

        InputStream inputStream = application.getAssets().open("app_patch.apk");
        assertNotNull(inputStream);
        File tempFile = new File(AmigoDirs.getInstance(application).amigoDir(), "temp.apk");
        FileOutputStream fos = new FileOutputStream(tempFile);
        copyFile(inputStream, fos);
        fos.close();
        inputStream.close();
        return PatchChecker.checkPatchAndCopy(application, tempFile, true);
    }

    @Test
    public void testHandleIntent() throws IOException, InterruptedException {
        final Context context = InstrumentationRegistry.getTargetContext();
        final AmigoService amigoService = new AmigoService();
        final String checksum = setUpPatchFile(context);

        final AtomicBoolean result = new AtomicBoolean();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final int reqCode = 100;
        final Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override public boolean handleMessage(Message msg) {
                if (msg.what == reqCode) {
                    result.set(msg.arg1 == 1);
                    countDownLatch.countDown();
                    return true;
                }
                return false;
            }
        });
        handler.post(new Runnable() {
            @Override public void run() {
                amigoService.onCreate();
                try {
                    FieldUtils.writeField(amigoService, "mBase", context);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(reqCode);
                }

                Messenger messenger = new Messenger(handler);
                amigoService.onHandleIntent(new Intent(context, AmigoService.class)
                        .putExtra(REMOTE_PROCESS_MSG_RECEIVER, messenger)
                        .putExtra(REMOTE_PROCESS_REQUEST_ID, reqCode)
                        .putExtra(EXTRA_APK_CHECKSUM, checksum)
                        .setAction(ACTION_RELEASE_DEX));
            }
        });

        countDownLatch.await();
        Assert.assertEquals(true, result.get());
        Assert.assertEquals(checksum, PatchInfoUtil.getWorkingChecksum(context));

        //Assert.assertEquals(true, AmigoDirs.getInstance(context).isOptedDexExists(checksum));
    }

    @Test
    public void testLifeCycle(){
        //life cycle test
        final Context context = InstrumentationRegistry.getTargetContext();
        final AmigoService amigoService = new AmigoService();
        if(Looper.myLooper() == null) {
            Looper.prepare();
        }
        amigoService.onCreate();
        amigoService.onBind(null);
        amigoService.onBind(new Intent());
        amigoService.onUnbind(null);
        amigoService.onUnbind(new Intent());
        amigoService.onStartCommand(new Intent(), 0, 0);
        amigoService.onStartCommand(null, 0, 0);
        amigoService.onDestroy();
        Looper.myLooper().quit();
    }
}
