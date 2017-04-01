package me.ele.amigo.release;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import me.ele.amigo.Amigo;
import me.ele.amigo.AmigoService;
import me.ele.amigo.utils.OrientationUtil;

public class ApkReleaseActivity extends Activity {

    static final String LAYOUT_ID = "layout_id";
    static final String THEME_ID = "theme_id";
    static final String PATCH_CHECKSUM = "patch_checksum";

    private int layoutId;
    private int themeId;
    private String checksum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(OrientationUtil.getReleaseActivityOrientation(this));
        layoutId = getIntent().getIntExtra(LAYOUT_ID, 0);
        themeId = getIntent().getIntExtra(THEME_ID, 0);
        checksum = getIntent().getStringExtra(PATCH_CHECKSUM);
        if (TextUtils.isEmpty(checksum)) {
            throw new RuntimeException("patch apk checksum must not be empty");
        }
        if (themeId != 0) {
            setTheme(themeId);
        }
        if (layoutId != 0) {
            setContentView(layoutId);
        }

        AmigoService.startReleaseDex(this.getApplicationContext(), checksum, new Amigo
                .WorkLaterCallback() {
            @Override
            public void onPatchApkReleased(boolean success) {
                finish();
                overridePendingTransition(0, 0);
            }
        });
    }

    public static void launch(Context context, String checksum, int layoutId, int themeId) {
        Intent intent = new Intent(context, ApkReleaseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ApkReleaseActivity.LAYOUT_ID, layoutId);
        intent.putExtra(ApkReleaseActivity.THEME_ID, themeId);
        intent.putExtra(ApkReleaseActivity.PATCH_CHECKSUM, checksum);
        context.startActivity(intent);
    }
}
