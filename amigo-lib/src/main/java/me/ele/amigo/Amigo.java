package me.ele.amigo;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;

import me.ele.amigo.exceptions.LoadPatchApkException;
import me.ele.amigo.hook.HookFactory;
import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.release.ApkReleaseActivity;
import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.ProcessUtils;
import me.ele.amigo.utils.component.ActivityFinder;
import me.ele.amigo.utils.component.ContentProviderFinder;
import me.ele.amigo.utils.component.ReceiverFinder;

import static android.content.pm.PackageManager.GET_META_DATA;
import static me.ele.amigo.compat.ActivityThreadCompat.instance;
import static me.ele.amigo.reflect.FieldUtils.readField;
import static me.ele.amigo.reflect.FieldUtils.readStaticField;
import static me.ele.amigo.reflect.FieldUtils.writeField;
import static me.ele.amigo.reflect.MethodUtils.invokeMethod;

public class Amigo extends Application {
    private static final String TAG = Amigo.class.getSimpleName();

    private static LoadPatchError loadPatchError;


    private Instrumentation originalInstrumentation = null;
    private Object originalCallback = null;

    private AmigoDirs amigoDirs;
    private PatchApks patchApks;
    private int revertBitFlag = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            init();
            String workingChecksum = PatchInfoUtil.getWorkingChecksum(this);
            Log.e(TAG, "#onCreate working checksum: " + workingChecksum);
            if (PatchChecker.checkUpgrade(this)) {
                Log.d(TAG, "Host app has upgrade");
                PatchCleaner.clearPatchIfInMainProcess(this);
                runOriginalApplication();
                return;
            }

            if (TextUtils.isEmpty(workingChecksum)
                    || !patchApks.exists(workingChecksum)) {
                Log.d(TAG, "#onCreate Patch apk doesn't exists");
                PatchCleaner.clearPatchIfInMainProcess(this);
                runOriginalApplication();
                return;
            }

            // ensure load dex process always run host apk not patch apk
            if (ProcessUtils.isLoadDexProcess(this)) {
                Log.e(TAG, "#onCreate load dex process");
                runOriginalApplication();
                return;
            }

            if (!ProcessUtils.isMainProcess(this) && isPatchApkFirstRun(workingChecksum)) {
                Log.e(TAG, "#onCreate None main process and patch apk is not released yet");
                runOriginalApplication();
                return;
            }

