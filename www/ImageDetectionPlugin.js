/**
*
**/
var ImageDetectionPlugin = function () {
  this.setPattern = function (pattern, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ImageDetectionPlugin", "greet", [pattern]);
  };
  this.init = function (pattern, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ImageDetectionPlugin", "init", [pattern]);
  };
  this.greet = function (name, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ImageDetectionPlugin", "greet", [name]);
  };
}

var imageDetectionPlugin = new ImageDetectionPlugin();
module.exports = imageDetectionPlugin;
