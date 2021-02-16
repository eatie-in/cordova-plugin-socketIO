var argscheck = require("cordova/argscheck");
var exec = require("cordova/exec");
const PLUGIN_NAME = "SocketIOPlugin";

function socketIOPlugin() {}

socketIOPlugin.prototype.connect = (options) =>
  new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "connect", [options]);
  });

socketIOPlugin.prototype.disconnect = (socket) =>
  new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "disconnect", [socket]);
  });

socketIOPlugin.prototype.disconnectAll = () =>
  new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "disconnectAll", []);
  });

socketIOPlugin.prototype.addListener = (options) =>
  new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "addListener", [options]);
  });

socketIOPlugin.prototype.removeListener = (options) =>
  new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "removeListener", [options]);
  });

socketIOPlugin.prototype.emit = (options) =>
  new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "emit", [options]);
  });

socketIOPlugin.prototype.hasOverlayPermission = () =>
  new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "hasOverlayPermission", []);
  });

socketIOPlugin.prototype.requestOverlayPermission = () =>
  new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "requestOverlayPermission", []);
  });

socketIOPlugin.prototype.onMessage = (successCallback, errorCallback) => {
  exec(successCallback, errorCallback, PLUGIN_NAME, "onMessage", []);
};

module.exports = new socketIOPlugin();
