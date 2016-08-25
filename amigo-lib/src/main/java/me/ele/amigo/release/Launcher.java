package me.ele.amigo.release;

import android.content.Context;
import android.content.Intent;

final class Launcher {

    private int layoutId;
    private int themeId = -1;
    private Context context;

    public Launcher(Context context, int layoutId) {
        this.layoutId = layoutId;
        this.context = context;
    }

    public Launcher themeId(int themeId) {
        this.themeId = themeId;
        return this;
    }

    public void launch() {
        Intent intent = new Intent(context, ApkReleaseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ApkReleaseActivity.LAYOUT_ID, layoutId);
        intent.putExtra(ApkReleaseActivity.THEME_ID, themeId);
        context.startActivity(intent);
    }
}
