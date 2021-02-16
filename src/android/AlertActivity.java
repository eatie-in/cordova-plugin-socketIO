package com.vishnu.socketio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class AlertActivity extends Activity {
    public static int isAlertShown = 0;
    private static final String mLayoutName = "activity_alert";
    private static final String mAudioName = "audio";
    private Context mContext;
    private String mPackageName;
    private Vibrator mVibrator;
    private Ringtone mRingtone;

    public void onCreate(Bundle state) {
        super.onCreate(state);
        mContext = getApplicationContext();
        mPackageName = mContext.getPackageName();
        setLayoutParams();
        getServices();
        setContentView(getLayout());
        showAlert();
    }

    public void setLayoutParams() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    public int getLayout() {
        int layout = mContext.getResources().getIdentifier(mLayoutName, "layout", mPackageName);
        return layout;
    }


    private void vibrate(long seconds) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrator.vibrate(VibrationEffect.createOneShot(seconds, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            mVibrator.vibrate(seconds);
        }
    }


    private AlertDialog.Builder createAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("You have new order");
        builder.setMessage("Tap ok to view the order");
        builder.setCancelable(false);
        builder.setIcon(mContext.getResources().getIdentifier("ic_launcher","mipmap",mPackageName));
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        mVibrator.cancel();
                        mRingtone.stop();
                        launchApp();
                    }
                });

        return builder;
    }


    private void launchApp() {
        PackageManager pm = getPackageManager();
        Intent notificationIntent = pm.getLaunchIntentForPackage(mPackageName);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(notificationIntent);
    }

    private void showAlert() {
        AlertDialog.Builder alert = createAlert();
        isAlertShown++;
        vibrate(5000);
        mRingtone.play();
        alert.show();
    }

    private void getServices() {
        mVibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        getSound();
    }

    private void getSound() {
        int checkExistence = mContext.getResources().getIdentifier("audio", "raw", mPackageName);
        if (checkExistence != 0) {
            Uri soundPath = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/" + mAudioName);
            mRingtone = RingtoneManager.getRingtone(this, soundPath);
        } else {
            // ringtone
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            mRingtone = RingtoneManager.getRingtone(this, notification);
        }
    }


    public void onDestroy() {
        isAlertShown--;
        super.onDestroy();
    }
}