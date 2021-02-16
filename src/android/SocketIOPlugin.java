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

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.Manifest;
import android.app.Activity;
import android.content.Context;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import static android.app.Activity.RESULT_OK;


public class SocketIOPlugin extends CordovaPlugin {
    public static final String READ = Manifest.permission.SYSTEM_ALERT_WINDOW;
    public static final int SEARCH_REQ_CODE = 0;
    protected static final String TAG = "SocketIoPlugin";
    public static Activity mActivity;
    public static Context mApplicationContext;
    protected static CallbackContext mCallbackContext;
    private static CallbackContext mMessageCallbackContext;
    private static final int OVERLAY_REQUEST_CODE = 5;

    @Override
    public void onDestroy() {
        mMessageCallbackContext = null;
        mCallbackContext = null;
        super.onDestroy();
    }
//
//    @Override
//    public Bundle onSaveInstanceState() {
//        return super.onSaveInstanceState();
//    }


    @Override
    public void onReset() {
        mMessageCallbackContext = null;
        mCallbackContext = null;
        super.onReset();
    }

    @Override
    protected void pluginInitialize() {
        Log.i(TAG, "pluginInitialize:");
        mActivity = cordova.getActivity();
        mApplicationContext = mActivity.getApplicationContext();
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
            } else if (action.equals("hasOverlayPermission")) {
                this.hasOverlayPermission();
            } else if (action.equals("requestOverlayPermission")) {
                this.requestOverlayPermission();
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
        SocketIOService.getUndelivered();
        sendPluginResultAndKeepCallback("ok", mMessageCallbackContext);
    }

    private void hasOverlayPermission() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Boolean hasPermission = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        hasPermission = Settings.canDrawOverlays(mApplicationContext);
                    }
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("hasPermission", hasPermission);
                    Log.i(TAG, "hasoverlayPermission: " + hasPermission);
                    mCallbackContext.success(jsonObject);
                } catch (JSONException e) {
                    mCallbackContext.error(e.toString());
                }
            }
        });
    }

    private void requestOverlayPermission() {
        CordovaPlugin instance = this;
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                mCallbackContext.success();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + mApplicationContext.getPackageName()));
                cordova.startActivityForResult(instance, intent, OVERLAY_REQUEST_CODE);
            }
        });
    }


    private void connect(final JSONArray args) {
        try {
            JSONObject options = args.getJSONObject(0);
            String name = options.getString("name");
            String url = options.getString("url");
            String token = options.getString("token");
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    SocketIOService.connect(name, url, token);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            mCallbackContext.error(e.toString());
        }
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

    public static void onData(Object payload) {
        if (mMessageCallbackContext == null) {
            Log.w(TAG, "no callbackContext");
        } else {
            Log.i(TAG, "onData: sending data");
            sendPluginResultAndKeepCallback(payload.toString(), mMessageCallbackContext);
        }
    }

    public static void onData(JSONObject payload) {
        if (mMessageCallbackContext == null) {
            Log.w(TAG, "no callbackContext");
        } else {
            Log.i(TAG, "onData: sending data");
            sendPluginResultAndKeepCallback(payload, mMessageCallbackContext);
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
