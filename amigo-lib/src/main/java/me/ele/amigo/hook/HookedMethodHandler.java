package me.ele.amigo.hook;

import android.content.Context;

import java.lang.reflect.Method;

import me.ele.amigo.utils.Log;

public class HookedMethodHandler {

    private static final String TAG = HookedMethodHandler.class.getSimpleName();
    protected final Context context;

    private Object fakedResult = null;
    private boolean useFakedResult = false;

    public HookedMethodHandler(Context context) {
        this.context = context;
    }


    public synchronized Object doHookInner(Object receiver, Method method, Object[] args) throws
            Throwable {
        long b = System.currentTimeMillis();
        try {
            useFakedResult = false;
            fakedResult = null;
            boolean suc = beforeInvoke(receiver, method, args);
            Object invokeResult = null;
            if (!suc) {
                invokeResult = method.invoke(receiver, args);
            }
            afterInvoke(receiver, method, args, invokeResult);
            if (useFakedResult) {
                return fakedResult;
            } else {
                return invokeResult;
            }
        } finally {
            long time = System.currentTimeMillis() - b;
            if (time > 5) {
                Log.i(TAG, "doHookInner method(%s.%s) cost %s ms", method.getDeclaringClass()
                        .getName(), method.getName(), time);
            }
        }
    }

    public void setFakedResult(Object fakedResult) {
        this.fakedResult = fakedResult;
        useFakedResult = true;
    }

    protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
        return false;
    }

    protected void afterInvoke(Object receiver, Method method, Object[] args, Object
            invokeResult) throws Throwable {
    }
}
