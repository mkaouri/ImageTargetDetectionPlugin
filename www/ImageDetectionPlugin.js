/**
*
**/
var ImageDetectionPlugin = function () {
  this.setPattern = function (pattern, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ImageDetectionPlugin", "setPattern", [pattern]);
  };
  this.isDetecting = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ImageDetectionPlugin", "isDetecting", []);
  };
  this.greet = function (name, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ImageDetectionPlugin", "greet", [name]);
  };
}

var imageDetectionPlugin = new ImageDetectionPlugin();
module.exports = imageDetectionPlugin;
