var argscheck = require("cordova/argscheck");
var exec = require("cordova/exec");
const PLUGIN_NAME = "SocketIOPlugin";


function socketIOPlugin() {}

socketIOPlugin.prototype.connect = (
  options,
  successCallback,
  errorCallback
) => {
  exec(successCallback, errorCallback, PLUGIN_NAME, "connect", [options]);
};

socketIOPlugin.prototype.addListener = (
  options,
  successCallback,
  errorCallback
) => {
  exec(successCallback, errorCallback, PLUGIN_NAME, "addListener", [options]);
};

socketIOPlugin.prototype.onMessage = (callback) => {
  socketIOPlugin.prototype.onMessageReceived = callback;
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
