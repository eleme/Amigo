#Amigo Usage

[中文版](https://github.com/eleme/Amigo/blob/master/README_zh.md#amigo)
[wiki](https://github.com/eleme/Amigo/wiki)

![amigo.png](http://amigotheband.com/wp-content/uploads/2015/02/logo_amigo-yellow.png)  

Amigo is a hotfix library which can hotfix almost everything for your app.

## Usage
In your project's `build.gradle`

```groovy
dependencies {
  classpath 'me.ele:amigo:0.4.2'
}
```
In your module's `build.gradle`

```
 apply plugin: 'me.ele.amigo'
```

you are good to go now, as simple as this.

## Demo
There is a app module sub project. And there is an Demo page in the app demonstrating how to apply patch apk. Please ref the usage.
Run the task `./gradlew runHost preparePatch`. And go to the `demo` page.

## Development
#### Process
There are two gradle tasks provided in the app/build.gradle, `:app:runHost`, `:app:preparePatch`, which can accelerate development.

* `./gradlew runHost`, launch the host app
* `./gradlew preparePatch`, build and push the patch apk to the device
* apply the patch apk in the Demo page

#### Gradle plugin
The plugin was put into buildSrc directory, which means the plugin code change will work immediately each time you build.

#### amigo lib
The gradle plugin would select right amigo lib automatically. In the development mode, the amigo-lib module will be used.

## notice

* Instant Run conflicts with Amigo, so disable Instant Run when used with amigo

#### to make hotfix work
There are two ways to make hotfix work.

* if you don't need hotfix work immediately

	you just need to download new apk file to /data/data/{your pkg}/files/amigo/demo.apk,
	when app restarts next time, hotfix apk will be loaded as fresh as new.

	```java
    Amigo.workLater(context, patchApkFile);
    ```

* work immediately, app will restart immediately

	```java
	Amigo.work(context, patchApkFile);
	```

### disable working patch apk

```java
Amigo.clear(context);
```
**note**：When the main process is restarted the host apk will be used and all patch files will be deleted.


## customize the fix layout
some time-tense operation is handled in a new process with an activity, you may customize it

```xml
<meta-data
    android:name="amigo_layout"
    android:value="{your-layout-name}" />

<meta-data
    android:name="amigo_theme"
    android:value="{your-theme-name}" />

```

## limitations
 - new added `provider` is not supported for now

 - app launcher activity's name cannot be changed
 
 - `RemoteViews`'s layout change in `notification` & `widget`is not support 
 
 - may conflict with google play terms
 
 - **the only limit is your imagination**

## retrieve hotfix file

- make it simple, you just need a fully new apk

- for user's network traffic's sake, you may just want to download a diff file
  [bspatch](https://github.com/eleme/bspatch) is an option for you
  

## Inspired by

[Android Patch 方案与持续交付](http://dev.qq.com/topic/57a31921ac3a1fb613dd40f3)

[DroidPlugin](https://github.com/DroidPluginTeam/DroidPlugin)


license
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
