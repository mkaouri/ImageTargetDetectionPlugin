# ARToolKit Cordova Plugin (android & ios)
This plugin allows the application to recognize trained NFT markers and see if they are detected or not in Cordova callback.

## Install
To install the plugin in your current Cordova project run
```
cordova plugin add <git url>.git
```

### Android
Change the config.xml and add to the platform android the following line
```
<preference name="android-minSdkVersion" value="15" />
```
This will set the  minimum API target to be 15 overwriting the ARToolKit library minimum of API 14.

### IOS
Work in progress...
