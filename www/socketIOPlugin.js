const exec = require("cordova/exec");

const PLUGIN_NAME = "SocketIOPlugin";
const defaultEvents = ["connect_error", "disconnect", "connect"];


function connect(options) {
  return new Promise((resolve, reject) => {
    let path = "/socket.io";
    if (!options["url"]) {
      return reject("url required");
    }
    if (!options["token"]) {
      return reject("token required");
    }
    if (!options["path"]) {
      options["path"] = path;
    }
    exec(resolve, reject, PLUGIN_NAME, "connect", [options]);
  });
}

function disconnect() {
  return new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "disconnect", []);
  });
}

function addListener(options) {
  return new Promise((resolve, reject) => {
    let alert = false;
    if (!options["event"]) {
      return reject("event name required");
    }
    if (options.alert == undefined) {
      options["alert"] = alert;
    }
    exec(resolve, reject, PLUGIN_NAME, "addListener", [options]);
  });
}

function removeListener(event) {
  return new Promise((resolve, reject) => {
    if (!event) {
      return reject(`event name required`);
    }
    exec(resolve, reject, PLUGIN_NAME, "removeListener", [event]);
  });
}

function emit(options) {
  return new Promise((resolve, reject) => {
    const data = {};
    if (!options["event"]) {
      return reject("event name required");
    }
    if (defaultEvents.includes(options["event"])) {
      return reject(`${options["event"]} is reserved event`);
    }

    if (!options["data"]) {
      options["data"] = data;
    }
    exec(resolve, reject, PLUGIN_NAME, "emit", [options]);
  });
}

function hasOverlayPermission() {
  return new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "hasOverlayPermission", []);
  });
}

function onMessage(successCallback, errorCallback) {
  exec(successCallback, errorCallback, PLUGIN_NAME, "onMessage", []);
}

function requestOverlayPermission() {
  return new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "requestOverlayPermission", []);
  });
}

function getStatus() {
  return new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "getStatus", []);
  });
}

function openBatterySettings() {
  return new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "openBatterySettings");
  });
}

function isIgnoringBatteryOptimizations() {
  return new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "isIgnoringBatteryOptimizations");
  });
}

function openAppStart() {
  return new Promise((resolve, reject) => {
    exec(resolve, reject, PLUGIN_NAME, "openAppStart");
  });
}

module.exports = {
  connect,
  disconnect,
  addListener,
  removeListener,
  emit,
  hasOverlayPermission,
  requestOverlayPermission,
  onMessage,
  getStatus,
  openBatterySettings,
  isIgnoringBatteryOptimizations,
  openAppStart,
};
