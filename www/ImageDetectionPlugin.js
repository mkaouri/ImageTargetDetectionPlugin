/**
*
**/
var ImageDetectionPlugin = function () {
  this.startProcessing = function (bool, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ImageDetectionPlugin", "startProcessing", [bool]);
  };
  this.setPattern = function (pattern, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ImageDetectionPlugin", "setPattern", [pattern]);
  };
  this.isDetecting = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ImageDetectionPlugin", "isDetecting", []);
  };
  this.setDetectionTimeout = function (timeout, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ImageDetectionPlugin", "setDetectionTimeout", [timeout]);
  };
  this.greet = function (name, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ImageDetectionPlugin", "greet", [name]);
  };
}

var imageDetectionPlugin = new ImageDetectionPlugin();
module.exports = imageDetectionPlugin;
