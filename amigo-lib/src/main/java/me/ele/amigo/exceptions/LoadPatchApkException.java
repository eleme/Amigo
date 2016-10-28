package me.ele.amigo.exceptions;

public class LoadPatchApkException extends Exception {
    public LoadPatchApkException(Throwable throwable) {
        super(throwable);
    }

    public LoadPatchApkException(String msg) {
        super(msg);
    }
}
