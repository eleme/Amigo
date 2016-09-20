package me.ele.app.amigo.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import me.ele.app.amigo.R;

public class SingleTopActivity extends AppCompatActivity {

    public static final String TAG = SingleTopActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_second);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SingleTopActivity.this, SingleTaskActivity.class);
                startActivity(intent);
            }
        });
    }
}
