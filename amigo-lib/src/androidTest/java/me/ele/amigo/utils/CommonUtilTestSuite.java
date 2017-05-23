package me.ele.amigo.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
public class CommonUtilTestSuite {

    @Test
    public void testGetVersionCode() {
        Context context = InstrumentationRegistry.getContext();
        Context mockedContent = Mockito.mock(Context.class);
        Mockito.when(mockedContent.getPackageManager()).thenReturn(context.getPackageManager());
        Mockito.when(mockedContent.getPackageName()).thenReturn("com.android.systemui");
        Assert.assertNotEquals(0, CommonUtils.getVersionCode(mockedContent));

        // can't use context here, as the test app has no version code set
        Mockito.when(mockedContent.getPackageName()).thenReturn("com.android.systemui1111");
        Assert.assertEquals(0, CommonUtils.getVersionCode(mockedContent));
    }

    @Test
    public void testGetSignature() throws PackageManager.NameNotFoundException, IOException {
        Context application = InstrumentationRegistry.getContext();
        Assert.assertNotNull(CommonUtils.getSignature(application));
        File file = new File(application.getApplicationInfo().sourceDir);
        Assert.assertNotNull(CommonUtils.getSignature(application, file));
    }

    @Test
    public void testGetPackageInfoBelowICS() {
        if (Build.VERSION.SDK_INT >= 14) {
            return;
        }
        Context application = InstrumentationRegistry.getContext();
        Assert.assertNotNull(
                CommonUtils.getPackageInfoBelowICS(application.getApplicationInfo().sourceDir,
                        PackageManager.GET_SIGNATURES));
    }
}
