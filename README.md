# Image Detection Plugin (android & ios)
This plugin allows the application to detect if an inputed image target is visible, or not, by matching the image features with the device camera features using [OpenCV](http://opencv.org/). It also presents the device camera preview in the background.

### Note
The plugin is aimed to work in **portrait mode**, should also work in landscape but no guarantees.

## Install
To install the plugin in your current Cordova project run
```
cordova plugin add https://github.com/Cloudoki/ImageDetectionCordovaPlugin.git
```

### Android
The plugin aims to be used with Android API >= 16 (4.1 Jelly Bean).

### IOS
The plugin aims to be used with iOS version >= 7.

### Note
In *config.xml* add Android and iOS target preference
```javascript
<platform name="android">
    <preference name="android-minSdkVersion" value="16" />
</platform>
<platform name="ios">
    <preference name="target-device" value="handset"/>
    <preference name="deployment-target" value="7.0"/>
</platform>
```

## Usage
The plugin offers the functions `startProcessing`, `isDetecting` and `setPattern`.

`startProcessing` - the plugin will process the video frames captured by the camera if the inputed argument is `true`, if the argument is `false` no frames will be processed. Calls on success if the argument is set and on error if no value set.

**Note:** the plugins start with this option true.
```javascript
startProcessing(true or false, successCallback, errorCallback);
```

`isDetecting` - the plugin will callback on success function if detecting the pattern or on error function if it's not.
```javascript
isDetecting(successCallback, errorCallback);
```
`setDetectionTimeout` - this function will set a timeout (**in seconds**) in which the processing of the frames will not occur. Calls on success if the argument is set and on error if no value set.
```javascript
setDetectionTimeout(timeout, successCallback, errorCallback);
```

`setPattern` - sets the new pattern target to be detected. Calls on success if the pattern is set and on error if no pattern set. The input pattern must be a base64 image.
```javascript
setPattern(base64image, successCallback, errorCallback);
```

## Usage example
```javascript
ImageDetectionPlugin.startProcessing(true, function(success){console.log(success);}, function(error){console.log(error);});

ImageDetectionPlugin.isDetecting(function(success){console.log(success);}, function(error){console.log(error);});

var img = new Image();
img.crossOrigin = "Anonymous";
img.onload = function () {
  var canvas = document.createElement('canvas');
  var ctx = canvas.getContext('2d');
  var dataURL;
  canvas.height = this.height;
  canvas.width = this.width;
  ctx.drawImage(this, 0, 0);
  dataURL = canvas.toDataURL("image/jpeg", 0.8);
  ImageDetectionPlugin.setPattern(dataURL, function(success){console.log(success);}, function(error){console.log(error);});
  canvas = null;
};
img.src = "img/patterns/target.jpg";

ImageDetectionPlugin.setDetectionTimeout(2, function(success){console.log(success);}, function(error){console.log(error);});
```
