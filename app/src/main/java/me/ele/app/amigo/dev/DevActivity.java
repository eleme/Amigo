package me.ele.app.amigo.dev;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ele.amigo.Amigo;
import me.ele.amigo.sdk.AmigoSdk;
import me.ele.amigo.sdk.utils.DeviceId;
import me.ele.app.amigo.R;

public class DevActivity extends AppCompatActivity {

    // TODO: 16/9/26
    static final String DEV_APP_ID = "123456";

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

    @OnClick(R.id.sdk)
    public void testSdk(View v) {
        AmigoSdk.init(getApplication(), DEV_APP_ID);
    }
}
