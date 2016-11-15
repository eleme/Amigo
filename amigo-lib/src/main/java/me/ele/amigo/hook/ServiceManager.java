package me.ele.amigo.hook;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Pair;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import me.ele.amigo.compat.ActivityThreadCompat;
import me.ele.amigo.compat.CompatibilityInfoCompat;
import me.ele.amigo.compat.QueuedWorkCompat;
import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.reflect.MethodUtils;
import me.ele.amigo.utils.component.ServiceFinder;

import static me.ele.amigo.AmigoInstrumentation.EXTRA_TARGET_INTENT;

public class ServiceManager {

    private Map<Object, Service> mTokenServices = new HashMap<>();
    private Map<String, Service> mNameService = new HashMap<>();
    private Map<Object, Integer> mServiceTaskIds = new HashMap<>();
    private Map<Object, Pair<Intent, Object>> mBindServiceRecord = new HashMap<>();

    private ServiceManager() {
    }

    private static ServiceManager sServiceManager;

    public static ServiceManager getDefault() {
        synchronized (ServiceManager.class) {
            if (sServiceManager == null) {
                sServiceManager = new ServiceManager();
            }
        }
        return sServiceManager;
    }

    public boolean hasServiceRunning() {
        return mTokenServices.size() > 0 && mNameService.size() > 0;
    }

    private Object findTokenByService(Service service) {
        for (Object s : mTokenServices.keySet()) {
            if (mTokenServices.get(s) == service) {
                return s;
            }
        }
        return null;
    }

    public void addBindServiceRecord(Object connection, Intent intent, Object proxyConnection) {
        mBindServiceRecord.put(connection, new Pair<>(intent, proxyConnection));
    }

    private ClassLoader getClassLoader(Context context) {
        return context.getClassLoader();
    }

    private void handleCreateServiceOne(ServiceInfo info) throws Exception {
        Object activityThread = ActivityThreadCompat.instance();
        IBinder fakeToken = new MyFakeIBinder();
        Class CreateServiceData = Class.forName(ActivityThreadCompat.clazz().getName() +
                "$CreateServiceData");
        Constructor init = CreateServiceData.getDeclaredConstructor();
        if (!init.isAccessible()) {
            init.setAccessible(true);
        }
        Object data = init.newInstance();

        FieldUtils.writeField(data, "token", fakeToken);
        FieldUtils.writeField(data, "info", info);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            FieldUtils.writeField(data, "compatInfo", CompatibilityInfoCompat
                    .DEFAULT_COMPATIBILITY_INFO());
        }

        MethodUtils.invokeMethod(activityThread, "handleCreateService", data);

        Object mService = FieldUtils.readField(activityThread, "mServices");
        Service service = (Service) MethodUtils.invokeMethod(mService, "get", fakeToken);
        MethodUtils.invokeMethod(mService, "remove", fakeToken);

