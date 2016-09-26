package me.ele.amigo.sdk;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

import me.ele.amigo.Amigo;
import me.ele.amigo.sdk.http.Error;
import me.ele.amigo.sdk.http.Http;
import me.ele.amigo.sdk.http.Method;
import me.ele.amigo.sdk.http.Request;
import me.ele.amigo.sdk.http.Response;
import me.ele.amigo.sdk.model.PatchInfo;
import me.ele.amigo.sdk.utils.DeviceId;
import me.ele.amigo.sdk.utils.MD5;
import me.ele.amigo.utils.CommonUtils;

import static me.ele.amigo.sdk.utils.CommonUtil.byteArray2String;

public class AmigoSdk {
    // todo need to change to real url
    public static final String TEST_PATCH_INFO_URL = "http://localhost:3000/patch_info";

    public static final String SP_NAME = "amigo-sdk";

    private static final String TAG = AmigoSdk.class.getSimpleName();
    private static final String APK_NAME_ROOT = "amigo_patch";

    private static Context context;
    // todo
    private static String appId = "";
    private static String deviceId = "";

    public static final void init(Context ctx) {
        context = ctx;
        deviceId = DeviceId.getDeviceId(context);

        Http.performRequest(Request.newRequest(TEST_PATCH_INFO_URL).method(Method.GET), new Http.SimpleCallback() {
            @Override
            public void onSucc(Response response) {
                PatchInfo patchInfo = PatchInfo.fromJson(byteArray2String(response.body()));
                if (!patchInfo.hasPatch()) {
                    return;
                }
                String url = patchInfo.apkUrl();
                String md5 = patchInfo.md5();
                PatchInfo.WorkPattern workPattern = patchInfo.workPattern();
                if (!getPatchApkFile(md5).exists()) {
                    requestPatchApk(url, md5, workPattern);
                } else {
                    applyPatchApk(getPatchApkFile(md5), workPattern);
                }
            }

            @Override
            public void onFail(Error error) {
                Log.e(TAG, "request patch info error: " + error);
            }
        });
    }

    private static final File getPatchApkFile(String md5) {
        return new File(Environment.getExternalStorageDirectory(), buildPatchApkName(md5));
    }

    private static String buildPatchApkName(String md5) {
        return APK_NAME_ROOT + "_" + md5 + ".apk";
    }

    private static final void requestPatchApk(String url, final String md5, final PatchInfo.WorkPattern workPattern) {
        Http.performRequest(Request.newRequest(url).method(Method.GET), new Http.SimpleCallback() {
            @Override
            public void onSucc(Response response) {
                try {
                    File apkFile = getPatchApkFile(md5);
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    fos.write(response.body());
                    fos.close();
                    if (!verifyPatchApk(apkFile, md5)) {
                        Log.e(TAG, "downloaded patch apk is wrong");
                        return;
                    }
                    applyPatchApk(apkFile, workPattern);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(Error error) {
                Log.e(TAG, "download apk error: " + error);
            }
        });
    }

    private static boolean verifyPatchApk(File apk, String md5) {
        return MD5.checkMD5(md5, apk);
    }

    private static final void applyPatchApk(File apk, PatchInfo.WorkPattern workPattern) {
        if (Amigo.hasWorked() && Amigo.workingPatchVersion(context) <= CommonUtils.getVersionCode(context, apk)) {
            return;
        }
        if (workPattern == PatchInfo.WorkPattern.WORK_LATER) {
            Amigo.workLater(context, apk);
        } else if (workPattern == PatchInfo.WorkPattern.WORK_NOW) {
            Amigo.work(context, apk);
        }
    }

    public static String appId() {
        return appId;
    }

    public static String deviceId() {
        return deviceId;
    }
}
