package com.vishnu.socketio;

import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Collections;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.client.SocketOptionBuilder;
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

    private IO.Options getOptions(String token) {
        SocketOptionBuilder builder = IO.Options.builder();
        builder.setAuth(Collections.singletonMap("token", token));
        builder.setTimeout(10000);
        builder.setReconnection(true);
        builder.setForceNew(true);
        return builder.build();
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

    private JSONObject parseData(String event) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("socket", name);
        payload.put("event", event);
        return payload;
    }

    public void addListener(String event, Boolean showAlert) {
        socket.on(event, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    Log.i(TAG, "call: " + args[0].toString());
                    JSONObject payload = parseData(args[0], event);
                    SocketIOService.sendMessage(payload, showAlert);
                } catch (JSONException e) {
                    e.printStackTrace();
                    SocketIOService.sendMessage(e.getMessage(), showAlert);
                }
            }
        });
    }

    public void removeListener(String event) {
        socket.off(event);
    }

    public void emit(String event, Object data) {
        this.socket.emit(event, data);
    }


    private void onError() {
        this.socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args.length > 0) {
                    try {
                        JSONObject status = parseData(args[0].toString(), Socket.EVENT_CONNECT_ERROR);
                        SocketIOService.sendMessage(status);
                        SocketIOService.updateStatus("Connecting...");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        SocketIOService.sendMessage(e.getMessage());
                    }
                }
            }
        });
    }

    private void onConnect() {
        this.socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject status = parseData(Socket.EVENT_CONNECT);
                    SocketIOService.sendMessage(status);
                    SocketIOService.updateStatus("Connected");
                } catch (JSONException e) {
                    e.printStackTrace();
                    SocketIOService.sendMessage(e.getMessage());
                }
            }
        });
    }

    private void onDisconnect() {
        this.socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject status = parseData(Socket.EVENT_DISCONNECT);
                    SocketIOService.sendMessage(status);
                    SocketIOService.updateStatus("Disconnected");
                } catch (JSONException e) {
                    e.printStackTrace();
                    SocketIOService.sendMessage(e.getMessage());
                }
            }
        });
    }

}