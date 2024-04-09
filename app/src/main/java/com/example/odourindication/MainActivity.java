package com.example.odourindication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity {

    public TextView coachD2Concentration;
    public TextView coachD10Concentration;
    private MyForegroundService mService;
    private boolean mBound = false;

    int coachD2Measure;
    int coachD10Measure;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Main-activity", "onReceive: receive data on main activity");
            coachD2Measure = intent.getIntExtra("coachD2", 0);
            coachD10Measure = intent.getIntExtra("coachD10", 0);
            Log.d("Main-activity", "onReceive: coachD2Measure : "+coachD2Measure);
            Log.d("Main-activity", "onReceive: coachD10Measure : "+coachD10Measure);
            String coachD2MeasureStr = String.valueOf(coachD2Measure);
            String coachD10MeasureStr = String.valueOf(coachD10Measure);
            Toast.makeText(MainActivity.this, "CoachD2 : " + coachD2Measure+"\nCoachD10 : "+coachD10Measure, Toast.LENGTH_SHORT).show();
            coachD2Concentration.setText(coachD2MeasureStr);
            coachD10Concentration.setText(coachD10MeasureStr);
            if(coachD2Measure >= 550 || coachD10Measure >=550){
                if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED){
                    msgControl();
                }
                else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS}, 100);
                }
            }
        }
    };
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("onServiceConnected", "onServiceConnected: service connecting");
            MyForegroundService.LocalBinder binder = (MyForegroundService.LocalBinder) service;
            mService = binder.getService();
            Log.d("MainActivity", "onServiceConnected : "+mService);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("custom-event-name"));

        Button startServiceButton = findViewById(R.id.startServiceButton);
        Button stopServiceButton = findViewById(R.id.stopServiceButton);
        coachD2Concentration = findViewById(R.id.coachD2Concentration);
        coachD10Concentration = findViewById(R.id.coachD10Concentration);

        startServiceButton.setOnClickListener(v -> {
            Log.d("click Start","Service started");
            Intent intent = new Intent(MainActivity.this, MyForegroundService.class);
            startForegroundService(intent);
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            Toast.makeText(MainActivity.this, "Service started", Toast.LENGTH_SHORT).show();
        });

        stopServiceButton.setOnClickListener(v -> {
            Log.d("click stop","Service stopped");
            Intent stopIntent = new Intent(MainActivity.this, MyForegroundService.class);
            stopIntent.setAction(MyForegroundService.STOP_FOREGROUND_ACTION);
            startService(stopIntent);
            Toast.makeText(MainActivity.this, "Service stopped", Toast.LENGTH_SHORT).show();
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("onStart", "onStart: Bind Service");
        Intent intent = new Intent(MainActivity.this, MyForegroundService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from MyForegroundService
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }
    @Override
    protected void onDestroy() {
        // Unregister the receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public  void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode ==100 && grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            sendMsg();
        }else{
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }
    String phone = "7385677315"; //default
    String message = "";
    private void msgControl(){

        String houseKeeping = "7385677315";
        String manager = "7385677315";

        String warningHousingKeeping = "Warning to House keeping staff for clean toilet in ";
        String warningManager = "Warning to Manager for clean toilet in ";

        if((coachD2Measure >=550 && coachD2Measure<800) && (coachD10Measure >=550 && coachD10Measure<800)){
            phone=houseKeeping;
            message = warningHousingKeeping+"Coach-D2 and Coach-D10";
            sendMsg();
        }else if((coachD2Measure >=800 && coachD2Measure<1250) && (coachD10Measure >=800 && coachD10Measure<1250)){
            phone=manager;
            message = warningManager+"Coach-D2 and Coach-D10";
            sendMsg();
        }
        else if((coachD2Measure >=550 && coachD2Measure<800) &&  (coachD10Measure >=800 && coachD10Measure<1250)){
            phone=houseKeeping;
            message = warningHousingKeeping+"Coach-D2";
            sendMsg();
            phone=manager;
            message=warningManager+"Coach-D10";
            sendMsg();
        }
        else if((coachD2Measure >=800 && coachD2Measure<1250) && (coachD10Measure >=550 && coachD10Measure<800)){
            phone=manager;
            message = warningManager+"Coach-D2";
            sendMsg();
            phone=houseKeeping;
            message= warningHousingKeeping+"Coach-D10";
            sendMsg();
        }
        else if(coachD2Measure >=550 && coachD2Measure<800){
            phone=houseKeeping;
            message = warningHousingKeeping+"Coach-D2";
            sendMsg();
        }else if(coachD2Measure >=800 && coachD2Measure<1250){
            phone=manager;
            message=warningManager+"Coach-D2";
            sendMsg();
        }else if(coachD10Measure >=550 && coachD10Measure<800){
            phone=houseKeeping;
            message = warningHousingKeeping+"Coach-D10";
            sendMsg();
        }else if(coachD10Measure >=800 && coachD10Measure<1250){
            phone=manager;
            message=warningManager+"Coach-D10";
            sendMsg();
        }
    }

    private void sendMsg() {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone, null, message, null, null);
        Toast.makeText(this, "Warning sent successfully", Toast.LENGTH_SHORT).show();
    }
}
