package me.ele.app.amigo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by wwm on 9/30/16.
 */

public class TestPatchedActivities extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_patched_activities);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.start_standard_activity)
    public void startStandard() {
        try {
            startActivity(new Intent().setClassName(this, "me.ele.demo.activity" +
                    ".PatchedStandardActivity"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.start_single_top_activity)
    public void startSingleTop() {
        try {
            startActivity(new Intent().setClassName(this, "me.ele.demo.activity" +
                    ".PatchedSingleTopActivity"));
            startActivity(new Intent().putExtra("extra", new ParcelBean()).setClassName(this, "me" +
                    ".ele.demo.activity.PatchedSingleTopActivity"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @OnClick(R.id.start_single_task_activity)
    public void startSingleTask() {
        try {
            startActivity(new Intent().setClassName(this, "me.ele.demo.activity" +
                    ".PatchedSingleTaskActivity"));
            startActivity(new Intent().putExtra("extra", "extra1").setClassName(this, "me.ele" +
                    ".demo.activity.PatchedSingleTaskActivity"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @OnClick(R.id.start_single_instance_activity)
    public void startSingleInstance() {
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
