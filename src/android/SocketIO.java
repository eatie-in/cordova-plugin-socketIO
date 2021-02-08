/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package com.vishnu.socketio;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.CordovaWebView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketIO extends CordovaPlugin {

    protected static final String TAG = "SocketIoPlugin";
    protected static Context applicationContext = null;
    private static Activity cordovaActivity = null;
    protected static SocketIO instance = null;
    private boolean isForeground;


    private static Map<String, SocketIOService> connections = new HashMap<String, SocketIOService>();

    protected static CallbackContext mcallbackContext;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

    }

    @Override
    protected void pluginInitialize() {
        instance = this;
        cordovaActivity = this.cordova.getActivity();
        applicationContext = cordovaActivity.getApplicationContext();
        Log.i(TAG, "pluginInitialize:");
        super.pluginInitialize();
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        mcallbackContext = callbackContext;
        if (action.equals("connect")) {
            this.connect(args);
        } else if (action.equals("listen")) {
            this.listen(args);
        }
        return true;
    }

    @Subscribe
    private void onMessage() {

    }

    private void connect(JSONArray args) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String name = options.getString("name");
        String url = options.getString("url");
        String query = options.getString("query");
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SocketIOService connection = connections.get(name);
                if (connection != null) {
                    mcallbackContext.success("already connected");
                } else {
                    SocketIOService socketIOService = new SocketIOService(name, url, query);
                    connections.put(name, socketIOService);
                    if (!isForeground) {
                        Intent intent = new Intent(cordova.getContext(), ForegroundService.class);
                        ContextCompat.startForegroundService(cordova.getContext(), intent);
                    }
                    isForeground = true;
                    mcallbackContext.success("connected");
                }
            }
        });
    }

    private void listen(JSONArray args) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String socketName = options.getString("name");
        String eventName = options.getString("event");
        Boolean showAlert = options.getBoolean("alert");
        Log.i(TAG, "listen: name:" + socketName + "event:" + eventName);
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SocketIOService connection = connections.get(socketName);
                if (connection != null) {
                    connection.listen(eventName,
                            mcallbackContext, showAlert);
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "listening" + eventName);
                    pluginResult.setKeepCallback(true);
                    mcallbackContext.sendPluginResult(pluginResult);
                } else {
                    mcallbackContext.error("First Please connect");
                }
            }
        });
    }

    private void emit(JSONArray args) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String socketName = options.getString("name");
        String eventName = options.getString("event");
        Object payload = options.getJSONObject("data");
        Log.i(TAG, "emit: name:" + socketName + "event:" + eventName);
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SocketIOService connection = connections.get(socketName);
                if (connection != null) {
                    Log.i(TAG, "run: " + connection.getName());
                    connection.emit(eventName,
                            payload);
                    mcallbackContext.success("done");
                } else {
                    mcallbackContext.error("No socket found");
                }
            }
        });
    }

    private void disconnect(JSONArray args) throws JSONException {
        String name = args.getString(0);
        SocketIOService connection = connections.get(name);
        connection.disconnect();
    }

    private void disconnectAll() {
        for (Map.Entry<String, SocketIOService> entry : connections.entrySet()) {
            SocketIOService connection = entry.getValue();
            connection.disconnect();
        }
        mcallbackContext.success("disconnected");
    }

    private void test(CallbackContext callbackContext, String text) {
        callbackContext.success(text);
    }



    @Subscribe
    public void onMessage(MessageEvent messageEvent){
        if(messageEvent.getShowAlert()){
//         this.showAlert(messageEvent.getData().toString());
//            PackageManager pm = getPackageManager();
//            Intent notificationIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
//            startActivity(notificationIntent);
            mcallbackContext.success(messageEvent.toString());
        }else {
            Log.i(TAG, "onMessage: "+"normal");
        }
    }
}
