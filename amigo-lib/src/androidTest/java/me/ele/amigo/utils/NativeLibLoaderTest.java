package me.ele.amigo.utils;

import android.support.test.runner.AndroidJUnit4;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NativeLibLoaderTest {

    @Test
    public void testCreateSymbolicLink() throws IOException {
        Assert.assertEquals(false, NativeLibLoader.loadLibrary("dummy_lib"));
        Assert.assertEquals(true, NativeLibLoader.loadLibrary("symbolic-link"));

        NativeLibLoader.delegate = new NativeLibLoader.NativeLibLoaderDelegate() {
            @Override public boolean loadLibrary(String libName) {
                try {
                    System.loadLibrary(libName);
                    return true;
                } catch (UnsatisfiedLinkError error) {
                    return false;
                }
            }
        };

        Assert.assertEquals(false, NativeLibLoader.loadLibrary("dummy_lib"));
        Assert.assertEquals(true, NativeLibLoader.loadLibrary("symbolic-link"));
    }
}
