package me.ele.amigo.hook;

import android.content.Context;

public abstract class Hook {

    protected Context context;
    protected BaseHookHandle mHookHandles;

    protected Hook(Context context) {
        this.context = context;
        mHookHandles = createHookHandle();
    }

    protected abstract BaseHookHandle createHookHandle();


    protected abstract void onInstall(ClassLoader classLoader) throws Throwable;

    protected void onUnInstall(ClassLoader classLoader) throws Throwable {

    }
}

