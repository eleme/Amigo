package me.ele.amigo.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import java.io.File;

public class CommonUtils {

  public static int getVersionCode(Context context) {
    PackageManager pm = context.getPackageManager();
    try {
      return pm.getPackageInfo(context.getPackageName(), 0).versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
      return 0;
    }
  }

  public static int getVersionCode(Context context, File apkFile) {
    PackageManager pm = context.getPackageManager();
    return pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0).versionCode;
  }
}
