package me.ele.amigo.hook;

import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Random;

import me.ele.amigo.AmigoInstrumentation;
import me.ele.amigo.reflect.MethodUtils;
import me.ele.amigo.stub.ServiceStub;
import me.ele.amigo.utils.component.ServiceFinder;

public class IActivityManagerHookHandle extends BaseHookHandle {

    private static final String TAG = IActivityManagerHookHandle.class.getSimpleName();

    public IActivityManagerHookHandle(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        sHookedMethodHandlers.put("startService", new startService(context));
        sHookedMethodHandlers.put("stopService", new stopService(context));
        sHookedMethodHandlers.put("stopServiceToken", new stopServiceToken(context));
        sHookedMethodHandlers.put("bindService", new bindService(context));
        sHookedMethodHandlers.put("unbindService", new unbindService(context));
        sHookedMethodHandlers.put("unbindFinished", new unbindFinished(context));
        sHookedMethodHandlers.put("peekService", new peekService(context));
    }

    private static class startService extends HookedMethodHandler {

        public startService(Context context) {
            super(context);
        }

        private ServiceInfo info = null;

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            info = replaceFirstServiceIntentOfArgs(args);
            return super.beforeInvoke(receiver, method, args);
        }

        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object invokeResult) throws Throwable {
            if (invokeResult instanceof ComponentName) {
                if (info != null) {
                    setFakedResult(new ComponentName(info.packageName, info.name));
                }
            }
            info = null;
            super.afterInvoke(receiver, method, args, invokeResult);
        }
    }

    private static class stopService extends HookedMethodHandler {

        public stopService(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            int index = 1;
            if (args != null && args.length > index && args[index] instanceof Intent) {
                Intent intent = (Intent) args[index];
                ServiceInfo info = ServiceFinder.resolveServiceInfo(context, intent);
                if (info != null) {
                    int re = ServiceManager.getDefault().stopService(context, intent);
                    setFakedResult(re);
                    return true;
                }
            }
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private static class stopServiceToken extends HookedMethodHandler {

        public stopServiceToken(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 2) {
                ComponentName componentName = (ComponentName) args[0];
                if (isComponentNameInNewApp(context, componentName)) {
                    IBinder token = (IBinder) args[1];
                    Integer startId = (Integer) args[2];
                    boolean re = ServiceManager.getDefault().stopServiceToken(context, componentName, token, startId);
                    setFakedResult(re);
                    return true;
                }
            }
            return super.beforeInvoke(receiver, method, args);
        }

    }

    private static class bindService extends HookedMethodHandler {

        public bindService(Context context) {
            super(context);
        }

        private ServiceInfo info = null;

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            info = replaceFirstServiceIntentOfArgs(args);
            int index = findIServiceConnectionIndex(method);
            if (info != null && index >= 0) {
                final Object oldIServiceConnection = args[index];
                args[index] = new MyIServiceConnection(info) {

                    public void connected(ComponentName name, IBinder service) {
                        try {
                            MethodUtils.invokeMethod(oldIServiceConnection, "connected", new ComponentName(mInfo.packageName, mInfo.name), service);
                        } catch (Exception e) {
                            Log.e(TAG, "invokeMethod connected", e);
                        }
                    }
                };
                ServiceManager.getDefault().addServiceIntent(oldIServiceConnection, (Intent) args[findFirstIntentIndexInArgs(args)]);
            }
            return super.beforeInvoke(receiver, method, args);
        }

        private int findIServiceConnectionIndex(Method method) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes != null && parameterTypes.length > 0) {
                for (int index = 0; index < parameterTypes.length; index++) {
                    if (parameterTypes[index] != null && TextUtils.equals(parameterTypes[index].getSimpleName(), "IServiceConnection")) {
                        return index;
                    }
                }
            }
            return -1;
        }

        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object invokeResult) throws Throwable {
            if (invokeResult instanceof ComponentName) {
                if (info != null) {
                    setFakedResult(new ComponentName(info.packageName, info.name));
                }
            }
            info = null;
            super.afterInvoke(receiver, method, args, invokeResult);
        }

        private abstract static class MyIServiceConnection extends IServiceConnection.Stub {
            protected final ServiceInfo mInfo;

            private MyIServiceConnection(ServiceInfo info) {
                mInfo = info;
            }
        }
    }

    private static class unbindService extends HookedMethodHandler {

        public unbindService(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            replaceFirstServiceIntentOfArgs(args);
            ServiceManager.getDefault().unbind(context, args[findIServiceConnectionIndex(method)]);
            return super.beforeInvoke(receiver, method, args);
        }

        private int findIServiceConnectionIndex(Method method) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes != null && parameterTypes.length > 0) {
                for (int index = 0; index < parameterTypes.length; index++) {
                    if (parameterTypes[index] != null && TextUtils.equals(parameterTypes[index].getSimpleName(), "IServiceConnection")) {
                        return index;
                    }
                }
            }
            return -1;
        }
    }

    private static class peekService extends HookedMethodHandler {

        public peekService(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            replaceFirstServiceIntentOfArgs(args);
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private static class unbindFinished extends HookedMethodHandler {

        public unbindFinished(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            replaceFirstServiceIntentOfArgs(args);
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private static ServiceInfo replaceFirstServiceIntentOfArgs(Object[] args) throws RemoteException {
        int intentOfArgIndex = findFirstIntentIndexInArgs(args);
        if (args != null && args.length > 1 && intentOfArgIndex >= 0) {
            Intent intent = (Intent) args[intentOfArgIndex];
            ServiceInfo serviceInfo = ServiceFinder.resolveServiceInfo(context, intent);
            if (serviceInfo != null) {
                ServiceInfo proxyService = selectProxyService(serviceInfo);
                if (proxyService != null) {
                    Intent newIntent = new Intent();
                    newIntent.setAction(proxyService.name + new Random().nextInt());
                    newIntent.setClassName(proxyService.packageName, proxyService.name);
                    newIntent.putExtra(AmigoInstrumentation.EXTRA_TARGET_INTENT, intent);
                    newIntent.setFlags(intent.getFlags());
                    args[intentOfArgIndex] = newIntent;
                    return proxyService;
                }
            }
        }
        return null;
    }

    private static ServiceInfo proxyServiceInfo;

    //// TODO: we may need to support new added services in multi-process app
    private static ServiceInfo selectProxyService(ServiceInfo serviceInfo) {
        if (proxyServiceInfo != null) {
            return proxyServiceInfo;
        }

        for (ServiceInfo info : ServiceFinder.getAppServices(context)) {
            if (info.name.equals(ServiceStub.class.getName())) {
                return proxyServiceInfo = info;
            }
        }
        return null;
    }

    private static int findFirstIntentIndexInArgs(Object[] args) {
        if (args != null && args.length > 0) {
            int i = 0;
            for (Object arg : args) {
                if (arg != null && arg instanceof Intent) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }


    private static boolean isComponentNameInNewApp(Context context, ComponentName componentName) {
        return ServiceFinder.resolveServiceInfo(context, new Intent().setComponent(componentName)) != null;
    }


}