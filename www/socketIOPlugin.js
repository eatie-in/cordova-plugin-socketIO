var exec = require("cordova/exec");
const PLUGIN_NAME = "SocketIOPlugin";

module.exports = {
  connect: (options) =>
    new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "connect", [options]);
    }),
  disconnect: (socket) =>
    new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "disconnect", [socket]);
    }),

  disconnectAll: () =>
    new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "disconnectAll", []);
    }),

  addListener: (options) =>
    new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "addListener", [options]);
    }),

  removeListener: (options) =>
    new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "removeListener", [options]);
    }),
  emit: (options) =>
    new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "emit", [options]);
    }),
  hasOverlayPermission: () =>
    new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "hasOverlayPermission", []);
    }),
  requestOverlayPermission: () =>
    new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "requestOverlayPermission", []);
    }),

  onMessage: (successCallback, errorCallback) => {
    exec(successCallback, errorCallback, PLUGIN_NAME, "onMessage", []);
  },
  getStatus: (socketName) =>
    new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "getStatus", [socketName]);
    }),

  openBatterySettings: () => {
    return new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "openBatterySettings");
    });
  },
  isIgnoringBatteryOptimizations: () => {
    return new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "isIgnoringBatteryOptimizations");
    });
  },
  openAppStart: () => {
    return new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "openAppStart");
    });
  },

  requestTopPermissions: () => {
    return new Promise((resolve, reject) => {
      exec(resolve, reject, PLUGIN_NAME, "requestTopPermissions");
    });
  },
};
