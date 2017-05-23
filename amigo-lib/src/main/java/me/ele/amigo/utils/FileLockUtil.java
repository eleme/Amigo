package me.ele.amigo.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Locale;

public class FileLockUtil {

    public static ExclusiveFileLock getFileLock(File file) throws FileNotFoundException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        return new ExclusiveFileLockImpl(raf, file.getPath());
    }

    public interface ExclusiveFileLock {
        void lock() throws IOException;

        boolean tryLock() throws IOException;

        byte[] readFully() throws IOException;

        long getFileLength() throws IOException;

        // will override existing data
        void write(byte[] content) throws IOException;

        void release();
    }

    private static class ExclusiveFileLockImpl implements ExclusiveFileLock {

        private final RandomAccessFile raf;
        private final String fileName;
        private FileLock lock;

        private ExclusiveFileLockImpl(RandomAccessFile raf, String fileName) {
            this.raf = raf;
            this.fileName = fileName;
        }

        @Override public void lock() throws IOException {
            FileChannel channel = raf.getChannel();
            lock = channel.lock();
        }

        @Override public boolean tryLock() throws IOException {
            FileChannel channel = raf.getChannel();
            lock = channel.tryLock();
            return lock != null;
        }

        @Override public byte[] readFully() throws IOException {
            long length = raf.length();
            if (length > Integer.MAX_VALUE) {
                throw new IOException(
                        String.format(Locale.US, "file[%s] too large to read fully: %d bytes",
                                fileName,
                                length));
            }

            byte[] data = new byte[(int) length];
            raf.readFully(data);
            return data;
        }

        @Override public long getFileLength() throws IOException {
            return raf.length();
        }

        @Override public void write(byte[] content) throws IOException {
            raf.setLength(0);
            if (content != null) {
                raf.write(content);
            }
        }

        @Override public void release() {
            try {
                if (lock != null) {
                    lock.release();
                }
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
