package me.ele.amigo.stub;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_INSTANCE;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_TASK;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_TOP;

public abstract class ActivityStub extends Activity {

    private static final List<Class<? extends ActivityStub>> SINGLE_TOP_STUBS = Arrays.asList(
            SingleTopStub1.class,
            SingleTopStub2.class,
            SingleTopStub3.class,
            SingleTopStub4.class,
            SingleTopStub5.class,
            SingleTopStub6.class,
            SingleTopStub7.class,
            SingleTopStub8.class);

    private static final List<Class<? extends ActivityStub>> SINGLE_TASK_STUBS = Arrays.asList(
            SingleTaskStub1.class,
            SingleTaskStub2.class,
            SingleTaskStub3.class,
            SingleTaskStub4.class,
            SingleTaskStub5.class,
            SingleTaskStub6.class,
            SingleTaskStub7.class,
            SingleTaskStub8.class);

    private static final List<Class<? extends ActivityStub>> SINGLE_INSTANCE_STUBS = Arrays.asList(
            SingleInstanceStub1.class,
            SingleInstanceStub1.class,
            SingleInstanceStub2.class,
            SingleInstanceStub3.class,
            SingleInstanceStub4.class,
            SingleInstanceStub5.class,
            SingleInstanceStub6.class,
            SingleInstanceStub7.class,
            SingleInstanceStub8.class);

    // [stub activity name : target activity instance]
    private static final Map<String, ActivityRecord> usedSingleTopStubs = new LinkedHashMap<>
            (SINGLE_TOP_STUBS.size(), 0.75f, true);
    private static final Map<String, ActivityRecord> usedSingleTaskStubs = new LinkedHashMap<>
            (SINGLE_TASK_STUBS.size(), 0.75f, true);
    private static final Map<String, ActivityRecord> usedSingleInstanceStubs = new
            LinkedHashMap<>(SINGLE_INSTANCE_STUBS.size(), 0.75f, true);

    private static Class findActivityStub(String componentName, Map<String, ActivityRecord>
            recordMap, List<Class<? extends ActivityStub>> stubs) {
        Log.d("stub", "findActivityStub for component[" + componentName + "]");
        for (Class stub : stubs) {
            ActivityRecord record = recordMap.get(stub.getName());
            if (record != null && record.activityClazzName.equals(componentName)) {
                Log.d("stub", "startStubActivity: (reopen) stubClazz = " + stub);
                return stub;
            }
        }

        for (Class stub : stubs) {
            if (!recordMap.containsKey(stub.getName())) {
                Log.d("stub", "startStubActivity: stubClazz = " + stub);
                return stub;
            }
        }
        return null;
    }

    private static void recycleEarliestUsedActivityStub(Map<String, ActivityRecord> recordMap,
                                                        int limit) {
        if (recordMap.size() < limit) {
            return;
        }

        Iterator<Map.Entry<String, ActivityRecord>> it = recordMap.entrySet().iterator();
        while (it.hasNext()) {
            ActivityRecord activityRecord = it.next().getValue();
            if (activityRecord.activity != null && !activityRecord.activity.isFinishing()) {
                activityRecord.activity.finish();
                it.remove();
                break;
            }
        }
    }

    private static Map<String, ActivityRecord> getActivityRecordMap(Class stubActivityClazz) {
        String clazzName = stubActivityClazz.getName();
        String subStr = clazzName.substring(0, clazzName.length() - 1);
        Map<String, ActivityRecord> recordMap = null;
        if (subStr.endsWith("SingleTopStub")) {
            recordMap = usedSingleTopStubs;
        } else if (subStr.endsWith("SingleTaskStub")) {
            recordMap = usedSingleTaskStubs;
        } else if (subStr.endsWith("SingleInstanceStub")) {
            recordMap = usedSingleInstanceStubs;
        }
        return recordMap;
    }

