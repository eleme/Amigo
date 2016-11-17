package me.ele.app.amigo.dev;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import me.ele.app.amigo.R;
import me.ele.demo.A;

public class SubprocessActivity extends AppCompatActivity {

    public static final String TAG = SubprocessActivity.class.getSimpleName();

    private TextView processInfoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_process);
        setTitle("SubProcess");
        processInfoView = (TextView) findViewById(R.id.process_info);

        StringBuilder sb = new StringBuilder("Page Info")
                .append("\n")
                .append("Current Process Name: ")
                .append(getCurrentProcessName())
                .append("\n")
                .append(A.getDes());
        processInfoView.setText(sb.toString());
    }

    private String getCurrentProcessName() {
        String currentProcName = "";
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                currentProcName = processInfo.processName;
                break;
            }
        }
        return currentProcName;
    }
}