        mTokenServices.put(fakeToken, service);
        mNameService.put(info.name, service);
    }

    private int handleOnStartOne(Context context, Intent intent, int flags) {
        ServiceInfo info = ServiceFinder.resolveNewServiceInfo(context, intent);
        if (info != null) {
            Service service = mNameService.get(info.name);
            if (service != null) {
                ClassLoader classLoader = getClassLoader(context);
                intent.setExtrasClassLoader(classLoader);
                Object token = findTokenByService(service);
                Integer integer = mServiceTaskIds.get(token);
                if (integer == null) {
                    integer = -1;
                }
                int startId = integer + 1;
                mServiceTaskIds.put(token, startId);
                int res = service.onStartCommand(intent, flags, startId);
                QueuedWorkCompat.waitToFinish();
                return res;
            }
        }
        return -1;
    }

    private void handleOnTaskRemovedOne(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ServiceInfo info = ServiceFinder.resolveNewServiceInfo(context, intent);
            if (info != null) {
                Service service = mNameService.get(info.name);
                if (service != null) {
                    ClassLoader classLoader = getClassLoader(context);
                    intent.setExtrasClassLoader(classLoader);
                    service.onTaskRemoved(intent);
                }
                QueuedWorkCompat.waitToFinish();
            }
        }
    }

    // TODO check if the service was ready to be destroyed ?
    private void handleOnDestroyOne(ServiceInfo targetInfo) {
        Service service = mNameService.get(targetInfo.name);
        if (service != null) {
            service.onDestroy();
            mNameService.remove(targetInfo.name);
            Object token = findTokenByService(service);
            mTokenServices.remove(token);
            mServiceTaskIds.remove(token);
        }
        QueuedWorkCompat.waitToFinish();
    }


    private IBinder handleOnBindOne(Context context, Intent intent) {
        ServiceInfo info = ServiceFinder.resolveNewServiceInfo(context, intent);
        if (info != null) {
            Service service = mNameService.get(info.name);
            if (service != null) {
                ClassLoader classLoader = getClassLoader(context);
                intent.setExtrasClassLoader(classLoader);
                return service.onBind(intent);
            }
        }
        return null;
    }

    private void handleOnRebindOne(Context context, Intent intent) {
        ServiceInfo info = ServiceFinder.resolveNewServiceInfo(context, intent);
        if (info != null) {
            Service service = mNameService.get(info.name);
            if (service != null) {
                ClassLoader classLoader = getClassLoader(context);
                intent.setExtrasClassLoader(classLoader);
                service.onRebind(intent);
            }
        }
    }

    private boolean handleOnUnbindOne(Context context, Intent intent) {
        ServiceInfo info = ServiceFinder.resolveNewServiceInfo(context, intent);
        if (info != null) {
            Service service = mNameService.get(info.name);
            if (service != null) {
                ClassLoader classLoader = getClassLoader(context);
                intent.setExtrasClassLoader(classLoader);
                return service.onUnbind(intent);
            }
        }
        return false;
    }


    public int onStart(Context context, Intent intent, int flags, int startId) throws Exception {
        Intent targetIntent = intent.getParcelableExtra(EXTRA_TARGET_INTENT);
        if (targetIntent != null) {
            ServiceInfo targetInfo = ServiceFinder.resolveNewServiceInfo(context, targetIntent);
            if (targetInfo != null) {
                Service service = mNameService.get(targetInfo.name);
                if (service == null) {
                    handleCreateServiceOne(targetInfo);
                }
                return handleOnStartOne(context, targetIntent, flags);
            }
        }
        return -1;
    }

    public void onTaskRemoved(Context context, Intent intent) throws Exception {
        Intent targetIntent = intent.getParcelableExtra(EXTRA_TARGET_INTENT);
        if (targetIntent != null) {
            ServiceInfo info = ServiceFinder.resolveNewServiceInfo(context, intent);
            Service service = mNameService.get(info.name);
            if (service == null) {
                handleCreateServiceOne(info);
            }
            handleOnTaskRemovedOne(context, targetIntent);
        }
    }

    public IBinder onBind(Context context, Intent intent) throws Exception {
        Intent targetIntent = intent.getParcelableExtra(EXTRA_TARGET_INTENT);
        if (targetIntent != null) {
            ServiceInfo info = ServiceFinder.resolveNewServiceInfo(context, targetIntent);
            Service service = mNameService.get(info.name);
            if (service == null) {
                handleCreateServiceOne(info);
            }
            return handleOnBindOne(context, targetIntent);
        }
        return null;
    }

    public void onRebind(Context context, Intent intent) throws Exception {
        Intent targetIntent = intent.getParcelableExtra(EXTRA_TARGET_INTENT);
        if (targetIntent != null) {
            ServiceInfo info = ServiceFinder.resolveNewServiceInfo(context, targetIntent);
            Service service = mNameService.get(info.name);
            if (service == null) {
                handleCreateServiceOne(info);
            }
            handleOnRebindOne(context, targetIntent);
        }
    }

    public boolean onUnbind(Context context, Intent intent) {
        Intent targetIntent = intent.getParcelableExtra(EXTRA_TARGET_INTENT);
        if (targetIntent != null) {
            ServiceInfo info = ServiceFinder.resolveNewServiceInfo(context, targetIntent);
            Service service = mNameService.get(info.name);
            if (service != null) {
                return handleOnUnbindOne(context, targetIntent);
            }
        }
        return false;
    }

    public boolean unbind(Context context, Object connection) {
        if (!mBindServiceRecord.containsKey(connection)) {
            return false;
        }

        Intent intent = mBindServiceRecord.remove(connection).first;
        if (intent != null)
            return onUnbind(context, intent);
        return false;
    }

    public Object getProxyConnection(Object connection) {
        Pair<Intent, Object> value = mBindServiceRecord.get(connection);
        return value != null ? value.second : null;
    }

    public int stopService(Context context, Intent intent) {
        ServiceInfo targetInfo = ServiceFinder.resolveNewServiceInfo(context, intent);
        if (targetInfo != null) {
            handleOnUnbindOne(context, intent);
            handleOnDestroyOne(targetInfo);
            return 1;
        }
        return 0;
    }

    public boolean stopServiceToken(Context context, ComponentName cn, IBinder token, int
            startId) {
        Service service = mTokenServices.get(token);
        if (service != null) {
            Integer lastId = mServiceTaskIds.get(token);
            if (lastId == null) {
                return false;
            }
            if (startId != lastId) {
                return false;
            }
            Intent intent = new Intent();
            intent.setComponent(cn);
            ServiceInfo info = ServiceFinder.resolveNewServiceInfo(context, intent);
            if (info != null) {
                handleOnUnbindOne(context, intent);
                handleOnDestroyOne(info);
                return true;
            }
        }
        return false;
    }

    public void onDestroy() {
        for (Service service : mTokenServices.values()) {
            service.onDestroy();
        }
        mTokenServices.clear();
        mServiceTaskIds.clear();
        mNameService.clear();
        QueuedWorkCompat.waitToFinish();
    }

}
