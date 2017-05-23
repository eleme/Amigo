package me.ele.amigo.utils.dexExtractor;

import android.app.Application;
import android.os.Build;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;
import me.ele.amigo.AmigoDirs;
import me.ele.amigo.PatchApks;
import me.ele.amigo.utils.CrcUtils;
import me.ele.amigo.utils.DexExtractor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static me.ele.amigo.utils.FileUtils.copyFile;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.KITKAT})
public class DexExtractorOnDalvikVmTest {

    public static String setUpPatchFile() throws IOException {
        final Application application = RuntimeEnvironment.application;
        AmigoDirs.getInstance(application).clear();
        InputStream inputStream =
                DexExtractorOnDalvikVmTest.class.getClassLoader()
                        .getResourceAsStream("app_patch.apk");
        assertNotNull(inputStream);
        File tempFile = new File(AmigoDirs.getInstance(application).amigoDir(), "temp.apk");
        FileOutputStream fos = new FileOutputStream(tempFile);
        copyFile(inputStream, fos);
        fos.close();
        inputStream.close();

        String patchChecksum = CrcUtils.getCrc(tempFile);
        assertEquals(false, PatchApks.getInstance(application).exists(patchChecksum));
        File dstFile = PatchApks.getInstance(application).patchFile(patchChecksum);
        tempFile.renameTo(dstFile);
        return patchChecksum;
    }

    @Test
    public void testExtractDexFilesOnDalvik() throws IOException {
        final Application application = RuntimeEnvironment.application;
        String patchChecksum = setUpPatchFile();

        assertEquals(true, PatchApks.getInstance(application).patchFile(patchChecksum).exists());
        DexExtractor dexExtractor = new DexExtractor(application, patchChecksum);
        assertEquals(true, dexExtractor.extractDexFiles());
        assertEquals(getDexCount(PatchApks.getInstance(application).patchFile(patchChecksum)),
                AmigoDirs.getInstance(application).dexDir(patchChecksum).listFiles().length);
    }

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
        }finally {
            if(file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
    }
}
