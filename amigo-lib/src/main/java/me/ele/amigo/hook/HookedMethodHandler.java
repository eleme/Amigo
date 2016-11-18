package me.ele.amigo.hook;

import android.content.Context;

import java.lang.reflect.Method;

import me.ele.amigo.Amigo;
import me.ele.amigo.utils.Log;

public class HookedMethodHandler {

    private static final String TAG = HookedMethodHandler.class.getSimpleName();
    protected final Context context;

    private Object fakedResult = null;
    private boolean useFakedResult = false;

    public HookedMethodHandler(Context context) {
        this.context = context;
    }

    public Object doHookInner(Object receiver, Method method, Object[] args) throws
            Throwable {
        long b = System.currentTimeMillis();
        Amigo.rollAmigoBack(context);
        useFakedResult = false;
        fakedResult = null;
        boolean suc = beforeInvoke(receiver, method, args);
        Log.d(TAG, "doHookInner beforeInvoke cost %s ms", System.currentTimeMillis()
                - b);
        b = System.currentTimeMillis();
        Object invokeResult = null;
        if (!suc) {
            invokeResult = method.invoke(receiver, args);
            Log.i(TAG, "doHookInner invoke cost %s ms", System.currentTimeMillis() - b);
            b = System.currentTimeMillis();
        }
        afterInvoke(receiver, method, args, invokeResult);
        Log.d(TAG, "doHookInner afterInvoke cost %s ms", System.currentTimeMillis() -
                b);
        if (useFakedResult) {
            return fakedResult;
        } else {
            return invokeResult;
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
