package me.ele.amigo.hook;

import android.content.Context;
import android.text.TextUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public abstract class ProxyHook extends Hook implements InvocationHandler {

    protected Object proxyObj;

    public ProxyHook(Context context) {
        super(context);
    }

    public void setProxyObj(Object proxyObj) {
        this.proxyObj = proxyObj;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            HookedMethodHandler hookedMethodHandler = mHookHandles.getHookedMethodHandler(method);
            if (hookedMethodHandler != null) {
                return hookedMethodHandler.doHookInner(proxyObj, method, args);
            }
            return method.invoke(proxyObj, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause != null && MyProxy.isMethodDeclaredThrowable(method, cause)) {
                throw cause;
            } else if (cause != null) {
                RuntimeException runtimeException = !TextUtils.isEmpty(cause.getMessage()) ? new
                        RuntimeException(cause.getMessage()) : new RuntimeException();
                runtimeException.initCause(cause);
                throw runtimeException;
            } else {
                RuntimeException runtimeException = !TextUtils.isEmpty(e.getMessage()) ? new
                        RuntimeException(e.getMessage()) : new RuntimeException();
                runtimeException.initCause(e);
                throw runtimeException;
            }
        } catch (IllegalArgumentException e) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(" Amigo{");
                if (method != null) {
                    sb.append("method[").append(method.toString()).append("]");
                } else {
                    sb.append("method[").append("NULL").append("]");
                }
                if (args != null) {
                    sb.append("args[").append(Arrays.toString(args)).append("]");
                } else {
                    sb.append("args[").append("NULL").append("]");
                }
                sb.append("}");

                String message = e.getMessage() + sb.toString();
                throw new IllegalArgumentException(message, e);
            } catch (Throwable e1) {
                throw e;
            }
        } catch (Throwable e) {
            if (MyProxy.isMethodDeclaredThrowable(method, e)) {
                throw e;
            } else {
                RuntimeException runtimeException = !TextUtils.isEmpty(e.getMessage()) ? new
                        RuntimeException(e.getMessage()) : new RuntimeException();
                runtimeException.initCause(e);
                throw runtimeException;
            }
        }
    }
}
