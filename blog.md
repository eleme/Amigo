# Amigo 源码解读
现在 hotfix 框架有很多，原理大同小异，基本上是基于[qq空间这篇文章](https://mp.weixin.qq.com/s?__biz=MzI1MTA1MzM2Nw==&mid=400118620&idx=1&sn=b4fdd5055731290eef12ad0d17f39d4a) 或者[微信的方案](http://mp.weixin.qq.com/s?__biz=MzAwNDY1ODY2OQ==&mid=2649286306&idx=1&sn=d6b2865e033a99de60b2d4314c6e0a25&scene=1&srcid=0811AOttpqUnh1Wu5PYcXbnZ#rd)。可惜的是微信的 Tinker 以及 QZone 都没有将其具体实现开源出来，只是在文章中分析了现有各个 hotfix 框架的优缺点以及他们的实现方案。Amigo 原理与 Tinker 基本相同，但是在 Tinker 的基础上，进一步实现了 so 文件、资源文件、Activity、BroadcastReceiver 的修复，几乎可以号称全面修复，不愧Amigo（朋友）这个称号，能在危急时刻送来全面的帮助。

在`Amigo`这个类中实现了主要的修复工作。我们一起追追看，到底是怎样的实现。

**Amigo.java**

```Java
...

if (demoAPk.exists() && isSignatureRight(this, demoAPk)) {
	 SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_MULTI_PROCESS);
	 String demoApkChecksum = checksum(demoAPk);
	 boolean isFirstRun = !sp.getString(NEW_APK_SIG, "").equals(demoApkChecksum);
...
```

这段代码中，首先检查是否有补丁包，并且签名正确，如果正确，则通过检验校验和是否与之前的检验和相同，不同则为检测到新的补丁包。

释放Apk
----

当这是新的补丁包时，首先第一件事就是释放。`ApkReleaser.work(this, layoutId, themeId)`在这个方法中最终会去开启一个 ApkReleaseActivity，而这个 Activity 的layout 和 theme 就是之前从配置中解析出来，在 work 方法中传进来的layoutId 和 themeId。

**ApkReleaseActivity.java**

```Java
@Override
protected void onCreate(Bundle savedInstanceState) {
   super.onCreate(savedInstanceState);
	...

   new Thread() {
       @Override
       public void run() {
           super.run();

           DexReleaser.releaseDexes(demoAPk.getAbsolutePath(), dexDir.getAbsolutePath());
           NativeLibraryHelperCompat.copyNativeBinaries(demoAPk, nativeLibraryDir);
           dexOptimization();

           handler.sendEmptyMessage(WHAT_DEX_OPT_DONE);
       }
   }.start();
}
```

在 ApkReleaseActivity 的 `onCreate()` 方法中会开启一个线程去进行一系列的释放操作，这些操作十分耗时，目前在不同的机子上测试，从几秒到二十几秒之间不等，如果就这样黑屏在用户前面未免太不优雅，所以 Amigo 开启了一个新的进程，启动这个 Activity。
在这个线程中，做了三件微小的事情：

* 释放 Dex 到指定目录
* 拷贝 so 文件到 Amigo 的指定目录下
  拷贝 so 文件是通过反射去调用 `NativeLibraryHelper`这个类的`nativeCopyNativeBinaries()`方法，但这个方法在不同版本上有不同的实现。

  	* 如果版本号在21以下

  		**NativeLibraryHelper**

		```
		public static int copyNativeBinariesIfNeededLI(File apkFile, File sharedLibraryDir) {
	        final String cpuAbi = Build.CPU_ABI;
	        final String cpuAbi2 = Build.CPU_ABI2;
	        return nativeCopyNativeBinaries(apkFile.getPath(), sharedLibraryDir.getPath(), cpuAbi,
	                cpuAbi2);
	    }
		```

  		会去反射调用这个方法，其中系统会自动判断出 primaryAbi 和 secondAbi。

	* 如果版本号在21以上
	`copyNativeBinariesIfNeededLI(file, file)`这个方法已经被废弃了，需要去反射调用这个方法

		**NativeLibraryHelper**

		```
		public static int copyNativeBinaries(Handle handle, File sharedLibraryDir, String abi) {
	        for (long apkHandle : handle.apkHandles) {
	            int res = nativeCopyNativeBinaries(apkHandle, sharedLibraryDir.getPath(), abi,
	                    handle.extractNativeLibs, HAS_NATIVE_BRIDGE);
	            if (res != INSTALL_SUCCEEDED) {
	                return res;
	            }
	        }
	        return INSTALL_SUCCEEDED;
	    }
		```

		所以首先得去获得一个`NativeLibraryHelper$Handle`类的实例。之后就是找 primaryAbi。Amigo 先对机器的位数做了判断，如果是64位的机子，就只找64位的 abi，如果是32位的，就只找32位的 abi。然后将 Handle 实例当做参数去调用`NativeLibraryHelper`的`findSupportedAbi`来获得primaryAbi。最后再去调用`copyNativeBinaries`去拷贝 so 文件。

	对于 so 文件加载的原理可以参考[这篇文章](http://mp.weixin.qq.com/s?__biz=MzA3NTYzODYzMg==&mid=2653577702&idx=1&sn=1288c77cd8fc2db68dc92cf18d675ace&scene=4#wechat_redirect)

* 优化 dex 文件

	**ApkReleaseActivity.java**

	```Java
	private void dexOptimization() {
		...
        for (File dex : validDexes) {
            new DexClassLoader(dex.getAbsolutePath(), optimizedDir.getAbsolutePath(), null, DexUtils.getPathClassLoader());
            Log.e(TAG, "dexOptimization finished-->" + dex);
        }
    }
	```

	DexClassLoader 没有做什么事情，只是调用了父类构造器，他的父类是 BaseDexClassLoader。在 BaseDexClassLoader 的构造器中又去构造了一个DexPathList 对象。
	在`DexPathList`类中，有一个 Element 数组

	**DexPathList**

	```
	/** list of dex/resource (class path) elements */
	private final Element[] dexElements;
	```

	Element 就是对 Dex 的封装。所以一个 Element 对应一个 Dex。这个 Element 在后文中会提到。

  优化 dex 只需要在构造 DexClassLoader 对象的时候将 dex 的路径传进去，系统会在最后会通过`DexFile`的

  **DexFile.java**

  ```Java
  native private static int openDexFile(String sourceName, String outputName,
        int flags) throws IOException;
  ```

   来这个方法来加载 dex，加载的同时会对其做优化处理。

这三项操作完成之后，通知优化完毕，之后就关闭这个进程，将补丁包的校验和保存下来。这样第一步释放 Apk 就完成了。之后就是重头戏替换修复。

替换修复
----

###替换classLoader

Amigo 先行构造一个`AmigoClassLoader`对象，这个`AmigoClassLoader`是一个继承于`PathClassLoader`的类，把补丁包的 Apk 路径作为参数来构造`AmigoClassLoader`对象，之后通过反射替换掉 LoadedApk 的 ClassLoader。这一步是 Amigo 的关键所在。

###替换Dex

之前提到，每个 dex 文件对应于一个`PathClassLoader`，其中有一个 Element[]，Element 是对于 dex 的封装。

**Amigo.java**

```java
private void setDexElements(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
   Object dexPathList = getPathList(classLoader);
   File[] listFiles = dexDir.listFiles();

   List<File> validDexes = new ArrayList<>();
   for (File listFile : listFiles) {
       if (listFile.getName().endsWith(".dex")) {
           validDexes.add(listFile);
       }
   }
   File[] dexes = validDexes.toArray(new File[validDexes.size()]);
   Object originDexElements = readField(dexPathList, "dexElements");
   Class<?> localClass = originDexElements.getClass().getComponentType();
   int length = dexes.length;
   Object dexElements = Array.newInstance(localClass, length);
   for (int k = 0; k < length; k++) {
       Array.set(dexElements, k, getElementWithDex(dexes[k], optimizedDir));
   }
   writeField(dexPathList, "dexElements", dexElements);
}
```

在替换dex时，Amigo 将补丁包中每个 dex 对应的 Element 对象拿出来，之后组成新的 Element[]，通过反射，将现有的 Element[] 数组替换掉。
在 QZone 的实现方案中，他们是通过将新的 dex 插到 Element[] 数组的第一个位置，这样就会先加载新的 dex ，微信的方案是下发一个 DiffDex，然后在运行时与旧的 dex 合成一个新的 dex。但是 Amigo 是下发一个完整的 dex直接替换掉了原来的 dex。与其他的方案相比，Amigo 因为直接替换原来的 dex ,兼容性更好，能够支持修复的方面也更多。但是这也导致了 Amigo 的补丁包会较大，当然，也可以发一个利用 BsDiff 生成的差分包，在本地合成新的 apk 之后再放到 Amigo 的指定目录下。

###替换动态链接库

**Amigo.java**

```java
private void setNativeLibraryDirectories(AmigoClassLoader hackClassLoader)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException {
   injectSoAtFirst(hackClassLoader, nativeLibraryDir.getAbsolutePath());
   nativeLibraryDir.setReadOnly();
   File[] libs = nativeLibraryDir.listFiles();
   if (libs != null && libs.length > 0) {
       for (File lib : libs) {
           lib.setReadOnly();
       }
   }
}
```

so 文件的替换跟 QZone 替换 dex 原理相差不多，也是利用 ClassLoader 加载 library 的时候，将新的 library 加到数组前面，保证先加载的是新的 library。但是这里会有几个小坑。

**DexUtils.java**

```Java
public static void injectSoAtFirst(ClassLoader hackClassLoader, String soPath) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        Object[] baseDexElements = getNativeLibraryDirectories(hackClassLoader);
        Object newElement;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Constructor constructor = baseDexElements[0].getClass().getConstructors()[0];
            constructor.setAccessible(true);
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] == File.class) {
                    args[i] = new File(soPath);
                } else if (parameterTypes[i] == boolean.class) {
                    args[i] = true;
                }
            }

            newElement = constructor.newInstance(args);
        } else {
            newElement = new File(soPath);
        }
        Object newDexElements = Array.newInstance(baseDexElements[0].getClass(), 1);
        Array.set(newDexElements, 0, newElement);
        Object allDexElements = combineArray(newDexElements, baseDexElements);
        Object pathList = getPathList(hackClassLoader);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            writeField(pathList, "nativeLibraryPathElements", allDexElements);
        } else {
            writeField(pathList, "nativeLibraryDirectories", allDexElements);
        }
    }
```

注入 so 文件到数组时，会发现在不同的版本上封装 so 文件的是不同的类，在版本23以下，是File

**DexPathList.java**

```java
/** list of native library directory elements */
private final File[] nativeLibraryDirectories;
```

在23以上却是改成了Element

**DexPathList.java**

```java
/** List of native library path elements. */
private final Element[] nativeLibraryPathElements;
```

因此在23以上，Amigo 通过反射去构造一个 Element 对象。之后就是将 so 文件插到数组的第一个位置就行了。
第二个小坑是nativeLibraryDir要设置成readOnly。

**DexPathList.java**

```
public String findNativeLibrary(String name) {
	  maybeInit();
	  if (isDirectory) {
	      String path = new File(dir, name).getPath();
	      if (IoUtils.canOpenReadOnly(path)) {
	          return path;
	      }
	  } else if (zipFile != null) {
	      String entryName = new File(dir, name).getPath();
	      if (isZipEntryExistsAndStored(zipFile, entryName)) {
	        return zip.getPath() + zipSeparator + entryName;
	      }
	  }
	  return null;
}
```

在ClassLoader 去寻找本地库的时候，如果 so 文件没有设置成ReadOnly的话是会不会返回路径的，这样就会报错了。

###替换资源文件

**Amigo.java**

```Java
...
AssetManager assetManager = AssetManager.class.newInstance();
Method addAssetPath = getDeclaredMethod(AssetManager.class, "addAssetPath", String.class);
addAssetPath.setAccessible(true);
addAssetPath.invoke(assetManager, demoAPk.getAbsolutePath());
setAPKResources(assetManager)
...
```

想要更新资源文件，只需要更新`Resource`中的 AssetManager 字段。`AssetManager`提供了一个方法`addAssetPath`。将新的资源文件路径加到`AssetManager`中就可以了。在不同的 configuration 下，会对应不同的 Resource 对象，所以通过 ResourceManager 拿到所有的 configuration 对应的 resource 然后替换其 assetManager。

###替换原有 Application

**Amigo.java**

```Java
...
Class acd = classLoader.loadClass("me.ele.amigo.acd");
String applicationName = (String) readStaticField(acd, "n");
Application application = (Application) classLoader.loadClass(applicationName).newInstance();
Method attach = getDeclaredMethod(Application.class, "attach", Context.class);
attach.setAccessible(true);
attach.invoke(application, getBaseContext());
setAPKApplication(application);
application.onCreate();
...
```

在编译过程中，Amigo 的插件将 app 的 application 替换成了 Amigo，并且将原来的 application 的 name 保存在了一个名为`acd`的类中，该修复的都修复完了是时候将原来的 application 替换回来了。拿到原有 Application 名字之后先调用 application 的`attach(context)`，然后将 application 设回到 loadedApk 中，最后调用`oncreate()`，执行原有 Application 中的逻辑。
这之后，一个修复完的 app 就出现在用户面前。优秀的库~

Amigo 插件
----

前文提到 Amigo 在编译期利用插件替换了 app 原有的 application，那这一个操作是怎么实现的呢？

**AmigoPlugin.groovy**

```groovy
File manifestFile = output.processManifest.manifestOutputFile
                        def manifest = new XmlParser().parse(manifestFile)
                        def androidTag = new Namespace("http://schemas.android.com/apk/res/android", 'android')
                        applicationName = manifest.application[0].attribute(androidTag.name)
                        manifestFile.text = manifestFile.text.replace(applicationName, "me.ele.amigo.Amigo")
```

首先，Amigo Plugin 将 AndroidManifest.xml 文件中的applicationName 替换成 Amigo。

**AmigoPlugin.groovy**

```groovy
Node node = (new XmlParser()).parse(manifestFile)
Node appNode = null
for (Node n : node.children()) {
   if (n.name().equals("application")) {
       appNode = n;
       break
   }
}
Node hackAppNode = new Node(appNode, "activity")
hackAppNode.attributes().put("android:name", applicationName)
manifestFile.text = XmlUtil.serialize(node)
```

之后，Amigo Plugin 做了很 hack 的一步，就是在 AndroidManifest.xml 中将原来的 application 做为一个 Activity 。我们知道 MultiDex 分包的规则中，一定会将 Activity 放到主 dex 中，Amigo Plugin 为了保证原来的 application 被替换后仍然在主 dex 中，就做了这个十分 hack 的一步。机智的少年。

接下来会再去判断是否开启了混淆，如果有混淆的话，查找 mapping 文件，将 applicationName 字段换成混淆后的名字。

下一步会去执行 GenerateCodeTask，在这个 task 中会生成一个 Java 文件，这个文件就是上文提到过得`acd.java`，并且将模板中的 appName 替换成applicationName。
然后执行 javaCompile task，编译 Java 代码。
最后还要做一件事，就是修改 maindexlist.txt。被定义在这个文件中的类会被加到主 dex 中，所以 Amigo plugin 在`collectMultiDexInfo`方法中扫描加到主 dex 的类，然后再在扫描的结果中加上 acd.class，把这些内容全部加到 maindexlist.txt。到此Amigo plugin 的任务就完成了。
Amigo plugin 的主要目的是在编译期用 amigo 替换掉原来的 application，但是还得保存下来这个 application，因为之后还得在运行时将这个 application 替换回来。

总结
----
Amigo 几乎实现了全方位的修复，通过替换 ClassLoader，直接全量替换 dex 的思路，保证了兼容性，成功率，但是可能下发的补丁包会比较大。还有一点 Amigo 的精彩之处就是利用 Amigo 替换了 app 原有的 application，这一点保证了 Amigo 连 application 都能修复。以后可能唯一不能修复的就是 Amigo 自身了。

最后我们比较下目前几个 hotfix 方案：

|对比项| Amigo | Tinker | nuwa/QZone | AndFix | Dexposed|
| ---|------|---|---|----|---|
|类替换 | yes | yes| yes |no| no|
|lib替换|yes | yes| no | no | no|
|资源替换|yes|yes|yes|no|no|
|全平台支持|yes|yes|yes|yes|no|
|即时生效|optional|no|no|yes|yes|
|性能损耗|无|较小|较大|较小|较小|
|补丁包大小|较大|较小|较大|一般|一般|
|开发透明|yes|yes|yes|no|no|
|复杂度|无|较低|较低|复杂|复杂|
|gradle支持|yes|yes|yes|no|no|
|接口文档|丰富|丰富|一般|一般|较少|
|占Rom体积|较大|较大|较小|较小|较小|
|成功率|100%|较好|很高|一般|一般|