            // only release loaded apk in the main process
            runPatchApk(workingChecksum);
        } catch (LoadPatchApkException e) {
            e.printStackTrace();
            loadPatchError = LoadPatchError.record(LoadPatchError.LOAD_ERR, e);
            try {
                runOriginalApplication();
            } catch (Throwable e2) {
                throw new RuntimeException(e2);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * WARNING: do not modify this method's name, used for hotfix itself
     */
    private void init() {
        amigoDirs = AmigoDirs.getInstance(this);
        patchApks = PatchApks.getInstance(this);
    }

    private void runPatchApk(String checksum) throws LoadPatchApkException {
        try {
            if (isPatchApkFirstRun(checksum) || !isOptedDexExists(checksum)) {
                PatchInfoUtil.updateDexFileOptStatus(this, checksum, false);
                releasePatchApk(checksum);
            } else {
                PatchChecker.checkDexAndSo(this, checksum);
            }

            AmigoClassLoader classLoader = AmigoClassLoader.newInstance(this, checksum);
            Object newAmigoInstance = null;
            try {
                Class<?> clazz = classLoader.loadClass(Amigo.class.getName());
                newAmigoInstance = clazz.newInstance();
                invokeMethod(newAmigoInstance, "attach", getBaseContext());
                invokeMethod(newAmigoInstance, "init");
                invokeMethod(newAmigoInstance, "installAndHook", checksum, classLoader);
                Log.i(TAG, "installAndHook successfully with patch");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, String.format("installAndHook fails with patch %s", e));
                if (newAmigoInstance != null) {
                    try {
                        invokeMethod(newAmigoInstance, "revertAll");
                        Log.i(TAG, "revertAll newAmigoInstance successfully");
                    } catch (Exception ee) {
                        ee.printStackTrace();
                        Log.i(TAG, "revertAll newAmigoInstance fails");
                    }
                }
                installAndHook(checksum, classLoader);
                Log.i(TAG, "installAndHook successfully with host");
            }
        } catch (Exception e) {
            throw new LoadPatchApkException(e);
        }
    }

    /**
     * WARNING: do not modify this method's name, used for hotfix itself
     * WARNING: all new extra operation should be done here
     */
    private void installAndHook(String checksum, ClassLoader classLoader) throws Exception {
        setAPKClassLoader(classLoader);
        revertBitFlag |= 1;
        setApkResource(checksum);

        boolean gotNewActivity = ActivityFinder.newActivityExistsInPatch(this);
        if (gotNewActivity) {
            setApkInstrumentation();
            revertBitFlag |= 1 << 1;
            setApkHandlerCallback();
            revertBitFlag |= 1 << 2;
        } else {
            Log.d(TAG, "installAndHook: there is no any new activity, skip hooking " +
                    "instrumentation & mH's callback");
        }

        installHookFactory();

        runPatchedApplication(checksum);

        dynamicRegisterNewReceivers();
        installPatchContentProviders();

        PatchInfoUtil.setWorkingChecksum(this, checksum); // not necessary
        PatchCleaner.clearOldPatches(this, checksum);
    }

    private void setApkResource(String checksum) throws Exception {
        PatchResourceLoader.loadPatchResources(this, checksum);
        Log.i(TAG, "hook Resources success");
    }

    private void setApkInstrumentation() throws Exception {
        Instrumentation oldIns = (Instrumentation) readField(instance(), "mInstrumentation", true);
        AmigoInstrumentation ins = new AmigoInstrumentation(oldIns);
        writeField(instance(), "mInstrumentation", ins, true);
        originalInstrumentation = ins;
        Log.i(TAG, "hook instrumentation success");
    }

    private void rollbackApkInstrumentation() {
        try {
            writeField(instance(), "mInstrumentation", originalInstrumentation, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setApkHandlerCallback() throws Exception {
        originalCallback = replaceHandlerCallback(this);
        Log.i(TAG, "hook handler success");
    }

    private static Handler.Callback replaceHandlerCallback(Context context) throws Exception {
        Handler handler = (Handler) readField(instance(), "mH", true);
        Object callback = readField(handler, "mCallback", true);
        if (callback != null && callback.getClass().getName().equals(AmigoCallback.class.getName
                ())) {
            return null;
        }
        AmigoCallback value = new AmigoCallback(context, (Handler.Callback) callback);
        writeField(handler, "mCallback", value);
        return value;
    }

    private void dynamicRegisterNewReceivers() {
        ReceiverFinder.registerNewReceivers(getApplicationContext(), getClassLoader());
        Log.i(TAG, "dynamic register new receivers done");
    }

    private void installHookFactory() {
        HookFactory.install(this, getClassLoader());
        Log.i(TAG, "installHookFactory success");
    }

    private void installPatchContentProviders() {
        ContentProviderFinder.installPatchContentProviders(getApplicationContext());
        Log.i(TAG, "installPatchContentProviders done");
    }

    private void rollbackApkHandlerCallback() {
        try {
            Handler handler = (Handler) readField(instance(), "mH", true);
            writeField(handler, "mCallback", originalCallback);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void releasePatchApk(String checksum) throws Exception {
        //clear previous working dir
        PatchCleaner.clearWithoutPatchApk(this, checksum);
        //start a new process to handle time-tense operation
        ApplicationInfo appInfo =
                getPackageManager().getApplicationInfo(getPackageName(), GET_META_DATA);
        String layoutName = appInfo.metaData.getString("amigo_layout");
        String themeName = appInfo.metaData.getString("amigo_theme");
        int layoutId = 0;
        int themeId = 0;
        if (!TextUtils.isEmpty(layoutName)) {
            layoutId = getResources().getIdentifier(layoutName, "layout", getPackageName());
        }
        if (!TextUtils.isEmpty(themeName)) {
            themeId = getResources().getIdentifier(themeName, "style", getPackageName());
        }
        Log.e(TAG, String.format("layoutName-->%s, themeName-->%s", layoutName, themeName));
        Log.e(TAG, String.format("layoutId-->%d, themeId-->%d", layoutId, themeId));

        releaseDex(checksum, layoutId, themeId);
        Log.e(TAG, "release apk once");
    }

    private static final int SLEEP_DURATION = 200;

    private boolean isDexOptDone(String checksum) {
        return PatchInfoUtil.isDexFileOptimized(this, checksum);
    }

    /**
     * start a new process to release and optimize dex files
     */
    private void waitDexOptDone(String checksum, int layoutId, int themeId) {
        ApkReleaseActivity.launch(this, checksum, layoutId, themeId);
        while (!isDexOptDone(checksum)) {
            try {
                Thread.sleep(SLEEP_DURATION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void releaseDex(String checksum, int layoutId, int themeId) {
        if (!ProcessUtils.isLoadDexProcess(this)) {
            if (!isDexOptDone(checksum)) {
                waitDexOptDone(checksum, layoutId, themeId);
            }
        }
    }

    private boolean isPatchApkFirstRun(String checksum) {
        return !PatchInfoUtil.getWorkingChecksum(this).equals(checksum);
    }

    private boolean isOptedDexExists(String checksum) {
        return amigoDirs.dexOptDir(checksum).listFiles() != null
                && amigoDirs.dexOptDir(checksum).listFiles().length > 0;
    }

    private void runOriginalApplication() throws Exception {
        revertAll();
        Class acd = getClassLoader().loadClass("me.ele.amigo.acd");
        String applicationName = (String) readStaticField(acd, "n");
        Application application =
                (Application) getClassLoader().loadClass(applicationName).newInstance();
        invokeMethod(application, "attach", getBaseContext());
        setAPKApplication(application);
        application.onCreate();
    }

    /**
     * WARNING: do not modify this method's name, used for hotfix itself
     */
    private void revertAll() throws Exception {
        if ((revertBitFlag & 1) != 0) {
            setAPKClassLoader(Amigo.class.getClassLoader());
        }
        if ((revertBitFlag & (1 << 1)) != 0) {
            rollbackApkInstrumentation();
        }
        if ((revertBitFlag & (1 << 2)) != 0) {
            rollbackApkHandlerCallback();
        }
        ReceiverFinder.unregisterNewReceivers(this);
        HookFactory.uninstallAllHooks(getClassLoader());
        PatchResourceLoader.revertLoadPatchResources();
        // TODO unregister providers
    }

    private void runPatchedApplication(String patchApkCheckSum) throws Exception {
        String appName = getPatchApplicationName(patchApkCheckSum);
        Application application = (Application) getClassLoader().loadClass(appName).newInstance();
        FieldUtils.writeField(getBaseContext(), "mOuterContext", application);
        invokeMethod(application, "attach", getBaseContext());
        setAPKApplication(application);
        application.onCreate();
    }

    private String getPatchApplicationName(String patchApkCheckSum) throws Exception {
        String applicationName = null;
        try {
            Class acd = getClassLoader().loadClass("me.ele.amigo.acd");
            if (acd != null && acd.getClassLoader() == getClassLoader()) {
                applicationName = (String) readStaticField(acd, "n");
            }
        } catch (ClassNotFoundException classNotFoundExp) {
            Log.d(TAG, "runPatchedApplication: " + classNotFoundExp);
        }
        if (applicationName == null) {
            applicationName = getPackageManager().getPackageArchiveInfo(
                    PatchApks.getInstance(this).patchPath(patchApkCheckSum), 0).applicationInfo
                    .className;
        }
        if (applicationName == null) {
            throw new RuntimeException(
                    "Amigo#runPatchedApplication : can't resolve original application name");
        }
        return applicationName;
    }

    private void setAPKClassLoader(ClassLoader classLoader) throws Exception {
        writeField(getLoadedApk(), "mClassLoader", classLoader);
    }

    private void setAPKApplication(Application application) throws Exception {
        Object apk = getLoadedApk();
        writeField(apk, "mApplication", application);
        writeField(instance(), "mInitialApplication", application);
    }

    private static Object getLoadedApk() throws Exception {
        Map<String, WeakReference<Object>> mPackages =
                (Map<String, WeakReference<Object>>) readField(instance(), "mPackages", true);
        for (String s : mPackages.keySet()) {
            WeakReference wr = mPackages.get(s);
            if (wr != null && wr.get() != null) {
                return wr.get();
            }
        }
        return null;
    }

    public static void work(Context context, File patchFile) {
        work(context, patchFile, true);
    }

    public static void workWithoutCheckingSignature(Context context, File patchFile) {
        work(context, patchFile, false);
    }

    private static void work(Context context, File patchFile, boolean checkSignature) {
        String patchChecksum = PatchChecker.checkPatchAndCopy(context, patchFile, checkSignature);
        if (PatchInfoUtil.setWorkingChecksum(context, patchChecksum)) {
            AmigoService.restartMainProcess(context);
        }
    }

    public static void workLater(Context context, File patchFile) {
        workLater(context, patchFile, true, null);
    }

    public static void workLater(Context context, File patchFile, WorkLaterCallback callback) {
        workLater(context, patchFile, true, callback);
    }

    public static void workLaterWithoutCheckingSignature(Context context, File patchFile) {
        workLater(context, patchFile, false, null);
    }

    public interface WorkLaterCallback {
        void onPatchApkReleased();
    }

    private static void workLater(Context context, File patchFile, boolean checkSignature,
                                  WorkLaterCallback callback) {
        String patchChecksum = PatchChecker.checkPatchAndCopy(context, patchFile, checkSignature);
        if (patchChecksum == null) {
            Log.e(TAG, "workLater: empty checksum");
            return;
        }

        if (callback != null) {
            AmigoService.startReleaseDex(context, patchChecksum, callback);
        } else {
            AmigoService.startReleaseDex(context, patchChecksum);
        }
    }

    public static boolean hasWorked() {
        ClassLoader classLoader = null;
        try {
            classLoader = (ClassLoader) readField(getLoadedApk(), "mClassLoader");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return classLoader != null
                && classLoader.getClass().getName().equals(AmigoClassLoader.class.getName());
    }

    public static PackageInfo getHostPackageInfo(Context context, int flags) {
        String hostApkPath = context.getApplicationInfo().sourceDir;
        return CommonUtils.getPackageInfo(context, new File(hostApkPath), flags);
    }

    public static String getWorkingPatchApkChecksum(Context ctx) {
        if (!hasWorked()) return "";
        return PatchInfoUtil.getWorkingChecksum(ctx);
    }

    public static void clear(Context context) {
        PatchInfoUtil.setWorkingChecksum(context, "");
    }

    public static LoadPatchError getLoadPatchError() {
        return loadPatchError;
    }

    /**
     * this is for some extreme condition,
     * like some safety app or malicious software replaces Amigo's hook
     *
     * @param context
     * @return
     */
    public static boolean rollAmigoBack(Context context) {
        return checkAndSetAmigoCallback(context) || checkAndSetAmigoClassLoader(context);
    }

    private static boolean checkAndSetAmigoCallback(Context context) {
        try {
            Handler handler = (Handler) readField(instance(), "mH", true);
            Object callback = readField(handler, "mCallback", true);
            if (callback != null) {
                Field[] fields = callback.getClass().getDeclaredFields();
                for (Field field : fields) {
                    Object obj = readField(field, callback, true);
                    if (obj == null || !obj.getClass().getName().equals(AmigoCallback.class.getName())) {
                        continue;
                    }
                    writeField(field, callback, null, true);
                }
            }
            return replaceHandlerCallback(context.getApplicationContext()) != null;
        } catch (Exception e) {
            //ignore
        }
        return false;
    }

    private static boolean checkAndSetAmigoClassLoader(Context context) {
        try {
            String classloaderName = context.getClassLoader().getClass().getName();
            if (classloaderName.equals(AmigoClassLoader.class.getName())) {
                return false;
            }
            Context app = context.getApplicationContext();
            ClassLoader classLoader = app.getClass().getClassLoader();
            writeField(getLoadedApk(), "mClassLoader", classLoader);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
