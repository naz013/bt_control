package com.example.helio.arduino.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ConnectionManager {

    private static final String TAG = "BluetoothService";
    private static final boolean D = true;

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    private final BluetoothAdapter btAdapter;
    private final BluetoothDevice connectedDevice;
    private final String deviceName;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    public ConnectionManager(DeviceData deviceData, Handler handler) {
        mHandler = handler;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        connectedDevice = btAdapter.getRemoteDevice(deviceData.getAddress());
        deviceName = (deviceData.getName() == null) ? deviceData.getAddress() : deviceData.getName();
        mState = STATE_NONE;
    }

    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void connect() {
        if (D) Log.d(TAG, "connect to: " + connectedDevice);
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                if (D) Log.d(TAG, "cancel mConnectThread");
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectThread = new ConnectThread(connectedDevice);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket) {
        if (D) Log.d(TAG, "connected");
        if (mConnectThread != null) {
            if (D) Log.d(TAG, "cancel mConnectThread");
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_CONNECTED);
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME, deviceName);
        mHandler.sendMessage(msg);
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
            if (D) Log.d(TAG, "cancel mConnectThread");
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            if (D) Log.d(TAG, "cancel mConnectedThread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    public void writeMessage(byte[] data) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        if (data.length == 1) r.write(data[0]);
        else r.writeData(data);
    }

    private void connectionFailed() {
        if (D) Log.d(TAG, "connectionFailed");
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
    }

    private void connectionLost() {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(STATE_NONE);
    }

    private class ConnectThread extends Thread {

        private static final String TAG = "ConnectThread";
        private static final boolean D = false;

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            if (D) Log.d(TAG, "create ConnectThread");
            mmDevice = device;
            mmSocket = BluetoothUtils.createRfcommSocket(mmDevice);
        }

        public void run() {
            if (D) Log.d(TAG, "ConnectThread run");
            btAdapter.cancelDiscovery();
            if (mmSocket == null) {
                if (D) Log.d(TAG, "unable to connect to device, socket isn't created");
                connectionFailed();
                return;
            }
            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    if (D) Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }
            synchronized (ConnectionManager.this) {
                mConnectThread = null;
            }
            connected(mmSocket);
        }

        public void cancel() {
            if (D) Log.d(TAG, "ConnectThread cancel");

            if (mmSocket == null) {
                if (D) Log.d(TAG, "unable to close null socket");
                return;
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private static final String TAG = "ConnectedThread";
        private static final boolean D = false;

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            if (D) Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                if (D) Log.e(TAG, "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            if (D) Log.i(TAG, "ConnectedThread run");
            byte[] byteBuffer = new byte[2000];
            boolean isDsoData = false;
            boolean hasMarkerStart = false;
            int bytes;
            int pointer = 0;
            StringBuilder readMessage = new StringBuilder();
            while (true) {
                try {
                    byte[] buffer = new byte[512];
                    bytes = mmInStream.read(buffer);
                    Log.d(TAG, "run: " + bytes + " bytes buffer " + Arrays.toString(buffer));
                    int endPosition = checkQueue(buffer, bytes);
                    if (bytes == 1 && buffer[0] == 121) {
                        isDsoData = true;
                    } else if (!isDsoData && buffer[0] == 121) {
                        isDsoData = true;
                        byteBuffer = new byte[2000];
                        pointer = 0;
                        if (bytes > 1) {
                            for (int i = 1; i < bytes; i++) {
                                byteBuffer[pointer] = buffer[i];
                                pointer++;
                            }
                        }
                    } else if (hasMarkerStart && (buffer[0] == 2 || (buffer[0] == 1 && buffer[1] == 2))) {
                        isDsoData = false;
                        int start = 1;
                        if (buffer[0] == 1 && buffer[1] == 2) start = 2;
                        short[] shorts = new short[byteBuffer.length / 2];
                        ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                        mHandler.obtainMessage(Constants.ARRAY_READ, bytes, -1, shorts).sendToTarget();
                        byteBuffer = new byte[2000];
                        pointer = 0;
                        if (bytes - 1 >= start) {
                            if (buffer[start] == 121) {
                                isDsoData = true;
                            }
                            for (int i = start + 1; i < bytes; i++) {
                                byteBuffer[pointer] = buffer[i];
                                pointer++;
                            }
                        }
                    } else if (isDsoData && bytes > 2 && endPosition != -1) {
                        isDsoData = false;
                        for (int i = 0; i < endPosition; i++) {
                            byteBuffer[pointer] = buffer[i];
                            pointer++;
                        }
                        short[] shorts = new short[byteBuffer.length / 2];
                        ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                        mHandler.obtainMessage(Constants.ARRAY_READ, bytes, -1, shorts).sendToTarget();
                        byteBuffer = new byte[2000];
                        pointer = 0;
                        if (bytes - endPosition - 1 > 0) {
                            if (buffer[endPosition + 3] == 121) {
                                isDsoData = true;
                            }
                            for (int i = endPosition + 4; i < bytes; i++) {
                                byteBuffer[pointer] = buffer[i];
                                pointer++;
                            }
                        }
                    } else if (isDsoData) {
                        if (pointer >= 2000) pointer = 0;
                        for (int i = 0; i < bytes; i++) {
                            byteBuffer[pointer] = buffer[i];
                            pointer++;
                        }
                    } else {
                        String readed = new String(buffer, 0, bytes);
                        readMessage.append(readed);
                        if (readed.contains("\n")) {
                            mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, readMessage.toString()).sendToTarget();
                            readMessage.setLength(0);
                        }
                    }
                    hasMarkerStart = checkMarker(buffer, bytes);
                } catch (IOException e) {
                    if (D) Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void writeData(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                if (D) Log.e(TAG, "Exception during write", e);
            }
        }

        public void write(byte command) {
            byte[] buffer = new byte[1];
            buffer[0] = command;
            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                if (D) Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private boolean checkMarker(byte[] buffer, int size) {
        return size >= 2 && (buffer[size - 2] == 0 && buffer[size - 1] == 1 || buffer[size - 1] == 0);
    }

    private int checkQueue(byte[] buffer, int size) {
        int position = -1;
        if (size <= 2) return position;
        for (int i = 0; i < size; i++) {
            byte b = buffer[i];
            byte b1 = buffer[i + 1];
            byte b2 = buffer[i + 2];
            if (b == 0 && b1 == 1 && b2 == 2) return i;
        }
        return position;
    }
}
