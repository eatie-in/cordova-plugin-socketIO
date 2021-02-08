package com.vishnu.socketio;

import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


class SocketIO {
    private static String TAG = "SOCKET_IO_SERVICE";
    private String name;
    private String url;
    private String query;
    private Socket socket;

    SocketIO(String name, String url, String query) {
        this.name = name;
        this.url = url;
        this.query = query;
        this.connect();
    }

    public String getName() {
        return name;
    }

    private IO.Options getOptions(String query) {
        IO.Options options = new IO.Options();
        options.query = query;
        options.forceNew = true;
        options.reconnection = true;
        return options;
    }

    public void connect() {
        IO.Options options = this.getOptions(query);
        URI uri = URI.create(url);
        socket = IO.socket(uri, options);
        socket.connect();
        this.registerDefaultListeners();
    }

    private void registerDefaultListeners() {
        this.onConnect();
        this.onError();
        this.onDisconnect();
    }

    public void disconnect() {
        if (socket != null)
            socket.disconnect();
    }


    private JSONObject parseData(Object arg, String event) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("socket", name);
        payload.put("event", event);
        payload.put("data", arg);
        return payload;
    }

    public void addListener(String event, Boolean showAlert) {
        socket.on(event, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject payload = parseData(args[0], event);
                    SocketIOService.sendMessage(payload, showAlert);
                } catch (JSONException e) {
                    e.printStackTrace();
                    SocketIOService.sendMessage(e.getMessage(), showAlert);
                }
            }
        });
    }

    public void removeListener() {

    }

    public void listen(String event, CallbackContext callbackContext, Boolean showAlert) {
        socket.on(event, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject payload = parseData(args[0], event);
//                    SocketIOService.sendMessage(payload, callbackContext, showAlert);
                } catch (JSONException e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public void emit(String event, Object data) {
        this.socket.emit(event, data);
    }

    private void onError() {
        this.socket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String error = args[0].toString();
                Log.i(TAG, error);
                SocketIOService.updateStatus(error);
            }
        });
    }

    private void onConnect() {
        this.socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.i(TAG, "connected");
                SocketIOService.updateStatus("connected");
            }
        });
    }

    private void onDisconnect() {
        this.socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.i(TAG, "disconnected");
                SocketIOService.updateStatus("Disconnected");
            }
        });
    }

}