package me.ele.amigo.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.junit.Assert;
import org.junit.Test;

public class MessageDigestUtilTest {

    @Test
    public void testMd5() throws IOException, NoSuchAlgorithmException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                "laeklaelfjpwjefal3pef5&*^97329772390185".getBytes("UTF-8"));
        byte[] actual = DigestUtils.md5Digest(byteArrayInputStream);
        Assert.assertEquals("b084089d46c97be0b74126108eff0049",
                org.apache.commons.codec.binary.Hex.encodeHexString(actual));
    }

    @Test
    public void testError() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                "laeklaelfjpwjefal3pef5&*^97329772390185".getBytes("UTF-8"));
        try {
            DigestUtils.digest("unknown-algorithm", byteArrayInputStream);
        } catch (Exception e) {
            Assert.assertEquals(NoSuchAlgorithmException.class, e.getClass());
        }
    }
}
