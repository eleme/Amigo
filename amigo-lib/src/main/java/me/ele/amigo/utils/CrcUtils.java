package me.ele.amigo.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.CRC32;

/**
 * Tools to build a quick partial crc of files.
 */
public final class CrcUtils {
    static class CentralDirectory {
        long offset;
        long size;
    }

    /**
     * Size of reading buffers.
     */
    private static final int BUFFER_SIZE = 0x4000;

    /**
     * Compute crc32 of the central directory of an apk. The central directory contains
     * the crc32 of each entries in the zip so the computed result is considered valid for the whole
     * zip file. Does not support zip64 nor multidisk but it should be OK for now since ZipFile does
     * not either.
     */
    public static String getCrc(File apk) {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(apk, "r");
            CentralDirectory dir = findCentralDirectory(raf);
            return String.valueOf(computeCrcOfCentralDir(raf, dir));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* Package visible for testing */
    static CentralDirectory findCentralDirectory(RandomAccessFile raf) throws IOException {
        CentralDirectory dir = new CentralDirectory();
        dir.size = Math.min(raf.length() >> 2, 10000);
        dir.offset = raf.length() >> 1;
        return dir;
    }

    /* Package visible for testing */
    static long computeCrcOfCentralDir(RandomAccessFile raf, CentralDirectory dir)
            throws IOException {
        CRC32 crc = new CRC32();
        long stillToRead = dir.size;
        raf.seek(dir.offset);
        int length = (int) Math.min(BUFFER_SIZE, stillToRead);
        byte[] buffer = new byte[BUFFER_SIZE];
        length = raf.read(buffer, 0, length);
        while (length != -1) {
            crc.update(buffer, 0, length);
            stillToRead -= length;
            if (stillToRead == 0) {
                break;
            }
            length = (int) Math.min(BUFFER_SIZE, stillToRead);
            length = raf.read(buffer, 0, length);
        }
        return crc.getValue();
    }
}