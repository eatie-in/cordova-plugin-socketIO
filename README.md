## Installation

    $ cordova plugin add https://github.com/eatie-in/cordova-plugin-socketIO.git

NOTE: Required Socket.io v3 server

## Default events

```
connect,disconnect,connect_error
```

## Methods

### onMessage(_callback_)

Called when a socket.io server message received while app is in foreground.

```js
cordova.plugins.socketio.onMessage(function (payload) {
  console.log("data ", payload);
});
```

### connect(_type_)

Connect to server and start foreground service on android
path(optional) if not passed uses default

```js
await cordova.plugins.socketio.connect({
  path?: String,
  url: String,
  token: String, // for authentication,it cannot be null
});
```

### disconnect(_type_)

Terminates the connection of socket.

```js
await cordova.plugins.socketio.disconnect();
```


### addListener(_type_)

Add the listener. For alert please make sure overlay permissions are granted on android 10. To play custom sound place the audio in `platforms/android/app/src/main/res/drawable`

When alert option is true show the alert with vibration and sound

```js
await cordova.plugins.socketio.addListener({
  event: String,
  alert: Boolean, // show the alert when app is in background or removed from recent apps or in foreground
});
```

### removeListener(_type_)

removes the listener

```js
await cordova.plugins.socketio.removeListener({
  event: String,
});
```

### emit(_type_)

emits event

```js
await cordova.plugins.socketio.emit({
  event: String,
  data: {}, // should be object
});
```

### hasOverlayPermissions(_type_)

Check the app has overlay permission

```js
await cordova.plugins.socketio.hasOverlayPermission();
```

### requestOverlayPermissions(_type_)

show the overlay permission screen of app

```js
await cordova.plugins.socketio.requestOverlayPermission();
```

### getStatus(_type_)

Get the connection status of socket

```js
await cordova.plugins.socketio.getStatus(socket);
// returns boolean
```

### openBatterySettings
```js
await cordova.plugins.socketio.openBatterySettings();
```


### isIgnoringBatteryOptimizations
```js
await cordova.plugins.socketio.isIgnoringBatteryOptimizations();
```
### openAppStart
opens app auto start settings
```js
await cordova.plugins.socketio.openAppStart();
```
