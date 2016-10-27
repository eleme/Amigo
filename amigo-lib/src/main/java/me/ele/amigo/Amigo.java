package me.ele.amigo;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;

import me.ele.amigo.exceptions.LoadPatchApkException;
import me.ele.amigo.hook.HookFactory;
import me.ele.amigo.release.ApkReleaser;
import me.ele.amigo.utils.CommonUtils;
import me.ele.amigo.utils.ProcessUtils;
import me.ele.amigo.utils.component.ContentProviderFinder;
import me.ele.amigo.utils.component.ReceiverFinder;

import static android.content.pm.PackageManager.GET_META_DATA;
import static me.ele.amigo.compat.ActivityThreadCompat.instance;
import static me.ele.amigo.reflect.FieldUtils.readField;
import static me.ele.amigo.reflect.FieldUtils.readStaticField;
import static me.ele.amigo.reflect.FieldUtils.writeField;
import static me.ele.amigo.reflect.MethodUtils.getMatchedMethod;

public class Amigo extends Application {
    private static final String TAG = Amigo.class.getSimpleName();

    public static final String SP_NAME = "Amigo";
    public static final String WORKING_PATCH_APK_CHECKSUM = "working_patch_apk_checksum";
    public static final String VERSION_CODE = "version_code";

    private static LoadPatchError loadPatchError;

    private SharedPreferences sharedPref;

    private AmigoClassLoader patchedClassLoader;
    private Instrumentation originalInstrumentation = null;
    private Object originalCallback = null;