    public static Class selectActivityStubClazz(ActivityInfo activityInfo) {
        Class clazz;
        switch (activityInfo.launchMode) {
            case LAUNCH_SINGLE_TOP:
                clazz = findActivityStub(activityInfo.name, usedSingleTopStubs, SINGLE_TOP_STUBS);
                break;
            case LAUNCH_SINGLE_TASK:
                clazz = findActivityStub(activityInfo.name, usedSingleTaskStubs, SINGLE_TASK_STUBS);
                break;
            case LAUNCH_SINGLE_INSTANCE:
                clazz = findActivityStub(activityInfo.name, usedSingleInstanceStubs,
                        SINGLE_INSTANCE_STUBS);
                break;
            default:
                clazz = ActivityStub.StandardStub.class;
                break;
        }
        return clazz;
    }

    public static void recycleActivityStub(ActivityInfo activityInfo) {
        switch (activityInfo.launchMode) {
            case LAUNCH_SINGLE_TOP:
                recycleEarliestUsedActivityStub(usedSingleTopStubs, SINGLE_TOP_STUBS.size());
                break;
            case LAUNCH_SINGLE_TASK:
                recycleEarliestUsedActivityStub(usedSingleTaskStubs, SINGLE_TASK_STUBS.size());
                break;
            case LAUNCH_SINGLE_INSTANCE:
                recycleEarliestUsedActivityStub(usedSingleInstanceStubs, SINGLE_INSTANCE_STUBS
                        .size());
                break;
            default:
                break;
        }
    }

    public static void onActivityCreated(Class stubActivityClazz, Activity activity, String
            activityClazzName /*alternatively*/) {
        Map<String, ActivityRecord> recordMap = getActivityRecordMap(stubActivityClazz);
        if (recordMap != null)
            recordMap.put(stubActivityClazz.getName(), new ActivityRecord(activity,
                    activityClazzName));
    }

    public static void onActivityDestroyed(Class stubActivityClazz, Activity activity) {
        Map<String, ActivityRecord> recordMap = getActivityRecordMap(stubActivityClazz);
        if (recordMap == null) return;
        Iterator<Map.Entry<String, ActivityRecord>> it = recordMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ActivityRecord> entry = it.next();

            if (entry.getValue().activity == activity) {
                it.remove();
                break;
            }
        }
    }

    // single instance activity stubs
    public static class SingleInstanceStub1 extends ActivityStub {
    }

    public static class SingleInstanceStub2 extends ActivityStub {
    }

    public static class SingleInstanceStub3 extends ActivityStub {
    }

    public static class SingleInstanceStub4 extends ActivityStub {
    }

    public static class SingleInstanceStub5 extends ActivityStub {
    }

    public static class SingleInstanceStub6 extends ActivityStub {
    }

    public static class SingleInstanceStub7 extends ActivityStub {
    }

    public static class SingleInstanceStub8 extends ActivityStub {
    }

    // single task activity stubs
    public static class SingleTaskStub1 extends ActivityStub {
    }

    public static class SingleTaskStub2 extends ActivityStub {
    }

    public static class SingleTaskStub3 extends ActivityStub {
    }

    public static class SingleTaskStub4 extends ActivityStub {
    }

    public static class SingleTaskStub5 extends ActivityStub {
    }

    public static class SingleTaskStub6 extends ActivityStub {
    }

    public static class SingleTaskStub7 extends ActivityStub {
    }

    public static class SingleTaskStub8 extends ActivityStub {
    }

    // single top activity stubs
    public static class SingleTopStub1 extends ActivityStub {
    }

    public static class SingleTopStub2 extends ActivityStub {
    }

    public static class SingleTopStub3 extends ActivityStub {
    }

    public static class SingleTopStub4 extends ActivityStub {
    }

    public static class SingleTopStub5 extends ActivityStub {
    }

    public static class SingleTopStub6 extends ActivityStub {
    }

    public static class SingleTopStub7 extends ActivityStub {
    }

    public static class SingleTopStub8 extends ActivityStub {
    }

    public static class StandardStub extends ActivityStub {
    }

    private static class ActivityRecord {
        public Activity activity;
        public String activityClazzName;

        public ActivityRecord(Activity activity, String activityClazzName) {
            this.activity = activity;
            this.activityClazzName = activity != null ? activity.getClass().getName() :
                    activityClazzName;
        }
    }


}
