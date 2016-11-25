package me.ele.amigo.hook;

import android.app.Activity;
import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.TypedArray;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;

import me.ele.amigo.AmigoInstrumentation;
import me.ele.amigo.compat.ActivityThreadCompat;
import me.ele.amigo.compat.RCompat;
import me.ele.amigo.reflect.MethodUtils;
import me.ele.amigo.stub.ServiceStub;
import me.ele.amigo.utils.component.ServiceFinder;

import static android.R.attr.activityCloseEnterAnimation;
import static android.R.attr.activityCloseExitAnimation;
import static android.R.attr.activityOpenEnterAnimation;
import static android.R.attr.activityOpenExitAnimation;
import static me.ele.amigo.reflect.FieldUtils.readField;

public class IActivityManagerHookHandle extends BaseHookHandle {

    private static final String TAG = IActivityManagerHookHandle.class.getSimpleName();
    private static ServiceInfo proxyServiceInfo;

    public IActivityManagerHookHandle(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        hookedMethodHandlers.put("startService", new startService(context));
        hookedMethodHandlers.put("stopService", new stopService(context));
        hookedMethodHandlers.put("stopServiceToken", new stopServiceToken(context));
        hookedMethodHandlers.put("bindService", new bindService(context));
        hookedMethodHandlers.put("unbindService", new unbindService(context));
        hookedMethodHandlers.put("unbindFinished", new unbindFinished(context));
        hookedMethodHandlers.put("peekService", new peekService(context));
        hookedMethodHandlers.put("startActivity", new startActivity(context));
        hookedMethodHandlers.put("startActivityAsUser", new startActivityAsUser(context));
        hookedMethodHandlers.put("startActivityAsCaller", new startActivityAsCaller(context));
        hookedMethodHandlers.put("finishActivity", new finishActivity(context));
        hookedMethodHandlers.put("overridePendingTransition", new overridePendingTransition
                (context));
    }

    private static class startService extends HookedMethodHandler {

        private ServiceInfo info = null;

