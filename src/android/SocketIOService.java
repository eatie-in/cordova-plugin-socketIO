package com.vishnu.socketio;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.socket.client.Socket;


public class SocketIOService extends Service {
    private static String CHANNEL_ID = "SocketIO";
    private static String TAG = SocketIOService.class.getName();
    private boolean mRunning = false;
    private static String connectionStatus = "Connecting..";

    private static String defaultSmallIconName = "notification_icon";
    private static int defaultSmallIconResID = 0;

    private static ArrayList<JSONObject> mUndeliveredMessages = new ArrayList<JSONObject>();

    private static final ArrayList<String> socketListeners = new ArrayList<String>();

    private static SocketIO mSocket;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (mSocket == null) {
            Log.i(TAG, "onTaskRemoved: " + "don't know what will happen");
            return;
        }
        Log.d(TAG, "onTaskRemoved " + "may be restarting service");
        Intent restartService = new Intent(getApplicationContext(), this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), restartServicePI);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = createNotification(connectionStatus);
        startForeground(2, notification);
        return START_STICKY_COMPATIBILITY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public static Notification createNotification(String text) {
        Context context = SocketIOPlugin.mApplicationContext;
        PackageManager pm = context.getPackageManager();
        Intent notificationIntent = pm.getLaunchIntentForPackage(context.getPackageName());
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("App is running")
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        int defaultSmallIconResID = context.getResources().getIdentifier(defaultSmallIconName, "drawable", context.getPackageName());
        if (defaultSmallIconResID != 0) {
            notification.setSmallIcon(defaultSmallIconResID);
        } else {
            notification.setSmallIcon(context.getApplicationInfo().icon);
        }
        notification.setOngoing(true);
        return notification.build();
    }

    public static void updateNotification(String text) {
        if (mSocket == null) {
            Log.w(TAG, "updateNotification: " + "socket not ini");
            return;
        }
        Context context = SocketIOPlugin.mApplicationContext;
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        Notification notification = createNotification(text);
        managerCompat.notify(2, notification);
    }


    public static void updateStatus(String status) {
        updateNotification(status);
        connectionStatus = status;
    }

    public static void getUndelivered() {
        if (mUndeliveredMessages.size() >= 1) {
            for (JSONObject message : mUndeliveredMessages) {
                SocketIOPlugin.onData(message);
            }
            mUndeliveredMessages.clear();
        } else {
            Log.i(TAG, "getUndelivered: no undelivered");
        }
    }


    private static void start() {
        Context context = SocketIOPlugin.mApplicationContext;
        Log.d(TAG, "NotificationSocketService STARTED");
        // remove all notifications from status bar. This method gets called when app is started
        NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancelAll();
        Intent intent = new Intent(context, SocketIOService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private static void stop() {
        Context context = SocketIOPlugin.mApplicationContext;
        Log.d(TAG, "NotificationSocketService STOPPED");
        Intent intent = new Intent(context, SocketIOService.class);
        context.stopService(intent);
    }

    public static void addDefaultListeners() {
        socketListeners.add("connect");
        socketListeners.add("disconnect");
        socketListeners.add("connect_error");
    }

    public static void connect(String url, String query, String path) {
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        if (mSocket != null) {
            String error = "Already connected";
            callbackContext.success(error);
            return;
        }
        SocketIO socketIO = new SocketIO(url, query, path);
        mSocket = socketIO;
        addDefaultListeners();
        // start service
        start();
        callbackContext.success();
    }


    public static void removeListener(String event) {
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        if (mSocket == null) {
            String error = "Socket not initialized";
            callbackContext.error(error);
            return;
        }
        mSocket.removeListener(event);
        socketListeners.remove(event);
        callbackContext.success();
    }

    public static void addListener(String event, Boolean alert) {
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        if (mSocket == null) {
            String error = "Socket not initialized";
            Log.w(TAG, "addListener: " + error);
            callbackContext.error(error);
            return;
        }
        if (socketListeners.contains(event)) {
            String error = event + " already registered";
            Log.w(TAG, "addListener: " + error);
            callbackContext.error(error);
            return;
        }
        mSocket.addListener(event, alert);
        socketListeners.add(event);
        String message = event + " listener added";
        callbackContext.success(message);
    }


    public static void emit(String event, JSONObject data) {
        SocketIO socketConnection = mSocket;
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        if (socketConnection == null) {
            String error = "Socket not initialized";
            callbackContext.error(error);
            return;
        }
        socketConnection.emit(event, data);
        callbackContext.success();
    }

    public static void disconnect() {
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        if (mSocket == null) {
            Log.w(TAG, "disconnect: Already disconnected");
            callbackContext.success();
            return;
        }
        mSocket.disconnect();
        mSocket = null;
        socketListeners.clear();
        stop();
        Log.i(TAG, "disconnect: stopping service");
        callbackContext.success();
    }


    public static void getStatus() {
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        SocketIO socketConnection = mSocket;
        if (socketConnection == null) {
            callbackContext.success("false");
            return;
        }
        String status = socketConnection.isConnected().toString();
        callbackContext.success(status);
    }


    private static void sendAcknowledgement(JSONObject payload) {
        try{
            String event = payload.getString("event");
            JSONObject data = null;
            if(!payload.has("data") || !Utils.isJSONValid(payload.getString("data"))){
                Log.w(TAG, "sendAcknowledgement: " + "no ack: " + event );
                return;
            }
            data = payload.getJSONObject("data");
            String id = null;
            if(data.has("id")){
                id = data.getString("id");
            }
            if(id !=null){
                Log.i(TAG, "sendAcknowledgement: " + "id: " + id +" event: " + event);
                JSONObject body = new JSONObject();
                body.put("event",event);
                body.put("id",id);
                mSocket.emit(event,body);
                return;
            }
            Log.w(TAG, "sendAcknowledgement: " + "no ack");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void sendMessage(JSONObject message, Boolean showAlert) {
         sendAcknowledgement(message);
        if (!isMainAppForeground() && showAlert) {
            mUndeliveredMessages.add(message);
            Log.i(TAG, "sendMessage: " + "saved to undelivered");
            SocketIOPlugin.utils.startActivity();
            return;
        }
        SocketIOPlugin.onData(message);
        Log.w(TAG, "sendMessage: " + showAlert + message.toString());
        if (showAlert) {
            SocketIOPlugin.utils.showAlert(false);
        }
    }

    public static void sendMessage(String message, Boolean showAlert) {
        if (!isMainAppForeground() && showAlert) {
            SocketIOPlugin.utils.startActivity();
            Log.i(TAG, "sendMessage: " + "App not active");
            return;
        }
        SocketIOPlugin.onData(message);
    }

    public static void sendMessage(JSONObject payload) {
        if (isMainAppForeground()) {
            SocketIOPlugin.onData(payload);
        }
    }

    public static void sendMessage(String payload) {
        if (!isMainAppForeground()) {
            SocketIOPlugin.onData(payload);
        }
    }

    private static boolean isMainAppForeground() {
        boolean isMainAppForeground = false;
        Context context = SocketIOPlugin.mApplicationContext;
        Activity activity = SocketIOPlugin.mActivity;
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

        boolean isPhoneLocked = km.inKeyguardRestrictedInputMode();
        boolean isSceenAwake = (Build.VERSION.SDK_INT < 20 ? pm.isScreenOn() : pm.isInteractive());

        List<ActivityManager.RunningAppProcessInfo> runningProcessInfo = activityManager.getRunningAppProcesses();
        if (runningProcessInfo != null && !Utils.isAlertActive) {
            for (ActivityManager.RunningAppProcessInfo appProcess : runningProcessInfo) {
                Log.d("NotificationService", String.valueOf(appProcess.importance));
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        && appProcess.processName.equals(activity.getApplication().getPackageName())
                        && !isPhoneLocked && isSceenAwake) {
                    isMainAppForeground = true;
                    break;
                }
            }
        }
        return isMainAppForeground;
    }
}
