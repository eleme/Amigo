package me.ele.app.amigo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ele.app.amigo.activity.PatchedSingleInstanceActivity;
import me.ele.app.amigo.activity.PatchedSingleTaskActivity;
import me.ele.app.amigo.activity.PatchedSingleTopActivity;
import me.ele.app.amigo.activity.PatchedStandardActivity;

/**
 * Created by wwm on 9/30/16.
 */

public class TestPatchedActivities extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_patched_activities);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.start_standard_activity)
    public void startStandard() {
        startActivity(new Intent(this, PatchedStandardActivity.class));
    }

    @OnClick(R.id.start_single_top_activity)
    public void startSingleTop() {
        startActivity(new Intent(this, PatchedSingleTopActivity.class));
        startActivity(new Intent(this, PatchedSingleTopActivity.class).putExtra("extra", new ParcelBean()));

//        startActivity(new Intent(this, PatchedSingleTopActivity2.class));
//        startActivity(new Intent(this, PatchedSingleTopActivity2.class).putExtra("extra", "extra1"));
    }

    @OnClick(R.id.start_single_task_activity)
    public void startSingleTask() {
        startActivity(new Intent(this, PatchedSingleTaskActivity.class));
        startActivity(new Intent(this, PatchedSingleTaskActivity.class).putExtra("extra", "extra1"));

//        startActivity(new Intent(this, PatchedSingleTaskActivity2.class));
//        startActivity(new Intent(this, PatchedSingleTaskActivity2.class).putExtra("extra", "extra1"));
    }

    @OnClick(R.id.start_single_instance_activity)
    public void startSingleInstance() {
        startActivity(new Intent(this, PatchedSingleInstanceActivity.class));
        startActivity(new Intent(this, PatchedSingleInstanceActivity.class).putExtra("extra", "extra1"));

//        startActivity(new Intent(this, PatchedSingleInstanceActivity2.class));
//        startActivity(new Intent(this, PatchedSingleInstanceActivity2.class).putExtra("extra", "extra1"));
    }
}
