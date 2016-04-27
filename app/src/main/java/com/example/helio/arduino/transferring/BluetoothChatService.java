package com.example.helio.arduino.transferring;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.helio.arduino.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothChatService {

    private static final String TAG = "BluetoothChatService";
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    private Context mContext;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public BluetoothChatService(Context context, Handler handler) {
        this.mContext = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    private synchronized void setState(int state) {
        mState = state;
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        cancelConnectThread();
        cancelConnectedThread();
        setState(STATE_LISTEN);
        startSecureThread();
        startInSecureThread();
    }

    private void startInSecureThread() {
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    private void startSecureThread() {
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
    }

    private void cancelConnectedThread() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    private void cancelConnectThread() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    public synchronized void connect(BluetoothDevice device, boolean secure) {
        if (mState == STATE_CONNECTING) {
            cancelConnectThread();
        }
        cancelConnectedThread();
        startConnectThread(device, secure);
        setState(STATE_CONNECTING);
    }

    private synchronized void startConnectThread(BluetoothDevice device, boolean secure) {
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        cancelConnectThread();
        cancelConnectedThread();
        cancelSecureThread();
        cancelInSecureThread();
        startConnectedThread(socket, socketType);
        postMessage(device);
        setState(STATE_CONNECTED);
    }

    private void postMessage(BluetoothDevice device) {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private void startConnectedThread(BluetoothSocket socket, String socketType) {
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();
    }

    private void cancelInSecureThread() {
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
    }

    private void cancelSecureThread() {
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
    }

    public synchronized void stop() {
        cancelConnectThread();
        cancelConnectedThread();
        cancelSecureThread();
        cancelInSecureThread();
        setState(STATE_NONE);
    }

    public void writeMessage(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    private void postToast(String message) {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, message);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private void connectionFailed() {
        postToast(mContext.getString(R.string.unable_to_connect_device));
        BluetoothChatService.this.start();
    }

    private void connectionLost() {
        postToast(mContext.getString(R.string.device_connection_was_lost));
        BluetoothChatService.this.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            mSocketType = secure ? "Secure" : "Insecure";
            mmServerSocket = openSocket(secure);
        }

        private BluetoothServerSocket openSocket(boolean secure) {
            try {
                if (secure) {
                    return mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    return mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
                return null;
            }
        }

        public void run() {
            setName("AcceptThread" + mSocketType);
            while (mState != STATE_CONNECTED) {
                BluetoothSocket socket = getSocket();
                if (socket != null) {
                    obtainSocket(socket);
                }
            }
        }

        private void obtainSocket(BluetoothSocket socket) {
            synchronized (BluetoothChatService.this) {
                switch (mState) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connected(socket, socket.getRemoteDevice(),
                                mSocketType);
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Could not close unwanted socket", e);
                        }
                        break;
                }
            }
        }

        private BluetoothSocket getSocket() {
            try {
                return mmServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                return null;
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            mSocketType = secure ? "Secure" : "Insecure";
            mmSocket = openSocket(device, secure);
        }

        private BluetoothSocket openSocket(BluetoothDevice device, boolean secure) {
            try {
                if (secure) {
                    return device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                } else {
                    return device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
                return null;
            }
        }

        public void run() {
            setName("ConnectThread" + mSocketType);
            mAdapter.cancelDiscovery();
            if (!connectToSocket()) {
                return;
            }
            disposeThread();
            connected(mmSocket, mmDevice, mSocketType);
        }

        private void disposeThread() {
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }
        }

        private boolean connectToSocket() {
            try {
                mmSocket.connect();
                return true;
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return false;
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] buffer = new byte[1024];
        private int bytes;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            while (true) {
                if (!readStream()) {
                    break;
                }
            }
        }

        private boolean readStream() {
            try {
                bytes = mmInStream.read(buffer);
                mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                        .sendToTarget();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "disconnected", e);
                connectionLost();
                // Start the service over to restart listening mode
                BluetoothChatService.this.start();
                return false;
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
