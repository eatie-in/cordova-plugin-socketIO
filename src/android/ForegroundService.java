package com.vishnu.socketio;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

import org.json.JSONObject;

import java.util.List;

import io.cordova.hellocordova.R;

import static com.vishnu.socketio.SocketIO.TAG;

public class ForegroundService extends Service {
    private static String CHANNEL_ID = "SocketIO";
    private static String TAG = "ForegroundService";
    private static final String INTENT_EXTRA_CONNECT_URL = "connect_url";
    private static final String INTENT_EXTRA_QUERY = "connect_url";
    private static final String ACTION_START = "socketio.service.start";
    private static final String ACTION_STOP = "socketio.service.stop";

    public static void start(Context context,String connectUrl,String query){
        Log.d(TAG, "NotificationSocketService STARTED");

        // remove all notifications from status bar. This method gets called when app is started
        NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancelAll();

        Intent intent = new Intent(context, ForegroundService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(INTENT_EXTRA_CONNECT_URL, connectUrl);
        intent.putExtra(INTENT_EXTRA_QUERY, connectUrl);
        context.startService(intent);
    }

    public static void stop(Context context){
        Log.d(TAG, "NotificationSocketService STOPPED");
        Intent intent = new Intent(context, ForegroundService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SocketIOService.bus.register(this);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = createNotification("connection..");
        startForeground(1, notification);
        return START_STICKY;
    }

    @Subscribe
    public void onGetStats(String status) {
        Log.i(TAG, "onGetStats: ");
        this.updateNotification(status);
//        this.showAlert(status);
    }

    @Subscribe
    public void onMessage(MessageEvent messageEvent){
     if(messageEvent.getShowAlert()){
         Boolean isMainAppInForeground = this.isMainAppForeground();
         if(!isMainAppInForeground){
             this.showAlert(messageEvent.getData().toString());
         }

//         PackageManager pm = getPackageManager();
//         Intent notificationIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
//         startActivity(notificationIntent);
     }else {
         Log.i(TAG, "onMessage: "+"normal");
     }
    }

    private void showAlert(String alertMessage) {
        Intent alertIntent;
        alertIntent = new Intent(getApplicationContext(), AlertActivity.class);

        alertIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        alertIntent.putExtra("alertMessage", alertMessage);
        startActivity(alertIntent);
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

    private Notification createNotification(String text) {
        PackageManager pm = getPackageManager();
        Intent notificationIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Connection")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0,
                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        return notification;
    }

    public void updateNotification(String text){
        Notification notification = createNotification(text);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(1,notification);
    }

    private boolean isMainAppForeground() {
        boolean isMainAppForeground = false;
        KeyguardManager km = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        boolean isPhoneLocked = km.inKeyguardRestrictedInputMode();
        boolean isSceenAwake = (Build.VERSION.SDK_INT < 20 ? pm.isScreenOn() : pm.isInteractive());

        List<ActivityManager.RunningAppProcessInfo> runningProcessInfo = activityManager.getRunningAppProcesses();
        if (runningProcessInfo != null && AlertActivity.isAlertShown == 0) {
            for (ActivityManager.RunningAppProcessInfo appProcess : runningProcessInfo) {
                Log.d("NotificationService", String.valueOf(appProcess.importance));
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        && appProcess.processName.equals(getApplication().getPackageName())
                        && !isPhoneLocked && isSceenAwake) {
                    isMainAppForeground = true;
                    break;
                }
            }
        }

        return isMainAppForeground;
    }
}