        public startService(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws
                Throwable {
            info = replaceFirstServiceIntentOfArgs(args);
            return super.beforeInvoke(receiver, method, args);
        }

        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object
                invokeResult) throws Throwable {
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
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws
                Throwable {
            int index = 1;
            if (args != null && args.length > index && args[index] instanceof Intent) {
                Intent intent = (Intent) args[index];
                int re = ServiceManager.getDefault().stopService(context, intent);
                if (re == 1) {
                    setFakedResult(1);
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
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws
                Throwable {
            if (args != null && args.length > 2) {
                ComponentName componentName = (ComponentName) args[0];
                if (isComponentNameInNewApp(context, componentName)) {
                    IBinder token = (IBinder) args[1];
                    Integer startId = (Integer) args[2];
                    boolean re = ServiceManager.getDefault().stopServiceToken(context,
                            componentName, token, startId);
                    setFakedResult(re);
                    return true;
                }
            }
            return super.beforeInvoke(receiver, method, args);
        }

    }

    private static class bindService extends HookedMethodHandler {

        private ServiceInfo info = null;

        public bindService(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws
                Throwable {
            info = replaceFirstServiceIntentOfArgs(args);
            int index = findIServiceConnectionIndex(method);
            if (info != null && index >= 0) {
                final Object oldIServiceConnection = args[index];
                MyIServiceConnection proxyConnection = new MyIServiceConnection(info) {
                    public void connected(ComponentName name, IBinder service) {
                        try {
                            MethodUtils.invokeMethod(oldIServiceConnection, "connected", new
                                    ComponentName(mInfo.packageName, mInfo.name), service);
                        } catch (Exception e) {
                            Log.e(TAG, "invokeMethod connected", e);
                        }
                    }
                };
                args[index] = proxyConnection;
                ServiceManager.getDefault().addBindServiceRecord(oldIServiceConnection, (Intent)
                        args[findFirstIntentIndexInArgs(args)], proxyConnection);
            }
            return super.beforeInvoke(receiver, method, args);
        }

        private int findIServiceConnectionIndex(Method method) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes != null && parameterTypes.length > 0) {
                for (int index = 0; index < parameterTypes.length; index++) {
                    if (parameterTypes[index] != null && TextUtils.equals(parameterTypes[index]
                            .getSimpleName(), "IServiceConnection")) {
                        return index;
                    }
                }
            }
            return -1;
        }

        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object
                invokeResult) throws Throwable {
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
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws
                Throwable {
            int index = findIServiceConnectionIndex(method);
            if (index != -1) {
                Object connection = args[index];
                Object proxyConnection = ServiceManager.getDefault().getProxyConnection(connection);
                setFakedResult(ServiceManager.getDefault().unbind(context, connection));
                if (proxyConnection != null) {
                    args[index] = proxyConnection;
                }
            }
            return super.beforeInvoke(receiver, method, args);
        }

        private int findIServiceConnectionIndex(Method method) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes != null && parameterTypes.length > 0) {
                for (int index = 0; index < parameterTypes.length; index++) {
                    if (parameterTypes[index] != null && TextUtils.equals(parameterTypes[index]
                            .getSimpleName(), "IServiceConnection")) {
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
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws
                Throwable {
            replaceFirstServiceIntentOfArgs(args);
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private static class unbindFinished extends HookedMethodHandler {

        public unbindFinished(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws
                Throwable {
            replaceFirstServiceIntentOfArgs(args);
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private static ServiceInfo replaceFirstServiceIntentOfArgs(Object[] args) throws
            RemoteException {
        int intentOfArgIndex = findFirstIntentIndexInArgs(args);
        if (args != null && args.length > 1 && intentOfArgIndex >= 0) {
            Intent intent = (Intent) args[intentOfArgIndex];
            ServiceInfo serviceInfo = ServiceFinder.resolveNewServiceInfo(context, intent);
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

    //// TODO: we may need to support new added services in multi-process app
    private static ServiceInfo selectProxyService(ServiceInfo serviceInfo) {
        if (proxyServiceInfo == null) {
            try {
                proxyServiceInfo = context.getPackageManager().getServiceInfo(new ComponentName
                        (context.getPackageName(), ServiceStub.class.getName()), 0);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return proxyServiceInfo;
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
        return ServiceFinder.resolveNewServiceInfo(context, new Intent().setComponent(componentName)
        ) != null;
    }


    private class startActivity extends HookedMethodHandler {
        public startActivity(Context context) {
            super(context);
        }

        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object
                invokeResult) throws Throwable {
            overridePendingTransition(args, activityOpenEnterAnimation, activityOpenExitAnimation);
            super.afterInvoke(receiver, method, args, invokeResult);
        }
    }

    private class startActivityAsUser extends HookedMethodHandler {
        public startActivityAsUser(Context context) {
            super(context);
        }

        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object
                invokeResult) throws Throwable {
            overridePendingTransition(args, activityOpenEnterAnimation, activityOpenExitAnimation);
            super.afterInvoke(receiver, method, args, invokeResult);
        }
    }

    private class startActivityAsCaller extends HookedMethodHandler {
        public startActivityAsCaller(Context context) {
            super(context);
        }

        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object
                invokeResult) throws Throwable {
            overridePendingTransition(args, activityOpenEnterAnimation, activityOpenExitAnimation);
            super.afterInvoke(receiver, method, args, invokeResult);
        }
    }


    private void overridePendingTransition(Object[] args, int enterAnimFlag, int exitAnimFlag) {
        try {
            IBinder token = getIBinderToken(args);
            if (token == null) {
                return;
            }
            Activity activity = (Activity) MethodUtils.invokeMethod(ActivityThreadCompat.instance
                    (), "getActivity", token);
            int windowAnimations = activity.getWindow().getAttributes().windowAnimations;
            int[] attrs = {enterAnimFlag, exitAnimFlag};
            TypedArray ta = activity.obtainStyledAttributes(windowAnimations, attrs);
            int enterAnimation = ta.getResourceId(0, 0);
            int exitAnimation = ta.getResourceId(1, 0);
            ta.recycle();
            activity.overridePendingTransition(enterAnimation, exitAnimation);
        } catch (Exception e) {
            //ignore
        }
    }

    public IBinder getIBinderToken(Object[] args) {
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg != null && arg instanceof IBinder) {
                    return (IBinder) arg;
                }
            }
        }
        return null;
    }

    private class finishActivity extends HookedMethodHandler {

        public finishActivity(Context context) {
            super(context);
        }

        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object
                invokeResult) throws Throwable {
            try {
                Map mActivities = (Map) readField(ActivityThreadCompat.instance(), "mActivities",
                        true);
                if (mActivities != null && mActivities.size() == 1) {
                    return;
                }
            } catch (Exception e) {
                //ignore
            }
            overridePendingTransition(args, activityCloseEnterAnimation,
                    activityCloseExitAnimation);
            super.afterInvoke(receiver, method, args, invokeResult);
        }
    }

    private class overridePendingTransition extends HookedMethodHandler {
        public overridePendingTransition(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws
                Throwable {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if ((arg.getClass() == int.class || arg.getClass() == Integer.class)) {
                    int anim = RCompat.getHostIdentifier(context, (int) arg);
                    args[i] = anim;
                }
            }
            return super.beforeInvoke(receiver, method, args);
        }
    }
}