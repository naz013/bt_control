package com.example.helio.arduino;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {

    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mDevice;
    private Context mContext;
    private ThreadListener mListener;

    public ConnectThread(Context context, BluetoothDevice device, UUID uuid, ThreadListener listener) {
        this.mContext = context;
        this.mListener = listener;
        BluetoothSocket tmp = null;
        mDevice = device;

        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {

        }
        mmSocket = tmp;
    }

    public void run() {
        try {
            mmSocket.connect();
        } catch (IOException connectException) {
            try {
                mmSocket.close();
            } catch (IOException closeException) {

            }
            return;
        }

        manageConnectedSocket(mmSocket);
    }

    private void manageConnectedSocket(BluetoothSocket mmSocket) {
        if (mmSocket.isConnected()) {
            Log.d("TAG", "Connected");
            SharedPreferences preferences = mContext.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.DEVICE_ADDRESS, mDevice.getAddress());
            editor.commit();
            if (mListener != null) {
            }
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
