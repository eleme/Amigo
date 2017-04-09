package me.ele.amigo.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

//注 ：手动运行 testHoldLock() testWaitLockToRelease()，且让他们先后在***两个进程***中运行
public class FileLockUtilTest {

    // works on unix/linux
    public static String getPid() {
        String[] cmd = new String[]{"/bin/sh", "-c", "echo $$ $PPID"};
        InputStream is = null;
        String pid;
        ByteArrayOutputStream baos = null;
        try {
            byte[] buf = new byte[1024];
            Process exec = Runtime.getRuntime().exec(cmd);
            is = exec.getInputStream();
            baos = new ByteArrayOutputStream();
            while (is.read(buf) != -1) {
                baos.write(buf);
            }
            String ppids = baos.toString();
            pid = ppids.split(" ")[1];
            int index = pid.indexOf("\n");
            if (index == -1) {
                index = pid.length();
            }
            pid = pid.substring(0, index);
            exec.waitFor();
            return pid;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
                baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private File getTestFile() {
        return new File("./build.gradle");
    }

    @Test
    public void testHoldLock() throws Exception {
        String pid = getPid();

        File file = getTestFile();
        assertEquals(true, file.exists());
        FileLockUtil.ExclusiveFileLock lock = FileLockUtil.getFileLock(file);
        lock.lock();
        System.out.printf("got file lock...\n");
        byte[] data = lock.readFully();
        int count = holdCount;
        while (count-- > 0) {
            System.out.printf("process %s is holding the file lock\n", pid);
            System.out.flush();
            Thread.sleep(1000);
        }

        lock.release();
        assertEquals(file.length(), data.length);
    }

    final int waitCount = 9;
    final int holdCount = 5;

    @Test
    public void testWaitLockToRelease() throws Exception {
        File file = getTestFile();
        assertEquals(true, file.exists());
        FileLockUtil.ExclusiveFileLock lock = FileLockUtil.getFileLock(file);

        System.out.println("running in process " + getPid());

        int count = waitCount;
        while (count-- > 0) {
            try {
                if (lock.tryLock()) {
                    System.out.printf("got file lock...\n");
                    break;
                }
                System.out.printf("file lock is hold by another process, try again later...\n");
                System.out.flush();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.printf("unable to get file lock, try again later...\n");
                System.out.flush();
                if (count == 0) {
                    throw new IOException("failed to lock file after tried 80 times");
                }
            }
            Thread.sleep(1000);
        }

        byte[] data = lock.readFully();
        lock.release();
        assertEquals(file.length(), data.length);
    }

}