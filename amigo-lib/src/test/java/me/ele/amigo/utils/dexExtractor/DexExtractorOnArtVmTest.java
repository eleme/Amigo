package me.ele.amigo.utils.dexExtractor;

import android.app.Application;
import android.os.Build;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;
import me.ele.amigo.utils.DexExtractor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.LOLLIPOP})
public class DexExtractorOnArtVmTest {

    @Test
    public void testExtractDexFilesOnArt() {
        final Application application = RuntimeEnvironment.application;
        String dummyChecksum = "131435";
        DexExtractor dexExtractor = new DexExtractor(application, dummyChecksum);
        assertEquals(true, dexExtractor.extractDexFiles());
    }

    @Test
    public void testCloseSilently() throws IOException {
        DexExtractor.closeSilently(new ByteArrayInputStream(new byte[0]));
        DexExtractor.closeSilently((InputStream) null);
        InputStream inputStream = mock(InputStream.class);
        Mockito.doThrow(new IOException("mock io exception")).when(inputStream).close();
        DexExtractor.closeSilently(inputStream);

        ZipFile zipFile = mock(ZipFile.class);
        Mockito.doThrow(new IOException("mock io exception")).when(zipFile).close();
        DexExtractor.closeSilently(zipFile);

        DexExtractor.closeSilently(mock(ZipFile.class));
        DexExtractor.closeSilently((ZipFile) null);
    }

    @Test
    public void testVerifyZip() throws IOException {
        Assert.assertEquals(true, DexExtractor.contentEquals(new ByteArrayInputStream(new byte[10]),
                new ByteArrayInputStream(new byte[10])));

        Assert.assertEquals(false, DexExtractor.contentEquals(new ByteArrayInputStream(new byte[10]),
                new ByteArrayInputStream(new byte[11])));

        InputStream inputStream = mock(InputStream.class);
        when(inputStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("mack io exception"));

        Assert.assertEquals(false,
                DexExtractor.contentEquals(new ByteArrayInputStream(new byte[10]),
                        inputStream));
    }
}
