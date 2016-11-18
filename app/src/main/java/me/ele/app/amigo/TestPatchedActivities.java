package me.ele.app.amigo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.util.Log;

/**
 * Created by wwm on 9/30/16.
 */

public class TestPatchedActivities extends BaseActivity {

    private static final String TAG = TestPatchedActivities.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_patched_activities);
        Log.e(TAG, "test from intent-->" + getIntent().getParcelableExtra("test"));
    }

    public void startStandard(View view) {
        try {
            startActivity(new Intent().setClassName(this, "me.ele.demo.activity" +
                    ".PatchedStandardActivity"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startSingleTop(View view) {
        try {
            startActivity(new Intent().setClassName(this, "me.ele.demo.activity" +
                    ".PatchedSingleTopActivity"));
            startActivity(new Intent().putExtra("extra", new ParcelBean()).setClassName(this, "me" +
                    ".ele.demo.activity.PatchedSingleTopActivity"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void startSingleTask(View view) {
        try {
            startActivity(new Intent().setClassName(this, "me.ele.demo.activity" +
                    ".PatchedSingleTaskActivity"));
            startActivity(new Intent().putExtra("extra", "extra1").setClassName(this, "me.ele" +
                    ".demo.activity.PatchedSingleTaskActivity"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void startSingleInstance(View view) {
        try {
            startActivity(new Intent().setClassName(this, "me.ele.demo.activity" +
                    ".PatchedSingleTaskActivity"));
            startActivity(new Intent().putExtra("extra", "extra1").setClassName(this, "me.ele" +
                    ".demo.activity.PatchedSingleTaskActivity"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
