/**
*
**/
var ARToolKitPlugin = function () {
  this.init = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ARToolKitPlugin", "init", []);
  };
  this.menu = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ARToolKitPlugin", "menu", []);
  };
  this.greet = function (name, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "ARToolKitPlugin", "greet", [name]);
  };
}

var artoolkitPlugin = new ARToolKitPlugin();
module.exports = artoolkitPlugin;
