package ab.explore.android_background_web_server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.pixplicity.sharp.OnSvgElementListener;
import com.pixplicity.sharp.Sharp;
import com.pixplicity.sharp.SharpPicture;

import uk.co.senab.photoview.PhotoViewAttacher;

public class WebServerActivity extends AppCompatActivity {

    private static final int DEFAULT_PORT = 8080;

    // INSTANCE OF ANDROID WEB SERVER
    private AndroidWebServer androidWebServer;
    private BroadcastReceiver br_network_receiver;
    private static boolean isStarted = false;

    private ImageView iv_server;
    private PhotoViewAttacher mAttacher;
    private Sharp mSvg;
    private FloatingActionButton fab_connect;
    private EditText et_Port;
    private TextView tv_Message;
    private TextView tv_IpAccess;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_server);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        initComponent();
        prepareComponent();
        setIpAccess();
        onClickListeners();
        initBroadcastReceiverNetworkStateChanged();

    }

    private void initComponent() {
        iv_server = (ImageView) findViewById(R.id.iv_server);
        fab_connect = (FloatingActionButton) findViewById(R.id.fab_connect);
        et_Port = (EditText) findViewById(R.id.et_Port);
        tv_Message = (TextView) findViewById(R.id.tv_Message);
        tv_IpAccess = (TextView) findViewById(R.id.tv_IpAccess);
    }

    private void prepareComponent() {
        mSvg = Sharp.loadResource(getResources(), R.raw.network_server);
        mAttacher = new PhotoViewAttacher(iv_server);
        mAttacher.setMaximumScale(10f);
        mAttacher.setZoomable(false);
        reloadSvg(false,1);

        mSvg = Sharp.loadResource(getResources(), R.raw.ic_power);
        mAttacher = new PhotoViewAttacher(fab_connect);
        mAttacher.setMaximumScale(10f);
        mAttacher.setZoomable(false);
        reloadSvg(false,2);
    }

    private void setIpAccess() {
        tv_IpAccess.setText(getIpAccess());
    }

    private void onClickListeners() {
        fab_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnectedInWifi()) {
                    if (!isStarted && startAndroidWebServer()) {
                        isStarted = true;
                        tv_Message.setText(getString(R.string.message));
                        fab_connect.setBackgroundTintList(ContextCompat.getColorStateList(WebServerActivity.this, R.color.colorGreen));
                        et_Port.setEnabled(false);
                    } else if (stopAndroidWebServer()) {
                        isStarted = false;
                        tv_Message.setText(getString(R.string.wifi_message));
                        fab_connect.setBackgroundTintList(ContextCompat.getColorStateList(WebServerActivity.this, R.color.colorRed));
                        et_Port.setEnabled(true);
                    }
                } else {
                    Snackbar.make(v, getString(R.string.wifi_message), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void initBroadcastReceiverNetworkStateChanged() {
        final IntentFilter filters = new IntentFilter();
        filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filters.addAction("android.net.wifi.STATE_CHANGE");
        br_network_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setIpAccess();
            }
        };
        super.registerReceiver(br_network_receiver, filters);
    }


    //region Start And Stop AndroidWebServer
    private boolean startAndroidWebServer() {
        if (!isStarted) {
            int port = getPortFromEditText();
            try {
                if (port == 0) {
                    throw new Exception();
                }
                androidWebServer = new AndroidWebServer(port);
                androidWebServer.start();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Snackbar.make(fab_connect, "The PORT " + port + " doesn't work, please change it between 1000 and 9999.", Snackbar.LENGTH_LONG).show();
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
    //endregion

    //region Private utils Method


    private String getIpAccess() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        Log.e(this.getClass().getCanonicalName(),"IP: " + ipAddress);
        if(ipAddress==0){
            if(stopAndroidWebServer()){
                isStarted = false;
                tv_Message.setText(getString(R.string.wifi_message));
                fab_connect.setBackgroundTintList(ContextCompat.getColorStateList(WebServerActivity.this, R.color.colorRed));
                et_Port.setEnabled(true);
            }else{
                tv_Message.setText(getString(R.string.wifi_message));
                fab_connect.setBackgroundTintList(ContextCompat.getColorStateList(WebServerActivity.this, R.color.colorRed));
                et_Port.setEnabled(true);
            }
        }else{
            tv_Message.setText("");
        }
        final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return "http://" + formatedIpAddress + ":";
    }

    private int getPortFromEditText() {
        String valueEditText = et_Port.getText().toString();
        return (valueEditText.length() > 0) ? Integer.parseInt(valueEditText) : DEFAULT_PORT;
    }

    public boolean isConnectedInWifi() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        NetworkInfo networkInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()
                && wifiManager.isWifiEnabled() && networkInfo.getTypeName().equals("WIFI")) {
            return true;
        }
        return false;
    }
    //endregion

    public boolean onKeyDown(int keyCode, KeyEvent evt) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isStarted) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.dialog_exit_message)
                        .setPositiveButton(getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        })
                        .setNegativeButton(getResources().getString(android.R.string.cancel), null)
                        .show();
            } else {
                finish();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAndroidWebServer();
        isStarted = false;
        if (br_network_receiver != null) {
            unregisterReceiver(br_network_receiver);
        }
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
                    if(who==1) {
                        Drawable drawable = picture.getDrawable(iv_server);
                        iv_server.setImageDrawable(drawable);
                    }else if(who==2){
                        Drawable drawable = picture.getDrawable(fab_connect);
                        fab_connect.setImageDrawable(drawable);
                    }
                }

                mAttacher.update();
            }
        });
    }



}
