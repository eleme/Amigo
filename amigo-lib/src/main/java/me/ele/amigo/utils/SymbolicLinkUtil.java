package me.ele.amigo.utils;

import java.io.File;
import java.io.IOException;

public class SymbolicLinkUtil {

    private static boolean libLoaded = false;

    static {
        libLoaded = NativeLibLoader.loadLibrary("symbolic-link");
    }

    public static void createLink(File targetFile, File link) throws IOException {
        if (!libLoaded) {
            throw new IOException(
                    "unable to create symbolic link because of native lib loading failure");
        }

        if (link.exists()) {
            throw new IOException("unable to create symbolic link: link file already exists");
        }

        if (!targetFile.exists()) {
            throw new IOException(
                    String.format("unable to create symbolic link: target file doesn't exist"));
        }

        makeSymbolicLink(link.getAbsolutePath(), targetFile.getAbsolutePath());
    }

    private static native int makeSymbolicLink(String link, String target);
}
