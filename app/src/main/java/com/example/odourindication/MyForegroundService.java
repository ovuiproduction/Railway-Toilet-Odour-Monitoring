package com.example.odourindication;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MyForegroundService extends Service implements onRequestPermissionsResult {

    private static final String TAG = "MyForegroundService";
    private static final int NOTIFICATION_ID = 123;
    private static final String CHANNEL_ID = "MyForegroundServiceChannel";
    private static final long FETCH_INTERVAL = 60000; // Fetch data every 1 minute

    public static final String STOP_FOREGROUND_ACTION = "stopForeground";

    private int coachD2;
    private int coachD10;

    private int CD2 =-1;
    private int CD10 =-1;


    Handler mHandler = new Handler(Looper.getMainLooper()); // Associates with the main (UI) thread's Looper
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            fetchDataAndCheckThreshold();
            mHandler.postDelayed(this, FETCH_INTERVAL);
        }
    };

    public class LocalBinder extends Binder {
        MyForegroundService getService() {
            Log.d(TAG, "getService: called getService");
            return MyForegroundService.this;
        }
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (intent != null) {
            if (STOP_FOREGROUND_ACTION.equals(intent.getAction())) {
                stopForegroundService();
                return START_NOT_STICKY;
            }
        }
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        mHandler.postDelayed(mRunnable, FETCH_INTERVAL);
        return START_STICKY;
    }

    private void stopForegroundService() {
        stopForeground(true); // Remove the service from the foreground state
        stopSelf(); // Stop the service
    }

    private void sendVariableToMainActivity() {
        Intent intent = new Intent("custom-event-name");
        Log.d(TAG, "coachD2 : "+coachD2);
        Log.d(TAG, "coachD10 : "+coachD10);
        intent.putExtra("coachD2", coachD2);
        intent.putExtra("coachD10", coachD10);
        Log.d(TAG, "sendVariableToMainActivity: Data sent to Main-activity");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mHandler.removeCallbacks(mRunnable); // Stop the periodic task
    }

    @SuppressLint("ObsoleteSdkInt")
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager == null) {
                return; // Unable to create notification channel
            }
            CharSequence name = "My Foreground Service Channel";
            String description = "Channel for My Foreground Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Odour Monitoring is On")
                .setContentText("Running...")
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntent)
                .build();
    }


    private void fetchCoachD2Data(DataFetchCallback callback) {
        String uri = "https://api.thingspeak.com/channels/2457707/fields/1.json?api_key=<Your_READ_API_KEY>&results=1";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, uri, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("feeds");
                JSONObject jsonObjectFeeds = jsonArray.getJSONObject(0);
                String field1 = jsonObjectFeeds.getString("field1");
                Log.d(TAG, "fetchCoachD2Data: "+field1);
                if(!field1.equals("null")){
                    CD2 = Integer.parseInt(field1);
                }
                callback.onDataFetched(CD2,CD10);
                //fetchCoachD10Data();
            } catch (JSONException e) {
                e.printStackTrace();
                callback.onError();
            }
        }, error -> CD2=-1);
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }

    private void fetchCoachD10Data(DataFetchCallback callback) {
        String uri = "https://api.thingspeak.com/channels/2458030/fields/1.json?api_key=<Your_READ_API_KEY>&results=1";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, uri, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("feeds");
                JSONObject jsonObjectFeeds = jsonArray.getJSONObject(0);
                String field1 = jsonObjectFeeds.getString("field1");
                Log.d(TAG, "fetchCoachD10Data: "+field1);
                if(!field1.equals("null")){
                    CD10 = Integer.parseInt(field1);
                }
                callback.onDataFetched(CD2,CD10);
            } catch (JSONException e) {
                e.printStackTrace();
                callback.onError();
            }
        }, error -> CD10=-1);
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }

    interface DataFetchCallback {
        void onDataFetched(int CD2, int CD10);
        void onError();
    }

    private void fetchDataAndCheckThreshold() {
        Log.d(TAG, "Fetching data from API and checking threshold...");

        fetchCoachD2Data(new DataFetchCallback() {
            @Override
            public void onDataFetched(int CD2, int CD10) {
                coachD2 = CD2;
                coachD10 = CD10;
                checkThresholdAndNotify();
            }

            @Override
            public void onError() {
                Log.e(TAG, "Error fetching data from Coach D2 API");
            }
        });

        fetchCoachD10Data(new DataFetchCallback() {
            @Override
            public void onDataFetched(int CD2, int CD10) {
                coachD2 = CD2;
                coachD10 = CD10;
                checkThresholdAndNotify();
            }

            @Override
            public void onError() {
                Log.e(TAG, "Error fetching data from Coach D10 API");
            }
        });
    }

    @SuppressLint("NotificationPermission")
    private void checkThresholdAndNotify() {
        Log.d(TAG, "Fetching data from API and checking threshold...");
            if (CD2 != -1 && CD10 != -1) {
                // Data fetched successfully, use it here
                coachD2 = CD2;
                coachD10 = CD10;
                Log.d(TAG, "onDataReceived: coachD2 "+coachD2);
                Log.d(TAG, "onDataReceived: coachD10 "+coachD10);

                sendVariableToMainActivity();

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager == null) {
                    return;
                }

                // Create a notification channel for Android Oreo and above
                createNotificationChannel();

                // Create the notification content
                String notificationContent = "Coach-D2 : " + CD2+"\nCoach-D10 : "+CD10;
                NotificationCompat.Builder builder = new NotificationCompat.Builder(MyForegroundService.this, CHANNEL_ID);
                builder.setSmallIcon(R.drawable.icon);
                builder.setContentTitle("Coach Wise Methane Concentration In toilets");
                builder.setContentText(notificationContent);
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);

                // Show the notification
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            } else {
                Log.e(TAG, "Error fetching data from API");
            }
    }
}
