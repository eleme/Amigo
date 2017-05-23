package me.ele.amigo;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import me.ele.amigo.utils.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AmigoClassLoaderTest {

    @Test
    public void testAmigoClassLoader() throws Exception {
        AmigoIntegrationTest integrationTest = new AmigoIntegrationTest();
        integrationTest.testRunPatchApplication();

        Context appContext = InstrumentationRegistry.getTargetContext();
        String workingChecksum = Amigo.getWorkingPatchApkChecksum(appContext);
        AmigoClassLoader classLoader = AmigoClassLoader.newInstance(appContext, workingChecksum);
        Assert.assertNotNull(classLoader.loadClass("me.ele.app.amigo.HomeActivity"));
        try {
            // load unknown class
            classLoader.loadClass("me.ele.app.amigo.HomeActivity1");
        } catch (Exception e) {
            Assert.assertEquals(ClassNotFoundException.class, e.getClass());
        }
        Assert.assertNotNull(classLoader.findResource("AndroidManifest.xml"));
    }

    @Test
    public void testJoinPath() throws IOException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        File tempDir = new File(appContext.getFilesDir(), "test_join_path");
        FileUtils.removeFile(tempDir);
        tempDir.mkdir();

        Assert.assertEquals(null, AmigoClassLoader.joinPath(tempDir));

        File file1 = new File(tempDir, "a.txt");
        file1.createNewFile();
        Assert.assertEquals(true,
                Arrays.asList(AmigoClassLoader.joinPath(tempDir).split(File.pathSeparator))
                        .containsAll(Collections.singletonList(file1.getAbsolutePath())));

        File file2 = new File(tempDir, "b.txt");
        file2.createNewFile();
        Assert.assertEquals(true,
                Arrays.asList(AmigoClassLoader.joinPath(tempDir).split(File.pathSeparator))
                        .containsAll(
                                Arrays.asList(file1.getAbsolutePath(), file2.getAbsolutePath())));

        File file3 = new File(tempDir, "c.txt");
        file3.createNewFile();
        Assert.assertEquals(true,
                Arrays.asList(AmigoClassLoader.joinPath(tempDir).split(File.pathSeparator))
                        .containsAll(Arrays.asList(file1.getAbsolutePath(), file2.getAbsolutePath(),
                                file3.getAbsolutePath())));
    }
}
