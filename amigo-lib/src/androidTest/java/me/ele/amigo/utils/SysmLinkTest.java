package me.ele.amigo.utils;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import java.io.File;
import java.io.IOException;
import me.ele.amigo.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SysmLinkTest {

    @Test
    public void testCreateSymbolicLink() throws IOException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        File file = new File(appContext.getApplicationInfo().sourceDir);

        File link = new File(appContext.getFilesDir(), "source_apk_link");
        link.delete();

        SymbolicLinkUtil.createLink(file, link);
        Assert.assertEquals(true, link.exists());
        Assert.assertEquals(link.getCanonicalPath(), file.getCanonicalPath());

        try {
            SymbolicLinkUtil.createLink(file, link);
        } catch (Exception e) {
            Assert.assertEquals(true, e.getMessage().contains("link file already exists"));
        }

        try {
            link.delete();
            SymbolicLinkUtil.createLink(new File("not_exist_file"), link);
        } catch (Exception e) {
            Assert.assertEquals(true, e.getMessage().contains("target file doesn't exist"));
        }

        try {
            FieldUtils.writeStaticField(SymbolicLinkUtil.class, "libLoaded", false);
            SymbolicLinkUtil.createLink(new File("not_exist_file"), link);
        } catch (Exception e) {
            Assert.assertEquals(true, e.getMessage().contains("native lib loading failure"));
        }
    }
}
