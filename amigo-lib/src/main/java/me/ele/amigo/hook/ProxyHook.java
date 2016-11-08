package me.ele.amigo.hook;

import android.content.Context;
import android.text.TextUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import me.ele.amigo.utils.Log;

public abstract class ProxyHook extends Hook implements InvocationHandler {

    protected Object proxyObj;

    public ProxyHook(Context context) {
        super(context);
    }

    public void setProxyObj(Object proxyObj) {
        this.proxyObj = proxyObj;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long t1 = System.currentTimeMillis();
        try {
            HookedMethodHandler hookedMethodHandler = mHookHandles.getHookedMethodHandler(method);
            if (hookedMethodHandler != null) {
                Object hookResult = hookedMethodHandler.doHookInner(proxyObj, method, args);
                long t2 = System.currentTimeMillis();
                Log.d("ProxyHook", "invoking (%s) \ncost %s ms totally", method, (t2 - t1));
                return hookResult;
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
            final StringBuilder errorMsg = new StringBuilder(e.getMessage());
            errorMsg.append(" || Amigo{");
            if (method != null) {
                errorMsg.append("method[").append(method.toString()).append("]");
            } else {
                errorMsg.append("method[").append("NULL").append("]");
            }
            if (args != null) {
                errorMsg.append("args[").append(Arrays.toString(args)).append("]");
            } else {
                errorMsg.append("args[").append("NULL").append("]");
            }
            errorMsg.append("}");
            throw new IllegalArgumentException(errorMsg.toString(), e);
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
