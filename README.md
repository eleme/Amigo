#Amigo Usage

[中文版](https://github.com/eleme/Amigo/blob/master/README_zh.md#amigo)

[wiki](https://github.com/eleme/Amigo/wiki)

[changelog](https://github.com/eleme/Amigo/blob/master/CHANGELOG.md)

![amigo.png](http://amigotheband.com/wp-content/uploads/2015/02/logo_amigo-yellow.png)  

Amigo is a hotfix library which can fix almost everything for your Android app.

## How to use 
### Download
In your project's `build.gradle`

```groovy
buildscript { 
    repositories {
        mavenCentral()
    }
    
    dependencies {
        classpath 'me.ele:amigo:0.5.0'
    }
}
```

In your module's `build.gradle`

```groovy
apply plugin: 'me.ele.amigo'

android {
...
}
```

### Customize loading page
Some time-consuming tasks are handled in a separate process to avoid ANR, you can customize the loading activity by add the follow code into your AndroidManifest.xml:

```xml
<meta-data
    android:name="amigo_layout"
    android:value="{your-layout-name}" />

<meta-data
    android:name="amigo_theme"
    android:value="{your-theme-name}" />
```

### Make hotfix work
There are two ways to make hotfix work.

* if you don't need hotfix work immediately
 
    you just need to download new apk file to /data/data/{your pkg}/files/amigo/demo.apk,
	when app restarts next time, hotfix apk will be loaded as fresh as new.
	
	```java
    Amigo.workLater(context, patchApkFile);
    ```
    
* work immediately, App will restart immediately
	
	```java
	Amigo.work(context, patchApkFile);
	```
	
### Remove patch

```java
Amigo.clear(context);
```
    
**note**：All patch files would be deleted on the next start up.

## Demo
And there is an Demo page in the app demonstrating how to apply patch apk.
Run the task `./gradlew runHost preparePatch`, and navigate to the `demo` page.

## Development

### Amigo gradle plugin
The plugin was put into buildSrc directory, which means the plugin code change will work immediately each time you build.

### Amigo lib
The amigo plugin will select the right amigo lib automatically.

### Run tests
There are two gradle tasks provided in the app/build.gradle, `:app:runHost`, `:app:preparePatch`, which can accelerate development.

* `./gradlew runHost`, launch the host app
* `./gradlew preparePatch`, build and push the patch apk to the device
* apply the patch apk in the Demo page

## Limits
 - have to change the way using a content provider
    * declare a new provider: the authorities string must start with "**${youPackageName}.provider**"
    
        ```xml
        <provider
            android:name="me.ele.demo.provider.StudentProvider"
            android:authorities="${youPackageName}.provider.student" />
        ```
        
    * change the uri used to do the query, insert, delete operations:
     
        ```java
        // 1. inside your app process, no modifications need:
        Cursor cursor = getContentResolver().query(Uri.parse("content://" + getPackageName() + ".provider.student?id=0"), null, null, null, null);
        // 2. in another process, have to change the authorities uri like the following : 
        Cursor cursor = getContentResolver().query(Uri.parse("content://" + targetPackageName + ".provider/student?id=0"), null, null, null, null);
        ```
        
 -  Instant Run conflicts with Amigo, so disable Instant Run when used with amigo
 - `RemoteViews`'s layout change in `notification` & `widget`is not support   
    any resource id in here should be used with ```java RCompat.getHostIdentifier(Context context, int id) ```

## Retrieve hotfix file

- make it simple, you just need a fully new apk

- to save the internet traffic, you may just want to download a diff file
  [bspatch](https://github.com/eleme/bspatch) is an option for you
  

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