    private AmigoDirs amigoDirs;
    private PatchApks patchApks;
    private int revertBitFlag = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            sharedPref = getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS);
            amigoDirs = AmigoDirs.getInstance(this);
            patchApks = PatchApks.getInstance(this);
            String workingPatchApkChecksum = sharedPref.getString(WORKING_PATCH_APK_CHECKSUM, "");
            Log.e(TAG, "working checksum: " + workingPatchApkChecksum);
            if (PatchChecker.checkUpgrade(this)) {
                Log.d(TAG, "Host app has upgrade");
                PatchCleaner.clearPatchIfInMainProcess(this);
                runOriginalApplication();
                return;
            }
            if (TextUtils.isEmpty(workingPatchApkChecksum)
                    || !patchApks.exists(workingPatchApkChecksum)) {
                Log.d(TAG, "Patch apk doesn't exists");
                PatchCleaner.clearPatchIfInMainProcess(this);
                runOriginalApplication();
                return;
            }

            // ensure load dex process always run host apk not patch apk
            if (ProcessUtils.isLoadDexProcess(this)) {
                Log.e(TAG, "load dex process");
                runOriginalApplication();
                return;
            }

            if (!ProcessUtils.isMainProcess(this) && isPatchApkFirstRun(workingPatchApkChecksum)) {
                Log.e(TAG, "None main process and patch apk is not released yet");
                runOriginalApplication();
                return;
            }

            // only release loaded apk in the main process
            runPatchApk(workingPatchApkChecksum);
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

    private void runPatchApk(String checksum) throws LoadPatchApkException {
        try {
            Log.e(TAG, "patchApkChecksum-->" + checksum + ", sp record checksum--->"
                    + sharedPref.getString(WORKING_PATCH_APK_CHECKSUM, ""));
            if (isPatchApkFirstRun(checksum) || !isOptedDexExists(checksum)) {
                // TODO This is workaround for now, refactor in future.
                sharedPref.edit().remove(checksum).commit();
                releasePatchApk(checksum);
            } else {
                PatchChecker.checkDexAndSo(this, checksum);
            }

            PatchDexAndSoLoader.loadPatchDexAndSo(this, checksum);
            revertBitFlag |= 1;
            patchedClassLoader = (AmigoClassLoader) getClassLoader();
            PatchResourceLoader.loadPatchResources(this, checksum);
            setApkInstrumentation();
            revertBitFlag |= 1 << 1;
            setApkHandlerCallback();
            revertBitFlag |= 1 << 2;
            ReceiverFinder.registerNewReceivers(this, patchedClassLoader);
            HookFactory.install(this, patchedClassLoader);
            ContentProviderFinder.installPatchContentProviders(this);

            sharedPref.edit().putString(WORKING_PATCH_APK_CHECKSUM, checksum).commit();
            PatchCleaner.clearOldPatches(this, checksum);
            runPatchedApplication(checksum);
        } catch (Exception e) {
            throw new LoadPatchApkException(e);
        }
    }

    private void setApkInstrumentation() throws Exception {
        Instrumentation oldInstrumentation =
                (Instrumentation) readField(instance(), "mInstrumentation", true);
        Log.e(TAG, "oldInstrumentation--->" + oldInstrumentation);
        AmigoInstrumentation instrumentation = new AmigoInstrumentation(oldInstrumentation);
        writeField(instance(), "mInstrumentation", instrumentation, true);
        originalInstrumentation = instrumentation;
        Log.e(TAG, "setApkInstrumentation success classloader-->"
                + instrumentation.getClass().getClassLoader());
    }

    private void rollbackApkInstrumentation() {
        try {
            writeField(instance(), "mInstrumentation", originalInstrumentation, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setApkHandlerCallback() throws Exception {
        Handler handler = (Handler) readField(instance(), "mH", true);
        Object callback = readField(handler, "mCallback", true);
        AmigoCallback value = new AmigoCallback(this, (Handler.Callback) callback);
        writeField(handler, "mCallback", value);
        originalCallback = callback;
        Log.e(TAG, "hook handler success");
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
            layoutId = (int) readStaticField(Class.forName(getPackageName()
                    + ".R$layout"), layoutName);
        }
        if (!TextUtils.isEmpty(themeName)) {
            themeId = (int) readStaticField(Class.forName(getPackageName()
                    + ".R$style"), themeName);
        }
        Log.e(TAG, String.format("layoutName-->%s, themeName-->%s", layoutName, themeName));
        Log.e(TAG, String.format("layoutId-->%d, themeId-->%d", layoutId, themeId));

        ApkReleaser.getInstance(this).work(checksum, layoutId, themeId);
        Log.e(TAG, "release apk once");
    }

    private boolean isPatchApkFirstRun(String checksum) {
        return !sharedPref.getString(WORKING_PATCH_APK_CHECKSUM, "").equals(checksum);
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
        Method attach = getMatchedMethod(Application.class, "attach", Context.class);
        attach.setAccessible(true);
        attach.invoke(application, getBaseContext());
        setAPKApplication(application);
        application.onCreate();
    }

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
        HookFactory.uninstallAllHooks(patchedClassLoader);
        PatchResourceLoader.revertLoadPatchResources();
        // TODO unregister providers
    }

    private void runPatchedApplication(String patchApkCheckSum) throws Exception {
        String applicationName = getPatchApplicationName(patchApkCheckSum);
        Application application =
                (Application) patchedClassLoader.loadClass(applicationName).newInstance();
        Method attach = getMatchedMethod(Application.class, "attach", Context.class);
        attach.setAccessible(true);
        attach.invoke(application, getBaseContext());
        setAPKApplication(application);
        application.onCreate();
    }

    private String getPatchApplicationName(String patchApkCheckSum) throws Exception {
        String applicationName = null;
        try {
            Class acd = patchedClassLoader.loadClass("me.ele.amigo.acd");
            if (acd != null && acd.getClassLoader() == patchedClassLoader) {
                applicationName = (String) readStaticField(acd, "n");
            }
        } catch (ClassNotFoundException classNotFoundExp) {
            Log.d(TAG, "runPatchedApplication: " + classNotFoundExp);
        }
        if (applicationName == null) {
            applicationName = getPackageManager().getPackageArchiveInfo(
                    PatchApks.getInstance(this).patchPath(patchApkCheckSum),
                    PackageManager.GET_META_DATA).applicationInfo.className;
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
        String patchChecksum = PatchChecker.checkPatchAndCopy(context, patchFile);
        if (patchChecksum == null) return;
        context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS)
                .edit()
                .putString(Amigo.WORKING_PATCH_APK_CHECKSUM, patchChecksum)
                .commit();
        AmigoService.start(context, patchChecksum, false);
        System.exit(0);
        Process.killProcess(Process.myPid());
    }

    public static void workLater(Context context, File patchFile) {
        String patchChecksum = PatchChecker.checkPatchAndCopy(context, patchFile);
        if (patchChecksum != null) {
            AmigoService.start(context, patchChecksum, true);
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

    public static int workingPatchVersion(Context ctx) {
        if (!hasWorked() || TextUtils.isEmpty(getWorkingPatchApkChecksum(ctx))) return -1;
        return CommonUtils.getVersionCode(ctx,
                PatchApks.getInstance(ctx).patchFile(getWorkingPatchApkChecksum(ctx)));
    }

    public static String getWorkingPatchApkChecksum(Context ctx) {
        if (!hasWorked()) return "";
        return ctx.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS)
                .getString(WORKING_PATCH_APK_CHECKSUM, "");
    }

    public static void clear(Context context) {
        context.getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS)
                .edit()
                .putString(WORKING_PATCH_APK_CHECKSUM, "")
                .commit();
    }

    public static LoadPatchError getLoadPatchError() {
        return loadPatchError;
    }
}
