package me.ele.amigo.hook;

import android.content.Context;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseHookHandle {

    protected static Context context;

    protected Map<String, HookedMethodHandler> sHookedMethodHandlers = new HashMap<>();

    public BaseHookHandle(Context context) {
        this.context = context;
        init();
    }

    protected abstract void init();

    public HookedMethodHandler getHookedMethodHandler(Method method) {
        if (method != null) {
            return sHookedMethodHandlers.get(method.getName());
        } else {
            return null;
        }
    }
}
