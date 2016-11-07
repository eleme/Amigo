package me.ele.amigo.hook;

import android.content.Context;

import java.util.List;

import me.ele.amigo.compat.ActivityThreadCompat;
import me.ele.amigo.compat.IPackageManagerCompat;
import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.reflect.Utils;

import static me.ele.amigo.reflect.FieldUtils.readField;
import static me.ele.amigo.reflect.FieldUtils.writeField;

public class IPackageManagerHook extends ProxyHook {

    private Object original_sPackageManager = null;

    public IPackageManagerHook(Context context) {
        super(context);
    }

    @Override
    protected BaseHookHandle createHookHandle() {
        return new IPackageManagerHookHandle(context);
    }

    @Override
    protected void onInstall(ClassLoader classLoader) throws Throwable {
        Object oldObj = IPackageManagerCompat.instance();

        Class<?> clazz = oldObj.getClass();
        List<Class<?>> interfaces = Utils.getAllInterfaces(clazz);
        Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces.toArray(new
                Class[interfaces.size()]) : new Class[0];
        Object newObj = MyProxy.newProxyInstance(clazz.getClassLoader(), ifs, this);
        FieldUtils.writeStaticField(ActivityThreadCompat.clazz(), "sPackageManager", newObj);

        setProxyObj(oldObj);
        original_sPackageManager = oldObj;

        Object mBase = readField(context.getApplicationContext(), "mBase");
        Object mPackageManager = readField(mBase, "mPackageManager");
        writeField(mPackageManager, "mPM", newObj);
    }

    private void rollbackProxyPackageManager() {
        if (original_sPackageManager == null) {
            return;
        }

        try {
            FieldUtils.writeStaticField(ActivityThreadCompat.clazz(), "sPackageManager",
                    original_sPackageManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        original_sPackageManager = null;
    }

    @Override
    protected void onUnInstall(ClassLoader classLoader) throws Throwable {
        super.onUnInstall(classLoader);
        rollbackProxyPackageManager();
    }
}
