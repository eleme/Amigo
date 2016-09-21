package me.ele.amigo.release;

import android.content.Context;
import android.content.Intent;

final class Launcher {

    private int layoutId;
    private int themeId;
    private String checksum;
    private Context context;

    public Launcher(Context context) {
        this.context = context;
    }

    public Launcher layoutId(int layoutId) {
        this.layoutId = layoutId;
        return this;
    }

    public Launcher themeId(int themeId) {
        this.themeId = themeId;
        return this;
    }

    public Launcher checksum(String checksum) {
        this.checksum = checksum;
        return this;
    }

    public void launch() {
        Intent intent = new Intent(context, ApkReleaseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ApkReleaseActivity.LAYOUT_ID, layoutId);
        intent.putExtra(ApkReleaseActivity.THEME_ID, themeId);
        intent.putExtra(ApkReleaseActivity.PATCH_CHECKSUM, checksum);
        context.startActivity(intent);
    }
}
