Amigo
====
[wiki](https://github.com/eleme/Amigo/wiki)

![amigo.png](http://amigotheband.com/wp-content/uploads/2015/02/logo_amigo-yellow.png)  

   一个Android 平台的hotfix 库


用法
----
   在project 的`build.gradle` 中

   ```groovy
   dependencies {
     classpath 'me.ele:amigo:0.1.0'
   }
   ```

   在module 的`build.gradle` 中

   ```groovy
   apply plugin: 'me.ele.amigo'
   ```

   就这样轻松的集成了Amigo。

### 生效补丁包
   补丁包生效有两种方式可以选择：

   * 稍后生效补丁包

   	如果不想立即生效而是用户第二次打开App 时才打入补丁包，第二次打开时就会自动生效。可以通过这个方法
   	
	```java
    Amigo.workLater(context);

    Amigo.workLater(context, apkFile);
    ```

   * 立即生效补丁包

   	如果想要补丁包立即生效，调用以下两个方法之一，App 会立即重启，并且打入补丁包。

   	```Java
   	Amigo.work(context);
   	```

   	```Java
   	Amigo.work(context, apkFile);
   	```


### 删除补丁包

如果需要删除掉已经下好的补丁包，可以通过这个方法

```Java
Amigo.clear(context);
```

**提示**：如果apk 发生了变化，Amigo 会自动清除之前的apk。

### 自定义界面

在热修复的过程中会有一些耗时的操作，这些操作会在一个新的进程中的Activity 中执行，所以你可以通过以下方式来自定义这个Activity。

```Java
<meta-data
  android:name="amigo_layout"
  android:value="{your-layout-name}" />

<meta-data
  android:name="amigo_theme"
  android:value="{your-theme-name}" />
```

### 局限
 - 新的apk中仅支持新增 `activity` & `receiver`, `service` & `provider`暂时不支持
       ```groovy
       classpath 'me.ele:amigo:0.0.6-beta1'
       ...
      ```
 - launcher activity的全类名暂时不支持修改
 
 - `notification` & `widget`中`RemoteViews`的自定义布局不支持修改,只支持内容修复
 
 - 可能会和google play上架协议有冲突
 
 - **唯一的限制就是你的想象力**
 
代码样例
----

   我们在代码中提供了demo 以便大家更快的上手Amigo 的使用，通过以下步骤尽情的去玩弄demo 吧：
   1. ./gradlew clean assembleRelease & adb install .../build/outputs/apk/app-release.apk
   2. 改动代码 & ./gradlew clean assembleRelease
   3. adb push .../build/outputs/apk/app-release.apk /sdcard/demo.apk
   4. 点击"apply patch apk"按钮, 加载新的apk
   
### 下载hotfix文件

- 简单来说,你只需要下载一个全新的apk

- 为用户的流量照想, 你可能只想下载一个差分文件
 [bspatch](https://github.com/eleme/bspatch)可能是你的一个选择



License
====

   	Copyright 2016 ELEME Inc.  

   	Licensed under the Apache License, Version 2.0 (the "License");
   	you may not use this file except in compliance with the License.
   	You may obtain a copy of the License at

   		http://www.apache.org/licenses/LICENSE-2.0

   	Unless required by applicable law or agreed to in writing, software
   	distributed under the License is distributed on an "AS IS" BASIS,
   	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   	See the License for the specific language governing permissions and
   	limitations under the License.
