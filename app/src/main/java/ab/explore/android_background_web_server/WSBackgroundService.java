package ab.explore.android_background_web_server;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.List;

/**
 * Created by abhi on 12/1/16.
 */

class WSBackgroundService extends Service{
    private AndroidWebServer androidWebServer;
    private BroadcastReceiver br_network_receiver;
    private static boolean isStarted = false;
    private int port;
    AppSharedPreference appSharedPreference = new AppSharedPreference();
    private static final String TAG = "WSBackgroundService";
    private static boolean isRunning  = false;
    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");
        if(appSharedPreference.loadSavedPreferences(getApplicationContext(),"service_running").isEmpty()) {
            isRunning = true;
            appSharedPreference.savePreferences(getApplicationContext(), "service_running", "" + isRunning);
        }else{
            isRunning = Boolean.parseBoolean(appSharedPreference.loadSavedPreferences(getApplicationContext(),"service_running"));
        }
        port = Integer.parseInt(appSharedPreference.loadSavedPreferences(getApplicationContext(),"port"));
        Log.e(TAG,"Port: " + port);
        initBroadcastReceiverNetworkStateChanged();
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "Service onStartCommand");



        //Creating new thread for my service
        //Always write your long running tasks in a separate thread, to avoid ANR
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isConnectedInWifi()) {
                    if (!isStarted && startAndroidWebServer()) {
                        isStarted = true;
                    } else if (stopAndroidWebServer()) {
                        isStarted = false;
                    }
                } else {
                    Log.e(TAG,getString(R.string.wifi_message));
                }


            }
        }).start();

        return Service.START_STICKY;
    }

    public boolean isConnectedInWifi() {
        @SuppressLint("WifiManagerLeak") WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        NetworkInfo networkInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()
                && wifiManager.isWifiEnabled() && networkInfo.getTypeName().equals("WIFI")) {
            return true;
        }
        return false;
    }



    private void initBroadcastReceiverNetworkStateChanged() {
        final IntentFilter filters = new IntentFilter();
        filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filters.addAction("android.net.wifi.STATE_CHANGE");
        br_network_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(isServiceRunning("ab.explore.android_background_web_server.WebServerActivity"))
                    WebServerActivity.setIpAccess(isRunning);
            }
        };
        super.registerReceiver(br_network_receiver, filters);
    }



    private boolean startAndroidWebServer() {
        if (!isStarted) {
            try {
                if (port == 0) {
                    throw new Exception();
                }
                androidWebServer = new AndroidWebServer(port);
                androidWebServer.start();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG,"The PORT " + port + " doesn't work, please change it between 1000 and 9999.");
                    }
                });
            }
        }
        return false;
    }

    private boolean stopAndroidWebServer() {
        if (isStarted && androidWebServer != null) {
            androidWebServer.stop();
            return true;
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {

        stopAndroidWebServer();
        isRunning = false;
        isStarted = false;
        appSharedPreference.savePreferences(getApplicationContext(),"service_running",""+isRunning);
        if (br_network_receiver != null) {
            unregisterReceiver(br_network_receiver);
        }
        Log.i(TAG, "Service onDestroy");
    }


    /**
     * Check for activity is in foreground and running right now
     *
     * @param activity full path of class
     * @return boolean Activity running or not
     */
    public boolean isServiceRunning(String activity) {

        ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> services = activityManager.getRunningTasks(Integer.MAX_VALUE);
        boolean isServiceFound = false;
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).topActivity.toString().equalsIgnoreCase("ComponentInfo{ab.explore.android_background_web_server/" + activity + "}")) {
                isServiceFound = true;
            }
        }
        return isServiceFound;
    }
}
