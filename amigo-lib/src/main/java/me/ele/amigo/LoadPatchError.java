package me.ele.amigo;


public class LoadPatchError {
    public static final int SIG_ERR = 1;

    public static final int LOAD_ERR = 2;

    public static LoadPatchError record(int type, Exception e) {
        return new LoadPatchError(type, e);
    }

    private int type;
    private Exception exception;

    public LoadPatchError(int type, Exception exception) {
        this.type = type;
        this.exception = exception;
    }

    public int getType() {
        return type;
    }

    public Exception getException() {
        return exception;
    }
}
