package me.ele.app.amigo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.ele.app.amigo.dev.DevActivity;
import me.ele.demo.A;


public class HomeActivity extends AppCompatActivity {

    public static final String TAG = HomeActivity.class.getSimpleName();

    @BindView(R.id.info)
    TextView infoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle("Home Activity");

        ButterKnife.bind(this);

        Log.e(TAG, "onCreate");
        Log.e(TAG, "getApplication from HomeActivity-->" + getApplication());

        infoView.setText(A.getDes());
    }

    @OnClick(R.id.goto_demo)
    public void gotoDemo(View v) {
        startActivity(new Intent(this, DemoActivity.class));
    }

    @OnClick(R.id.goto_dev)
    public void gotoDev(View v) {
        startActivity(new Intent(this, DevActivity.class));
    }
}
