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
      classpath 'me.ele:amigo:0.4.2'
   }
   ```

   在module 的`build.gradle` 中

   ```groovy
   apply plugin: 'me.ele.amigo'
   ```

   就这样轻松的集成了Amigo。

示例
----
执行命令 `./gradlew runHost preparePatch`. 到Demo页面查看.


开发样例
----
#### 流程
1. 我们在`app/build.gradle`提供了两个gradle task: `:app:runHost`, `:app:preparePatch`, 可以有效的帮助加快开发.

    * `./gradlew runHost`, 编译并启动宿主app
    * `./gradlew preparePatch`, 编译patch apk并推到设备sdcard中
    * 在app中应用patch apk

#### Gradle 插件
Gradle插件的代码在buildSrc目录下,这样每次编译项目时都会使用最新的插件代码。

#### amigo lib
Gradle插件会自动选择正确的库版本,在开发过程中,我们会使用amigo-lib这个模块,从而无需每次推送到maven仓库。

### 生效补丁包
   补丁包生效有两种方式可以选择：

   * 稍后生效补丁包

   	如果不想立即生效而是用户第二次打开App 时才打入补丁包，第二次打开时就会自动生效。可以通过这个方法
   	
	```java
    Amigo.workLater(context, apkFile);
    ```

   * 立即生效补丁包

   	如果想要补丁包立即生效，调用以下两个方法之一，App 会立即重启，并且打入补丁包。

   	```Java
   	Amigo.work(context, apkFile);
   	```

### 停用patch apk

```Java
Amigo.clear(context);
```

**提示**：当主进程重启之后, 将会使用宿主apk, 同时删除所有的patch files.


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
 - 新的apk中, 新增`provider`暂时不支持
      
 - launcher activity的全类名暂时不支持修改
 
 - `notification` & `widget`中`RemoteViews`的自定义布局不支持修改,只支持内容修复
 
 - 可能会和google play上架协议有冲突
 
 - **唯一的限制就是你的想象力**

### 下载hotfix文件

- 简单来说,你只需要下载一个全新的apk

- 为用户的流量照想, 你可能只想下载一个差分文件
 [bspatch](https://github.com/eleme/bspatch)可能是你的一个选择

## Inspired by

[Android Patch 方案与持续交付](http://dev.qq.com/topic/57a31921ac3a1fb613dd40f3)

[DroidPlugin](https://github.com/DroidPluginTeam/DroidPlugin)


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
