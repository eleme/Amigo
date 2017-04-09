package me.ele.amigo.utils;

public final class NativeLibLoader {
    public static NativeLibLoaderDelegate delegate;

    private NativeLibLoader() {
    }

    public static boolean loadLibrary(String libName) {
        if (delegate == null) {
            try {
                System.loadLibrary(libName);
                return true;
            } catch (UnsatisfiedLinkError e) {
                e.printStackTrace();
                return false;
            }
        }

        return delegate.loadLibrary(libName);
    }

    interface NativeLibLoaderDelegate {
        boolean loadLibrary(String libName);
    }
}
