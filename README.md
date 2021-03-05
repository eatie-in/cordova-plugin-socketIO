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

```js
await cordova.plugins.socketio.connect({
  name: String,
  url: String,
  token: String, // for authentication,it cannot be null
});
```

### disconnect(_type_)

Terminates the connection of socket.

```js
await cordova.plugins.socketio.disconnect({
  name: String, // name of socket
});
```

### disconnectAll(_type_)

Terminates All sockets connections and stops foreground service

```js
await cordova.plugins.socketio.disconnect();
```

### addListener(_type_)

Add the listener. For alert please make sure overlay permissions are granted on android 10

```js
await cordova.plugins.socketio.addListener({
  name: String, // name of socket
  event: String,
  alert: Boolean, // show the alert when app is in background or removed from recent apps
});
```

### removeListener(_type_)

removes the listener

```js
await cordova.plugins.socketio.removeListener({
  name: String, // name of socket
  event: String,
});
```

### emit(_type_)

emits event

```js
await cordova.plugins.socketio.emit({
  name: String, // name of socket
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
```
