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


public class SocketIOService extends Service {
    private static String CHANNEL_ID = "SocketIO";
    private static String TAG = "SocketIOService";
    private boolean mRunning = false;
    private static String connectionStatus = "Connecting..";

    private static String defaultSmallIconName = "notification_icon";
    private static int defaultSmallIconResID = 0;

    private static ArrayList<JSONObject> mUndeliveredMessages = new ArrayList<JSONObject>();

    private static final Map<String, SocketIO> socketConnections = new HashMap<String, SocketIO>();
    private static final Map<String, ArrayList<String>> socketListeners = new HashMap<String, ArrayList<String>>();


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (socketConnections.size() < 1) {
            return;
        }
        Log.d("NotificationService", "onTaskRemoved METHOD");
        Intent restartService = new Intent(getApplicationContext(), this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePI);
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
                .setContentTitle("Connection")
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
        if (socketConnections.size() < 1) return;
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
                mUndeliveredMessages.remove(message);
            }
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

    public static void addDefaultListeners(String name) {
        ArrayList<String> defaultListeners = new ArrayList<>();
        defaultListeners.add("connect");
        defaultListeners.add("disconnect");
        defaultListeners.add("connect_error");
        socketListeners.put(name, defaultListeners);
    }

    public static void connect(String name, String url, String query) {
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        String socketName = name.toLowerCase();
        SocketIO socketConnection = socketConnections.get(name.toLowerCase());
        if (socketConnection != null) {
            String error = name + "Already connected";
            callbackContext.success(error);
            return;
        }
        SocketIO socketIO = new SocketIO(name, url, query);
        socketConnections.put(socketName, socketIO);
        addDefaultListeners(socketName);
        callbackContext.success();
        // start service
        start();
    }


    public static void removeListener(String name, String event) {
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        SocketIO socketConnection = socketConnections.get(name.toLowerCase());
        if (socketConnection == null) {
            String error = name + "Socket not found";
            callbackContext.error(error);
            return;
        }
        socketConnection.removeListener(event);
        callbackContext.success();
        //remove listener from listeners list
        ArrayList<String> listeners = socketListeners.get(name);
        listeners.remove(event.toLowerCase());
        socketListeners.put(name, listeners);
    }

    public static void addToListeners(String socket, String event) {
        ArrayList<String> listeners = socketListeners.get(socket);
        listeners.add(event.toLowerCase());
        socketListeners.put(socket, listeners);
    }

    public static void addListener(String name, String event, Boolean alert) {
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        String socketName = name.toLowerCase();
        SocketIO socketConnection = socketConnections.get(name.toLowerCase());
        if (socketConnection == null) {
            String error = name + "Socket not found";
            callbackContext.error(error);
            return;
        }
        //adding to listeners
        addToListeners(socketName, event);
        // socket.io listener
        socketConnection.addListener(event, alert);
        String message = event + "listener added";
        callbackContext.success(message);
    }


    public static void emit(String name, String event, Object data) {
        SocketIO socketConnection = socketConnections.get(name.toLowerCase());
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        if (socketConnection == null) {
            String error = name + "Socket not found";
            callbackContext.error(error);
            return;
        }
        socketConnection.emit(event, data);
        callbackContext.success();
    }

    public static void disconnect(String socket) {
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        SocketIO connection = socketConnections.get(socket);
        if (connection == null) {
            Log.w(TAG, "disconnect: Already disconnected");
            callbackContext.success();
            return;
        }
        connection.disconnect();
        socketConnections.remove(socket);
        callbackContext.success();
        if (socketConnections.size() < 1) {
            stop();
            Log.i(TAG, "disconnect: stopping service");
        }
    }

    public static void disconnectAll() {
        for (Map.Entry<String, SocketIO> entry : socketConnections.entrySet()) {
            SocketIO connection = entry.getValue();
            connection.disconnect();
        }
        SocketIOPlugin.mCallbackContext.success();
        stop();
    }

    public static void getStatus(String socketName){
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        String socketName = name.toLowerCase();
        SocketIO socketConnection = socketConnections.get(name.toLowerCase());
        if (socketConnection == null) {
            callbackContext.success("false");
            return;
        }
        callbackContext.success("true")
    }


    public static void sendMessage(JSONObject message, Boolean showAlert) {
        if (!isMainAppForeground()) {
            mUndeliveredMessages.add(message);
            Log.i(TAG, "sendMessage: " + "saved to undelivered");
            if (showAlert) {
                showAlert("Test");
            }
        } else {
            SocketIOPlugin.onData(message);
        }

    }

    public static void sendMessage(String message, Boolean showAlert) {
        if (!isMainAppForeground()) {
            if (showAlert) {
                showAlert("Test");
            }
        } else {
            SocketIOPlugin.onData(message);
        }
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

    private static void showAlert(String alertMessage) {
        if (AlertActivity.isAlertShown > 0) {
            Log.i(TAG, "showAlert: " + "alert active");
            return;
        }
        Context context = SocketIOPlugin.mApplicationContext;
        Intent alertIntent;
        alertIntent = new Intent(context, AlertActivity.class);
        alertIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        alertIntent.putExtra("alertMessage", alertMessage);
        context.startActivity(alertIntent);
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
        if (runningProcessInfo != null && AlertActivity.isAlertShown == 0) {
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