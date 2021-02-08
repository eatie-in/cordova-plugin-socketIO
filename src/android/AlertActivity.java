package com.vishnu.socketio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import io.cordova.hellocordova.R;

public class AlertActivity extends Activity {
    public static int isAlertShown = 0;

    public void onCreate(Bundle state){
        super.onCreate(state);
        isAlertShown++;
        setContentView(R.layout.activity_alert);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        setContentView(getApplicationContext().getResources().getIdentifier("activity_alert", "layout", getApplicationContext().getPackageName()));
        this.showAlert("okay");
    }



    private  void showAlert(String text){
//        Intent intent = new Intent(getApplicationContext(), AlertActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        startActivity(intent);
        AlertDialog.Builder builder = new AlertDialog.Builder(
                AlertActivity.this);
        builder.setTitle("Sample Alert");
        builder.setMessage(text);
        builder.setCancelable(false);
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        PackageManager pm = getPackageManager();
                        Intent notificationIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(notificationIntent);
                        Toast.makeText(getApplicationContext(),"Yes is clicked",Toast.LENGTH_LONG).show();
                    }
                });
        builder.show();
    }


    public void onDestroy() {
        isAlertShown--;
        super.onDestroy();
    }
}