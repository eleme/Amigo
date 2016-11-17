package me.ele.amigo.compat;


import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import me.ele.amigo.reflect.MethodUtils;
import me.ele.amigo.utils.FileUtils;

public class NativeLibraryHelperCompat {

    private static final String TAG = NativeLibraryHelperCompat.class.getSimpleName();

    private static final Class nativeLibraryHelperClass() throws ClassNotFoundException {
        return Class.forName("com.android.internal.content.NativeLibraryHelper");
    }

    private static final Class handleClass() throws ClassNotFoundException {
        return Class.forName("com.android.internal.content.NativeLibraryHelper$Handle");
    }

    public static final int copyNativeBinaries(File apkFile, File sharedLibraryDir) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return copyNativeBinariesAfterL(apkFile, sharedLibraryDir);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return copyNativeBinariesAfterICE(apkFile, sharedLibraryDir);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return copyNativeBinariesAfterGingerBread(apkFile, sharedLibraryDir);
        } else {
            return copyNativeBinariesAfterECLAIR_MR1(apkFile, sharedLibraryDir);
        }
    }

    private static int copyNativeBinariesAfterGingerBread(File apkFile, File sharedLibraryDir) {
        try {
            Object[] args = new Object[2];
            args[0] = apkFile;
            args[1] = sharedLibraryDir;
            return (int) MethodUtils.invokeStaticMethod(nativeLibraryHelperClass(),
                    "copyNativeBinariesLI", args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return -1;
    }

    private static int copyNativeBinariesAfterICE(File apkFile, File sharedLibraryDir) {
        try {
            Object[] args = new Object[2];
            args[0] = apkFile;
            args[1] = sharedLibraryDir;
            return (int) MethodUtils.invokeStaticMethod(nativeLibraryHelperClass(),
                    "copyNativeBinariesIfNeededLI", args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static int copyNativeBinariesAfterL(File apkFile, File sharedLibraryDir) {
        try {
            Object handleInstance = MethodUtils.invokeStaticMethod(handleClass(), "create",
                    apkFile);
            if (handleInstance == null) {
                return -1;
            }

            String abi = null;

            if (isVM64()) {
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                    Set<String> abis = getAbisFromApk(apkFile.getAbsolutePath());
                    if (abis == null || abis.isEmpty()) {
                        return 0;
                    }
                    int abiIndex = (int) MethodUtils.invokeStaticMethod(nativeLibraryHelperClass
                            (), "findSupportedAbi", handleInstance, Build.SUPPORTED_64_BIT_ABIS);
                    if (abiIndex >= 0) {
                        abi = Build.SUPPORTED_64_BIT_ABIS[abiIndex];
                    }
                }
            } else {
                if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                    Set<String> abis = getAbisFromApk(apkFile.getAbsolutePath());
                    if (abis == null || abis.isEmpty()) {
                        return 0;
                    }
                    int abiIndex = (int) MethodUtils.invokeStaticMethod(nativeLibraryHelperClass
                            (), "findSupportedAbi", handleInstance, Build.SUPPORTED_32_BIT_ABIS);
                    if (abiIndex >= 0) {
                        abi = Build.SUPPORTED_32_BIT_ABIS[abiIndex];
                    }
                }
            }

            if (abi == null) {
                return -1;
            }

            Object[] args = new Object[3];
            args[0] = handleInstance;
            args[1] = sharedLibraryDir;
            args[2] = abi;
            return (int) MethodUtils.invokeStaticMethod(nativeLibraryHelperClass(),
                    "copyNativeBinaries", args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean isVM64() throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        Set<String> supportedAbis = getAbisFromApk(getHostApk());
        if (Build.SUPPORTED_64_BIT_ABIS.length == 0) {
            return false;
        }

        if (supportedAbis == null || supportedAbis.isEmpty()) {
            return true;
        }

        for (String supportedAbi : supportedAbis) {
            if ("arm64-v8a".endsWith(supportedAbi)
                    || "x86_64".equals(supportedAbi)
                    || "mips64".equals(supportedAbi)) {
                return true;
            }
        }

        return false;
    }

    private static Set<String> getAbisFromApk(String apk) {
        try {
            ZipFile apkFile = new ZipFile(apk);
            Enumeration<? extends ZipEntry> entries = apkFile.entries();
            Set<String> supportedAbis = new HashSet<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.contains("../")) {
                    continue;
                }
                if (name.startsWith("lib/") && !entry.isDirectory() && name.endsWith(".so")) {
                    String supportedAbi = name.substring(name.indexOf("/") + 1, name.lastIndexOf
                            ("/"));
                    supportedAbis.add(supportedAbi);
                }
            }
            Log.d(TAG, "supportedAbis : " + supportedAbis);
            return supportedAbis;
        } catch (Exception e) {
            Log.e(TAG, "get supportedAbis failure", e);
        }

        return null;
    }

    private static String getHostApk() throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Application application = (Application) MethodUtils.invokeStaticMethod
                (ActivityThreadCompat.clazz(), "currentApplication");
        return application.getApplicationInfo().sourceDir;
    }

    private static int copyNativeBinariesAfterECLAIR_MR1(File apkFile, File sharedLibraryDir) {
        final String sharedLibraryABI = Build.CPU_ABI;
        final String apkSharedLibraryPrefix = "lib/" + sharedLibraryABI + "/";
        final String sharedLibrarySuffix = ".so";
        byte[] buffer = new byte[8 * 1024];
        try {
            FileUtils.mkdirChecked(sharedLibraryDir);

            ZipInputStream zis = new ZipInputStream(new FileInputStream(apkFile));
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                String fileName = ze.getName();
                if (!fileName.startsWith(apkSharedLibraryPrefix)
                        || !fileName.endsWith(sharedLibrarySuffix)) {
                    ze = zis.getNextEntry();
                    continue;
                }
                File newFile = new File(sharedLibraryDir + File.separator +
                        fileName.substring(apkSharedLibraryPrefix.length()));
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return -1;
        }
        return 1;
    }
}