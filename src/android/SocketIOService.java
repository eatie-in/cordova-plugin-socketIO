package com.vishnu.socketio;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


class SocketIOService {
    public static Bus bus = new Bus(ThreadEnforcer.MAIN);
    private static String TAG = "SOCKET_IO_SERVICE";
    private String name;
    private String url;
    private String query;
    private Socket socket;

    SocketIOService(String name, String url, String query) {
        this.name = name;
        this.url = url;
        this.query = query;
        this.connect();
    }

    public String getName() {
        return name;
    }

    public static void postStatus(String status) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                bus.post(status);
            }
        });
    }

    public static void postMessage(MessageEvent messageEvent) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                bus.post(messageEvent);
            }
        });
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

    private MessageEvent getMessage(Object arg, String event,Boolean showAlert) throws JSONException {
        MessageEvent messageEvent = new MessageEvent(name,event,arg,showAlert);
        return messageEvent;
    }

    public void listen(String event, CallbackContext callbackContext,Boolean showAlert) {
        socket.on(event, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject payload = parseData(args[0], event);
                    callbackContext.success(payload);
                    MessageEvent messageEvent = getMessage(args[0],event,showAlert);
                    postMessage(messageEvent);
//                    bus.post(payload);
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
                Log.i(TAG, "error");
            }
        });
    }



    private void onConnect() {
        this.socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.i(TAG, "connected");
               postStatus("connected");
            }
        });
    }

    private void onDisconnect() {
        this.socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String error = args[0].toString();
                Log.i(TAG, "disconnected");
                postStatus("disconnected");
            }
        });
    }

    private void onConnecting() {
        this.socket.on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String error = args[0].toString();
                Log.i(TAG, "Connecting...");
            }
        });
    }


    private void onConnectError() {
        this.socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String error = args[0].toString();
                Log.i(TAG, "Timeout");
            }
        });
    }
}