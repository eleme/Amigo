package me.ele.amigo;

import android.app.Activity;
import android.app.Fragment;
import android.app.Instrumentation.ActivityResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;

public interface IInstrumentation {

    ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target,
                                     Intent intent, int requestCode, android.os.Bundle options);

    ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target,
                                     Intent intent, int requestCode);

    ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Fragment target,
                                     Intent intent, int requestCode);

    ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Fragment target,
                                     Intent intent, int requestCode, Bundle options);

    ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target,
                                     Intent intent, int requestCode, Bundle options, UserHandle user);

    ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, String target,
                                     Intent intent, int requestCode, Bundle options);
}
