package me.ele.amigo.hook;

import android.content.Context;
import android.util.AndroidRuntimeException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import me.ele.amigo.compat.ActivityManagerNativeCompat;
import me.ele.amigo.compat.IActivityManagerCompat;
import me.ele.amigo.compat.SingletonCompat;
import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.reflect.Utils;
import me.ele.amigo.utils.Log;

public class IActivityManagerHook extends ProxyHook {

    private static final String TAG = IActivityManagerHook.class.getSimpleName();

    public IActivityManagerHook(Context context) {
        super(context);
    }

    @Override
    public BaseHookHandle createHookHandle() {
        return new IActivityManagerHookHandle(context);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            String msg = String.format("msg[%s],args[%s]", e.getMessage(), Arrays.toString(args));
            SecurityException e1 = new SecurityException(msg);
            e1.initCause(e);
            throw e1;
        }
    }

    @Override
    public void onInstall(ClassLoader classLoader) throws Throwable {
        Class cls = ActivityManagerNativeCompat.Class();
        Object gDefault = FieldUtils.readStaticField(cls, "gDefault");
        if (gDefault == null) {
            ActivityManagerNativeCompat.getDefault();
            gDefault = FieldUtils.readStaticField(cls, "gDefault");
        }

        if (IActivityManagerCompat.isIActivityManager(gDefault)) {
            setProxyObj(gDefault);
            Class<?> objClass = proxyObj.getClass();
            List<Class<?>> interfaces = Utils.getAllInterfaces(objClass);
            Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces.toArray(new Class[interfaces.size()]) : new Class[0];
            Object proxyActivityManager = MyProxy.newProxyInstance(objClass.getClassLoader(), ifs, this);
            FieldUtils.writeStaticField(cls, "gDefault", proxyActivityManager);
            Log.i(TAG, "Install ActivityManager Hook 1 old=%s,new=%s", proxyObj, proxyActivityManager);
        } else if (SingletonCompat.isSingleton(gDefault)) {
            Object mInstance = FieldUtils.readField(gDefault, "mInstance");
            if (mInstance == null) {
                SingletonCompat.get(gDefault);
                mInstance = FieldUtils.readField(gDefault, "mInstance");
            }
            setProxyObj(mInstance);
            List<Class<?>> interfaces = Utils.getAllInterfaces(proxyObj.getClass());
            Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces.toArray(new Class[interfaces.size()]) : new Class[0];
            final Object object = MyProxy.newProxyInstance(proxyObj.getClass().getClassLoader(), ifs, IActivityManagerHook.this);
            Object iam1 = ActivityManagerNativeCompat.getDefault();

            FieldUtils.writeField(gDefault, "mInstance", object);

            FieldUtils.writeStaticField(cls, "gDefault", new android.util.Singleton<Object>() {
                @Override
                protected Object create() {
                    Log.e(TAG, "Install ActivityManager 3 Hook  old=%s,new=%s", proxyObj, object);
                    return object;
                }
            });

            Log.i(TAG, "Install ActivityManager Hook 2 old=%s,new=%s", proxyObj.toString(), object);
            Object iam2 = ActivityManagerNativeCompat.getDefault();
            if (iam1 == iam2) {
                FieldUtils.writeField(gDefault, "mInstance", object);
            }
        } else {
            throw new AndroidRuntimeException("Can not install IActivityManagerNative hook");
        }
    }
}
