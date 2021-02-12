var argscheck = require("cordova/argscheck");
var exec = require("cordova/exec");
const PLUGIN_NAME = "SocketIOPlugin";

function socketIOPlugin() { }

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

socketIOPlugin.prototype.onMessage = (successCallback,errorCallback) => {
  // // socketIOPlugin.prototype.onMessageReceived = callback;
  // return new Promise((resolve, reject) => {
  //   // socketIOPlugin.prototype.onMessageReceived = resolve;
  //   exec(resolve, reject, PLUGIN_NAME, "onMessage", []);
  // });

  exec(successCallback, errorCallback, PLUGIN_NAME, "onMessage", []);
};

socketIOPlugin.prototype.onMessageReceived = (payload) => {
  console.log("[RECEIVED]", payload);
};

module.exports = new socketIOPlugin();

// module.exports = {
//   test: function (text, errorCallback, successCallback) {
//     exec(successCallback, errorCallback, PLUGIN_NAME, "test", [text]);
//   },

//   connect: (options, successCallback, errorCallback) => {
//     exec(successCallback, errorCallback, PLUGIN_NAME, "connect", [options]);
//   },

//   addListener: (options, successCallback, errorCallback) => {
//     exec(successCallback, errorCallback, PLUGIN_NAME, "addListener", [options]);
//   },

//   removeListener: (options, successCallback, errorCallback) => {
//     exec(successCallback, errorCallback, PLUGIN_NAME, "addListener", [options]);
//   },

//   disconnect: (options, successCallback, errorCallback) => {
//     exec(successCallback, errorCallback, PLUGIN_NAME, "connect", [options]);
//   },
//   onMessage: (callback) => {
//     this.onMessageReceived = callback;
//     console.log(callback, "red", this.onMessageReceived);
//   },
//   onMessageReceived: (payload) => {
//     console.log("messageReceived");
//     console.log(payload);
//   },
//   emit: (options, successCallback, errorCallback) => {
//     exec(successCallback, errorCallback, PLUGIN_NAME, "emit", [options]);
//   },
// };
