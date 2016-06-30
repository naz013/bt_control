package com.example.helio.arduino.core;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.helio.arduino.R;

import de.greenrobot.event.EventBus;

public class BluetoothService extends Service {

    final static String TAG = "BTService";
    private static final int NOTIFICATION_ID = 12563;

    private ConnectionManager mBtService = null;
    private BluetoothAdapter mBtAdapter = null;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    obtainConnectionMessage(msg);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    EventBus.getDefault().post(new ConnectionEvent(true));
                    break;
                case Constants.MESSAGE_READ:
                    EventBus.getDefault().post(new ResponseEvent(msg));
                    break;
            }
        }
    };

    private void obtainConnectionMessage(Message msg) {
        switch (msg.arg1) {
            case ConnectionManager.STATE_CONNECTED:
                EventBus.getDefault().post(new ConnectionEvent(true));
                break;
        }
    }

    public BluetoothService() {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        showNotification();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        setupConnection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void onEvent(ControlEvent controlEvent) {
        String message = controlEvent.getMsg();
        if (message != null) {
            mBtService.writeMessage(message.getBytes());
        }
    }

    private void showNotification() {
        Notification.Builder mNotificationBuilder = new Notification.Builder(getApplicationContext());
        mNotificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mNotificationBuilder.setContentTitle(getString(R.string.app_name));
        mNotificationBuilder.setContentText(getString(R.string.bt_service_started));
        mNotificationBuilder.setOngoing(true);
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        stopConnection();
        EventBus.getDefault().unregister(this);
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
    }

    private void setupConnection() {
        try {
            String emptyName = "None";
            SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Activity.MODE_PRIVATE);
            String mAddress = preferences.getString(Constants.DEVICE_ADDRESS, null);
            if (mAddress != null) {
                BluetoothDevice mConnectedDevice = mBtAdapter.getRemoteDevice(mAddress);
                DeviceData data = new DeviceData(mConnectedDevice, emptyName);
                mBtService = new ConnectionManager(data, mHandler);
                mBtService.connect();
            }
        } catch (IllegalArgumentException e) {
            Log.d("TAG", "setupConnector failed: " + e.getMessage());
        }
    }

    private void stopConnection() {
        if (mBtService != null) {
            mBtService.stop();
            mBtService = null;
        }
        EventBus.getDefault().post(new ConnectionEvent(false));
    }
}
