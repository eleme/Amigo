
[wiki](https://github.com/eleme/Amigo/wiki)

[changelog](https://github.com/eleme/Amigo/blob/master/CHANGELOG.md)

[Amigo平台](https://amigo.ele.me) (Amigo后端管理服务上线啦 :v:)

![amigo.png](http://amigotheband.com/wp-content/uploads/2015/02/logo_amigo-yellow.png)  

   一个Android 平台的hotfix 库, 支持热更新，支持热修复

## 用法

### 下载依赖
   在project 的`build.gradle` 中

```groovy
buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath 'me.ele:amigo:0.6.3'
    }
}
```

   在module 的`build.gradle` 中

```groovy
apply plugin: 'me.ele.amigo'

android {
 ...
}

//开发过程中你可以不想开启Amigo，可以把这个值设置为true
//你可以把这个定义在mybuild.gradle，以区分debug & release打包
amigo {
    disable false //默认 false
}

```

   就这样轻松的集成了Amigo。

### 兼容性

- 支持所有设备，从 ECLAIR `2.1` to Nougat `7.1`
- 甚至可以支持下个Android release的版本, 不管改动是否很大。 很酷，有木有 :v:
- 不支持Android 3.0


### 自定义loading界面（可选）

在热修复的过程中会进行一些耗时(dex优化)的操作，这些操作会在一个新的进程中的Activity 中执行，所以你可以在Manifest文件中增加下面的两个配置来自定义这个Activity，美化loading界面。

```xml
<meta-data
   android:name="amigo_layout"
   android:value="{your-layout-name}" />

<meta-data
   android:name="amigo_theme"
   android:value="{your-theme-name}" />

<meta-data
    android:name="amigo_orientation"
    android:value="{your-custom-orientation}"/>
```

**提示**:

- 这三个`meta-data`信息请写在你自己的app module下的`AndroidManifest.xml`
- orientation value值必须在[screenOrientation](https://developer.android.com/guide/topics/manifest/activity-element.html#screen)之内

### 运行patch apk
   补丁包生效方式有两种可以选择：

*    稍后生效

     如果不想立即生效而是用户第二次打开App 时才打入补丁包，可以通过这个方法：

    ```java
    Amigo.workLater(context, apkFile, callback);
    ```

*    立即生效

     如果想要补丁包立即生效，调用以下方法，App 会立即重启，并且打入补丁包:

    ```java
    Amigo.work(context, apkFile);
    ```

### 清除patch

```java
Amigo.clear(context);
```

**提示**: 将App下次启动时删除所有的patch文件.

## 示例

执行命令 `./gradlew runHost preparePatch` 到Demo页面查看.


## 开发样例

### Amigo gradle 插件
Gradle插件的代码在buildSrc目录下,这样每次编译项目时都会使用最新的插件代码。

### Amigo lib
Gradle插件会自动选择正确的库版本,在开发过程中,我们会使用amigo-lib这个模块,从而无需每次推送到maven仓库。

### 测试
我们在`app/build.gradle`提供了两个gradle task: `:app:runHost`, `:app:preparePatch`, 可以有效的帮助加快开发.

*   `./gradlew runHost`, 编译并启动宿主app
*   `./gradlew preparePatch`, 编译patch apk并推到设备sdcard中
*   在app中应用patch apk

## 局限
- patch包中新增provider
    * 修改声明方式，authorities须以"**${youPackageName}.provider**"开头

        ```xml
        <provider
            android:name="me.ele.demo.provider.StudentProvider"
            android:authorities="${youPackageName}.provider.student" />
        ```


    * 修改调用方式

        ```java
        // 1. app进程内使用时，无需做任何修改
        Cursor cursor = getContentResolver().query(Uri.parse("content://" + getPackageName() + ".provider.student?id=0"), null, null, null, null);
        // 2. 其他进程中的使用时，需要修改uri为以下形式, 其中targetPackageName为你的App的包名
        Cursor cursor = getContentResolver().query(Uri.parse("content://" + targetPackageName + ".provider/student?id=0"), null, null, null, null);
        ```

- 不支持和Instant Run同时使用

-  Amigo 不支持 Honeycomb `3.0`
    * Android 3.0 是一个满是bug的版本, & 并且Google已经关闭这个版本.

- `notification` & `widget`中`RemoteViews`的自定义布局不支持修改,只支持内容修复

   任何使用在`RemoteViews`里面的资源id都需要进行这样的包装 ```java RCompat.getHostIdentifier(Context context, int id) ```

- **唯一的限制就是你的想象力**

## 下载hotfix文件

- 简单来说, 你只需要下载一个全新的apk

- 为用户的流量着想, 你可能只想下载一个差分文件
   [bspatch](https://github.com/eleme/bspatch)(可针对Apk差分，或者基于Apk内容更细力度的差分)可能是你的一个选择

## Inspired by

[Android Patch 方案与持续交付](http://dev.qq.com/topic/57a31921ac3a1fb613dd40f3)

[DroidPlugin](https://github.com/DroidPluginTeam/DroidPlugin)


## License

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
