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


import android.app.Activity;
import android.content.Context;

import android.util.Log;


public class SocketIOPlugin extends CordovaPlugin {

    protected static final String TAG = "SocketIoPlugin";
    protected static SocketIOPlugin instance = null;
    public static Activity mActivity;
    public static Context mApplicationContext;
    protected static CallbackContext mCallbackContext;
    public static CordovaWebView mWebView;

    private static CallbackContext mMessageCallbackContext;
    public static String messageReceivedCallback = "cordova.plugins.socketIO.onMessageReceived";

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        mWebView = webView;
        mActivity = cordova.getActivity();
        mApplicationContext = mActivity.getApplicationContext();
        super.initialize(cordova, webView);
        Log.i(TAG, "initialize: ");
    }


    @Override
    public void onDestroy() {
        mMessageCallbackContext = null;
        super.onDestroy();
    }

    @Override
    protected void pluginInitialize() {
        Log.i(TAG, "pluginInitialize:");
        SocketIOService.getUndelivered();
        super.pluginInitialize();
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("onMessage")) {
            mMessageCallbackContext = callbackContext;
            this.onSocketMessage();
        } else {
            mCallbackContext = callbackContext;
            if (action.equals("connect")) {
                this.connect(args);
            } else if (action.equals("emit")) {
                this.emit(args);
            } else if (action.equals("disconnect")) {
                this.disconnect(args);
            } else if (action.equals("disconnectAll")) {
                this.disconnectAll();
            } else if (action.equals("addListener")) {
                this.addListener(args);
            } else if (action.equals("removeListener")) {
                this.removeListener(args);
            } else {
                callbackContext.error("invalid");
                return false;
            }
        }
        return true;
    }

    private void onSocketMessage() {
        sendPluginResultAndKeepCallback("ok", mMessageCallbackContext);
    }

    private void connect(final JSONArray args) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String name = options.getString("name");
        String url = options.getString("url");
        String query = options.getString("query");
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SocketIOService.connect(name, url, query);
            }
        });
    }

    private void addListener(final JSONArray args) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String socketName = options.getString("name");
        String eventName = options.getString("event");
        Boolean showAlert = options.has("alert") && options.getBoolean("alert");
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SocketIOService.addListener(socketName, eventName, showAlert);
            }
        });
    }

    private void removeListener(final JSONArray args) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String socketName = options.getString("name");
        String eventName = options.getString("event");
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SocketIOService.removeListener(socketName, eventName);
            }
        });
    }

    private void emit(JSONArray args) throws JSONException {
        JSONObject options = args.getJSONObject(0);
        String socketName = options.getString("name");
        String eventName = options.getString("event");
        Object payload = options.getJSONObject("data");
        Log.i(TAG, "emit: name:" + socketName + "event: " + eventName);
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SocketIOService.emit(socketName, eventName, payload);
            }
        });
    }

    private void disconnect(JSONArray args) throws JSONException {
        String name = args.getString(0);
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SocketIOService.disconnect(name);
            }
        });
    }

    private void disconnectAll() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SocketIOService.disconnectAll();
            }
        });
    }

    private static void executeGlobalJavascript(final String jsString) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl("javascript:" + jsString);
            }
        });
    }

    public static void onData(Object payload) {
        if (mMessageCallbackContext == null) {
            Log.w(TAG, "sendConnectionStatus:null");
        } else {
            Log.i(TAG, "onData: sending througth callback");
            sendPluginResultAndKeepCallback(payload.toString(), mMessageCallbackContext);
        }
    }


    protected static void sendPluginResultAndKeepCallback(String result, CallbackContext callbackContext) {
        PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, result);
        sendPluginResultAndKeepCallback(pluginresult, callbackContext);
    }

    protected static void sendPluginResultAndKeepCallback(JSONObject result, CallbackContext callbackContext) {
        PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, result);
        sendPluginResultAndKeepCallback(pluginresult, callbackContext);
    }

    protected static void sendPluginResultAndKeepCallback(PluginResult pluginresult, CallbackContext callbackContext) {
        pluginresult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginresult);
    }
}
