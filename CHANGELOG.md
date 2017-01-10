## Version 0.6.3
_2017-01-10_
* add `autoDisableInInstantRunMode` for amigo extension

## Version [0.6.2](https://github.com/eleme/Amigo/releases/tag/v0.6.2)
_2016-12-17_
* support custom `ApkReleaseActivity`'s  orientation
* fix `getPackageInfo` issue

## Version [0.6.1](https://github.com/eleme/Amigo/releases/tag/v0.6.1)
_2016-12-09_
* refactor app restart self logic
* only compare version for upgrade

## Version [0.6.0-beta2](https://github.com/eleme/Amigo/releases/tag/v0.6.0-beta2)
_2016-12-05_
* use ContentProvider instead of SharedPreferences in multi-process communication
* fix clear issue
* fix cannot pull up main process issue
* fix Chinese issue in AndroidManifest, thanks to [wkt](https://github.com/wkt)'s contribution

## Version [0.6.0-beta](https://github.com/eleme/Amigo/releases/tag/v0.6.0-beta)
_2016-11-25_
* smaller diff file, compat with [zippatch](https://github.com/eleme/bspatch#usage)
* support disable amigo
* compat with all android versions, even Android 2.1
* support workLater **callback**
* fix android windowAnimation bug
* refactor some code & some small bugfix

## Version [0.5.0](https://github.com/eleme/Amigo/releases/tag/v0.5.0)
_2016-11-03_
* support Amigo hotfix itself :v:
* support **ContentProvider** :+1:
* **metaData** retrieved from patch.apk is support :+1:
* support app LauncherActivity' name change
* refactor **RCompact**
* fix Activity's **label** or **theme** not found

## Version [0.4.4](https://github.com/eleme/Amigo/releases/tag/v0.4.4)
_2016-10-19_
* fix delete directory bug

## Version [0.4.3](https://github.com/eleme/Amigo/releases/tag/v0.4.3)
_2016-10-17_
* optimize app init time

## Version [0.4.2](https://github.com/eleme/Amigo/releases/tag/v0.4.2)
_2016-10-15_
* fix load receiver error
* avoid service from mixed up

## Version [0.4.1](https://github.com/eleme/Amigo/releases/tag/v0.4.1)
_2016-10-11_
* fix check permission bug

## Version [0.4.0](https://github.com/eleme/Amigo/releases/tag/v0.4.0) (Deprecated)

_2016-10-10_

* support new added `service`
* refactor ClassLoader replacement
* support new added permission's check

## Version [0.3.1](https://github.com/eleme/Amigo/releases/tag/v0.3.1)

_2016-09-29_

* fix Intent unmarshalling Exception [issue99](https://github.com/eleme/Amigo/issues/99)

## Version [0.3.0](https://github.com/eleme/Amigo/releases/tag/v0.3.0)

_2016-09-26_

* support new added `activity` & `receiver`

## Version [0.2.0](https://github.com/eleme/Amigo/releases/tag/v0.2.0)

_2016-9-23_

* Refactor the demo/develop app
* Fix [re-apply patch apk bug](https://github.com/eleme/Amigo/issues/73)

## Version [0.1.0](https://github.com/eleme/Amigo/releases/tag/v0.1.0) (Deprecated)

_2016-09-14_

* First stable version
