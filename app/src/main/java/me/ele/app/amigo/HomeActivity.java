package me.ele.app.amigo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import me.ele.amigo.Amigo;
import me.ele.app.amigo.dev.DevActivity;
import me.ele.demo.A;


public class HomeActivity extends BaseActivity {

    public static final String TAG = HomeActivity.class.getSimpleName();

    private TextView infoView;

    private TextView metaDataView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        infoView = (TextView) findViewById(R.id.info);
        metaDataView = (TextView) findViewById(R.id.meta_data);
        Log.e(TAG, "onCreate");
        Log.e(TAG, "getApplication from HomeActivity-->" + getApplication());
        Log.e(TAG, "version code from host-->" + Amigo.getHostPackageInfo(this, 0).versionCode);

        infoView.setText(A.getDes());

        try {
            String metaData = getPackageManager().getApplicationInfo(getPackageName(),
                    PackageManager.GET_META_DATA).metaData.getString("data_key");
            metaDataView.setText("metaData:" + metaData);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void gotoDemo(View v) {
        startActivity(new Intent(this, DemoActivity.class));
    }

    public void gotoDev(View v) {
        startActivity(new Intent(this, DevActivity.class));
    }
}
