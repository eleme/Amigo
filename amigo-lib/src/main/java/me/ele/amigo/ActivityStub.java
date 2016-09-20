package me.ele.amigo;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public abstract class ActivityStub extends Activity {

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(getClass().getName(), "onNewIntent: " + intent);
    }

    public static class SingleInstanceStub extends ActivityStub {
    }

    public static class SingleTaskStub extends ActivityStub {

        @Override
        protected void onNewIntent(Intent intent) {
            super.onNewIntent(intent);
        }
    }

    public static class SingleTopStub extends ActivityStub {
    }

    public static class StandardStub extends ActivityStub {
    }


}
