package me.ele.amigo.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Method;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.e(TAG, "resources--->" + getResources());
        Log.e(TAG, "onCreate");
        Log.e(TAG, "added string--->" + getString(R.string.added_string));
        Log.e(TAG, "app_name--->" + getString(R.string.app_name));
        Log.e(TAG, "getApplication from MainActivity-->" + getApplication());
        Log.e(TAG, "NewAddedClass-->" + new NewAddedClass());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                startActivity(new Intent(MainActivity.this, SecondActivity.class));

            }
        });

        ImageView imageView = (ImageView) findViewById(R.id.imageview);
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            Class clazz = Class.forName("me.ele.blur.JniStackBlur");
            Method method = clazz.getDeclaredMethod("blur", Bitmap.class, int.class, boolean.class);
            method.setAccessible(true);
            Bitmap blurBmp = (Bitmap) method.invoke(null, bitmap, 15, false);
            imageView.setImageBitmap(blurBmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
