package ab.explore.android_background_web_server;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.pixplicity.sharp.OnSvgElementListener;
import com.pixplicity.sharp.Sharp;
import com.pixplicity.sharp.SharpPicture;

import uk.co.senab.photoview.PhotoViewAttacher;

public class WebServerActivity extends AppCompatActivity {
    String intentName="IntentName";
    private static final int DEFAULT_PORT = 8080;
    private static boolean isStarted = false;
    // INSTANCE OF ANDROID WEB SERVER
    private AndroidWebServer androidWebServer;
    private BroadcastReceiver br_network_receiver;
    private ImageView iv_server;
    private PhotoViewAttacher mAttacher;
    private Sharp mSvg;
    static FloatingActionButton fab_connect;
    static EditText et_Port;
    static TextView tv_Message;
    public static TextView tv_IpAccess;
    static WifiManager wifiManager;
    private static String wifi_message = "You must connect to a WiFi network to use this service.";
    private static String message = "You can now access the Android Web Server from a browser connected to the same wifi network.";
    Intent intent = null;
    static AppSharedPreference appSharedPreference;
    static boolean isRunning = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_server);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        initComponent();
        prepareComponent();
        onClickListeners();
        setIpAccess(isRunning);
    }

    private void initComponent() {
        iv_server = (ImageView) findViewById(R.id.iv_server);
        fab_connect = (FloatingActionButton) findViewById(R.id.fab_connect);
        et_Port = (EditText) findViewById(R.id.et_Port);
        tv_Message = (TextView) findViewById(R.id.tv_Message);
        tv_IpAccess = (TextView) findViewById(R.id.tv_IpAccess);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        appSharedPreference = new AppSharedPreference();
        intent = new Intent(WebServerActivity.this, WSBackgroundService.class);
    }

    private void prepareComponent() {
        mSvg = Sharp.loadResource(getResources(), R.raw.network_server);
        mAttacher = new PhotoViewAttacher(iv_server);
        mAttacher.setMaximumScale(10f);
        mAttacher.setZoomable(false);
        reloadSvg(false, 1);

        mSvg = Sharp.loadResource(getResources(), R.raw.ic_power);
        mAttacher = new PhotoViewAttacher(fab_connect);
        mAttacher.setMaximumScale(10f);
        mAttacher.setZoomable(false);
        reloadSvg(false, 2);

        isRunning = Boolean.parseBoolean(appSharedPreference.loadSavedPreferences(getApplicationContext(),"service_running"));
        if(isRunning){
            tv_Message.setText(getString(R.string.message));
            fab_connect.setBackgroundTintList(ContextCompat.getColorStateList(WebServerActivity.this, R.color.colorGreen));
            et_Port.setEnabled(false);
        }else{
            tv_Message.setText("");
            fab_connect.setBackgroundTintList(ContextCompat.getColorStateList(WebServerActivity.this, R.color.colorRed));
            et_Port.setEnabled(true);
        }
    }

    public static void setIpAccess(boolean isRunning) {
        tv_IpAccess.setText(getIpAccess(isRunning));
    }

    private void onClickListeners() {
        fab_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isConnectedInWifi()){
                    if(isRunning){
                        isRunning = false;
                        appSharedPreference.savePreferences(WebServerActivity.this,"service_running",""+isRunning);
                        tv_Message.setText("");
                        fab_connect.setBackgroundTintList(ContextCompat.getColorStateList(WebServerActivity.this, R.color.colorRed));
                        et_Port.setEnabled(true);
                        if(intent!=null){
                            stopService(intent);
                        }
                    }else{
                        isRunning = true;
                        appSharedPreference.savePreferences(WebServerActivity.this,"service_running",""+isRunning);
                        appSharedPreference.savePreferences(WebServerActivity.this,"port",""+getPortFromEditText());
                        startService(intent);
                        tv_Message.setText(getString(R.string.message));
                        fab_connect.setBackgroundTintList(ContextCompat.getColorStateList(WebServerActivity.this, R.color.colorGreen));
                        et_Port.setEnabled(false);
                    }
                } else {
                    Snackbar.make(v, getString(R.string.wifi_message), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }





    private static String getIpAccess(boolean isRunning) {

        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        Log.e("WebServerActivity", "IP: " + ipAddress);

        if(isRunning){
            if(ipAddress == 0){
                tv_Message.setText(wifi_message);
                fab_connect.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF4081")));
                et_Port.setEnabled(true);
            }else {
                tv_Message.setText(message);
                fab_connect.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                et_Port.setEnabled(false);
            }
        }else{
            tv_Message.setText("");
            fab_connect.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF4081")));
            et_Port.setEnabled(true);
        }

        final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return "http://" + formatedIpAddress + ":";
    }

    private int getPortFromEditText() {
        String valueEditText = et_Port.getText().toString();
        return (valueEditText.length() > 0) ? Integer.parseInt(valueEditText) : DEFAULT_PORT;
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
    //endregion



    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void reloadSvg(final boolean changeColor, final int who) {
        mSvg.setOnElementListener(new OnSvgElementListener() {

            @Override
            public void onSvgStart(@NonNull Canvas canvas,
                                   @Nullable RectF bounds) {
            }

            @Override
            public void onSvgEnd(@NonNull Canvas canvas,
                                 @Nullable RectF bounds) {
            }

            @Override
            public <T> T onSvgElement(@Nullable String id,
                                      @NonNull T element,
                                      @Nullable RectF elementBounds,
                                      @NonNull Canvas canvas,
                                      @Nullable RectF canvasBounds,
                                      @Nullable Paint paint) {
                /*if (changeColor && paint != null && paint.getStyle() == Paint.Style.FILL &&
                        ("shirt".equals(id))) {
                    Random random = new Random();
                    paint.setColor(Color.argb(255, random.nextInt(256),
                            random.nextInt(256), random.nextInt(256)));
                }*/
                return element;
            }

            @Override
            public <T> void onSvgElementDrawn(@Nullable String id,
                                              @NonNull T element,
                                              @NonNull Canvas canvas,
                                              @Nullable Paint paint) {
            }

        });
        mSvg.getSharpPicture(new Sharp.PictureCallback() {
            @Override
            public void onPictureReady(SharpPicture picture) {
                {
                    if (who == 1) {
                        Drawable drawable = picture.getDrawable(iv_server);
                        iv_server.setImageDrawable(drawable);
                    } else if (who == 2) {
                        Drawable drawable = picture.getDrawable(fab_connect);
                        fab_connect.setImageDrawable(drawable);
                    }
                }

                mAttacher.update();
            }
        });
    }


}
