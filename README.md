## Amigo

![amigo.png](http://amigotheband.com/wp-content/uploads/2015/02/logo_amigo-yellow.png)  


An old Chinese saying goes: **Road to Jane**

古人有云：**大道至简**

so...

### usage

```groovy
 classpath 'me.ele:amigo:0.1.0'
 ...

 apply plugin: 'me.ele.amigo'
 ...
```

you are good to good now, as simple as this.

### if you don't need hotfix work immediately

you just need to download new apk file to /data/data/{your pkg}/files/amigo/demo.apk,
when app restarts next time, hotfix apk will be loaded as fresh as new.

```java
File hotfixApk = Amigo.getHotfixApk(context);
```

### work immediately, app will restart immediately

```java
Amigo.work(context);

Amigo.work(context, apkFile);
```

### maybe hotfix needs to be cleared

```java
Amigo.clear(context);
```