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

import io.cordova.hellocordova.R;


public class SocketIOService extends Service {
    private static String CHANNEL_ID = "SocketIO";
    private static String TAG = "SocketIOService";
    private boolean mRunning = false;
    private static String connectionStatus = "Connecting..";

    private static ArrayList<JSONObject> mUndeliveredMessages = new ArrayList<JSONObject>();

    private static Map<String, SocketIO> socketConnections = new HashMap<String, SocketIO>();


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("NotificationService", "onTaskRemoved METHOD");
        Intent restartService = new Intent(getApplicationContext(), this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);
        start();
//        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
//        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePI);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = createNotification(connectionStatus);
        startForeground(2, notification);
        return START_STICKY_COMPATIBILITY;
    }


    private static void showAlert(String alertMessage) {
        Context context = SocketIOPlugin.mApplicationContext;
        Intent alertIntent;
        alertIntent = new Intent(context, AlertActivity.class);

        alertIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        alertIntent.putExtra("alertMessage", alertMessage);
        context.startActivity(alertIntent);
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
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Connection")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(context, 0,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        return notification;
    }

    public static void updateNotification(String text) {
        Context context = SocketIOPlugin.mApplicationContext;
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        Notification notification = createNotification(text);
        managerCompat.notify(2, notification);

//        NotificationManagerCompat managerCompat =
//                notificationManagerCompat.notify(1, notification);
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


    public static void updateStatus(String status) {
        updateNotification(status);
        connectionStatus = status;
        Log.i(TAG, "updateStatus: " + status);
    }

    public static void getUndelivered() {
        ArrayList<JSONObject> messages = new ArrayList<JSONObject>(mUndeliveredMessages.size());
        if (mUndeliveredMessages.size() >= 1) {
            for (JSONObject message : mUndeliveredMessages) {
                messages.add(message);
            }
            for (JSONObject message:messages){
                sendMessage(message,false);
                mUndeliveredMessages.remove(message);
            }
        } else {
            Log.i(TAG, "getUndelivered: no undelivered");
        }
    }


    public static void sendMessage(JSONObject message, Boolean showAlert) {
        if (!isMainAppForeground()) {
            mUndeliveredMessages.add(message);
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

    public static void connect(String name, String url, String query) {
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        SocketIO socketConnection = socketConnections.get(name.toLowerCase());
        if (socketConnection != null) {
            String error = name + "Already connected";
            callbackContext.error(error);
            return;
        }
        SocketIO socketIO = new SocketIO(name, url, query);
        socketConnections.put(name.toLowerCase(), socketIO);
        callbackContext.success("connect");
        // start service
        start();
    }

    public static void addListener(String name, String event, Boolean alert) {
        CallbackContext callbackContext = SocketIOPlugin.mCallbackContext;
        SocketIO socketConnection = socketConnections.get(name.toLowerCase());
        if (socketConnection == null) {
            String error = name + "Socket not found";
            callbackContext.error(error);
            return;
        }
        socketConnection.addListener(event, alert);
        String message = event + "listener added";
        callbackContext.success(message);
    }

    public static void listen(String name, String event, CallbackContext callbackContext, Boolean showAlert) {
        SocketIO socketConnection = socketConnections.get(name.toLowerCase());
        if (socketConnection == null) {
            String error = name + "Socket not found";
            callbackContext.error(error);
            return;
        }
        socketConnection.listen(event,
                callbackContext, showAlert);
        callbackContext.success("listeners added");
    }

    public static void emit(String name, String event, Object data) {
        SocketIO socketConnection = socketConnections.get(name.toLowerCase());
        if (socketConnection == null) {
            String error = name + "Socket not found";
//            SocketIOPlugin.mCallbackContext.error(error);
            return;
        }
        socketConnection.emit(event,
                data);
//        SocketIOPlugin.mCallbackContext.success(event + "is sent");
    }

    public static void disconnect(String socket) {
        SocketIO connection = socketConnections.get(socket);
        connection.disconnect();
        socketConnections.remove(socket);
        if (socketConnections.size() > 1) {
            stop();
            Log.i(TAG, "disconnect: stopping service");
        }
//        SocketIOPlugin.mCallbackContext.success("disconnected");
    }

    public static void disconnectAll() {
        for (Map.Entry<String, SocketIO> entry : socketConnections.entrySet()) {
            SocketIO connection = entry.getValue();
            connection.disconnect();
        }
//        SocketIOPlugin.mCallbackContext.success("disconnected");
        stop();
    }
}