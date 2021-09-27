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
import android.content.ComponentName;
import android.content.Context;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.POWER_SERVICE;
import static android.content.pm.PackageManager.MATCH_DEFAULT_ONLY;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS;


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
            } else if (action.equals("getStatus")) {
                this.getStatus(args);
            } else if (action.equals("requestTopPermissions")) {
                this.requestTopPermissions();
            } else if (action.equals("isIgnoringBatteryOptimizations")) {
                this.isIgnoringBatteryOptimizations();
            } else if (action.equals("openBatterySettings")) {
                this.openBatterySettings();
            } else if (action.equals("openAppStart")) {
                this.openAppStart();
            } else {
                callbackContext.error("invalid");
                return false;
            }
        }
        return true;
    }

    private void getStatus(final JSONArray args) {
        try {
            String socketName = args.getString(0);
            if (socketName == null) {
                mCallbackContext.error("socket name is required");
                return;
            }
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    SocketIOService.getStatus(socketName);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            mCallbackContext.error(e.toString());
        }
    }

    private void onSocketMessage() {
        SocketIOService.getUndelivered();
        sendPluginResultAndKeepCallback("ok", mMessageCallbackContext);
    }


    private void connect(final JSONArray args) {
        try {
            JSONObject options = args.getJSONObject(0);
            String name = options.getString("name");
            String url = options.getString("url");
            String token = options.getString("token");
            String path = options.has("path")? options.getString("path"):"/socket.io";
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    SocketIOService.connect(name, url, token,path);
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


    private void hasOverlayPermission() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Boolean hasPermission = false;
                if (SDK_INT >= M) {
                    hasPermission = Settings.canDrawOverlays(mApplicationContext);
                }
                Log.i(TAG, "hasoverlayPermission: " + hasPermission);
                PluginResult res = new PluginResult(PluginResult.Status.OK, hasPermission);
                mCallbackContext.sendPluginResult(res);
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

    private void requestTopPermissions() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if (SDK_INT >= M) {
                    if (Settings.canDrawOverlays(mApplicationContext)) {
                        return;
                    }
                    String pkgName = mActivity.getPackageName();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + pkgName));
                    mActivity.startActivity(intent);
                }
            }
        });
    }

    private void isIgnoringBatteryOptimizations() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if (SDK_INT < M)
                    return;

                Activity activity = cordova.getActivity();
                String pkgName = activity.getPackageName();
                PowerManager pm = (PowerManager) getService(POWER_SERVICE);
                boolean isIgnoring = pm.isIgnoringBatteryOptimizations(pkgName);
                PluginResult res = new PluginResult(PluginResult.Status.OK, isIgnoring);
                mCallbackContext.sendPluginResult(res);
            }
        });
    }

    private Object getService(String name) {
        return mActivity.getSystemService(name);
    }

    private void openBatterySettings() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if (SDK_INT < M)
                    return;
                Intent intent = new Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                mActivity.startActivity(intent);
            }
        });
    }

    private void openAppStart() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PackageManager pm = mActivity.getPackageManager();
                for (Intent intent : getAppStartIntents()) {
                    if (pm.resolveActivity(intent, MATCH_DEFAULT_ONLY) != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mActivity.startActivity(intent);
                        break;
                    }
                }
            }
        });
    }

    /**
     * Returns list of all possible intents to present the app start settings.
     */
    private List<Intent> getAppStartIntents() {
        return Arrays.asList(
                new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                new Intent().setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
                new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
                new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
                new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
                new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
                new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
                new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
                new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")),
                new Intent().setComponent(new ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")).setData(android.net.Uri.parse("mobilemanager://function/entry/AutoStart")),
                new Intent().setAction("com.letv.android.permissionautoboot"),
                new Intent().setComponent(new ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.ram.AutoRunActivity")),
                new Intent().setComponent(ComponentName.unflattenFromString("com.iqoo.secure/.MainActivity")),
                new Intent().setComponent(ComponentName.unflattenFromString("com.meizu.safe/.permission.SmartBGActivity")),
                new Intent().setComponent(new ComponentName("com.yulong.android.coolsafe", ".ui.activity.autorun.AutoRunListActivity")),
                new Intent().setComponent(new ComponentName("cn.nubia.security2", "cn.nubia.security.appmanage.selfstart.ui.SelfStartActivity")),
                new Intent().setComponent(new ComponentName("com.zui.safecenter", "com.lenovo.safecenter.MainTab.LeSafeMainActivity"))
        );
    }
}
