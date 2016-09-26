package me.ele.app.amigo.dev;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ele.app.amigo.R;

public class DevActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Dev");

        setContentView(R.layout.activity_dev);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.launch_sub_process)
    public void launchSubProcess(View v) {
        startActivity(new Intent(this, SubprocessActivity.class));
    }
}
