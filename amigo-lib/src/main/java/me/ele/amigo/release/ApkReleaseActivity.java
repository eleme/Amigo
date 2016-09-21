package me.ele.amigo.release;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

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
        overridePendingTransition(0, 0);
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

        ApkReleaser.getInstance(this).release(checksum);
    }

    @Override
    public void onBackPressed() {
        //do nothing
    }
}
