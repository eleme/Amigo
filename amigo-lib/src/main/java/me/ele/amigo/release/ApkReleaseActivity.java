package me.ele.amigo.release;

import android.app.Activity;
import android.os.Bundle;

public class ApkReleaseActivity extends Activity {

    static final String LAYOUT_ID = "layout_id";
    static final String THEME_ID = "theme_id";

    private int layoutId;
    private int themeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        layoutId = getIntent().getIntExtra(LAYOUT_ID, 0);
        themeId = getIntent().getIntExtra(THEME_ID, 0);
        if (themeId != 0) {
            setTheme(themeId);
        }
        if (layoutId != 0) {
            setContentView(layoutId);
        }

        ApkReleaser.getInstance(this).release();
    }

    @Override
    public void onBackPressed() {
        //do nothing
    }
}
