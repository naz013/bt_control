package com.example.helio.arduino;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helio.arduino.transferring.OriginalChatService;

import static com.example.helio.arduino.transferring.Constants.CURRENT;
import static com.example.helio.arduino.transferring.Constants.DEVICE_NAME;
import static com.example.helio.arduino.transferring.Constants.KEY_CURRENT;
import static com.example.helio.arduino.transferring.Constants.KEY_MULTIMETER;
import static com.example.helio.arduino.transferring.Constants.KEY_RESISTANCE;
import static com.example.helio.arduino.transferring.Constants.KEY_VOLTAGE;
import static com.example.helio.arduino.transferring.Constants.MESSAGE_DEVICE_NAME;
import static com.example.helio.arduino.transferring.Constants.MESSAGE_TOAST;
import static com.example.helio.arduino.transferring.Constants.RESISTANCE;
import static com.example.helio.arduino.transferring.Constants.TOAST;
import static com.example.helio.arduino.transferring.Constants.VOLTAGE;

public class MultimeterActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 3;

    private TextView meterField;

    private BluetoothDevice mConnectedDevice = null;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter = null;
    private OriginalChatService mChatService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multimeter);
        initBluetoothAdapter();
        initActionBar();
        initButtons();
        meterField = (TextView) findViewById(R.id.meterField);
    }

    private void initBluetoothAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initButtons() {
        findViewById(R.id.resistanceButton).setOnClickListener(mListener);
        findViewById(R.id.voltageButton).setOnClickListener(mListener);
        findViewById(R.id.currentButton).setOnClickListener(mListener);
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle(R.string.multimeter);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    }

    private void showCurrent() {
        sendMessage(getString(R.string.current_key));
    }

    private void showResistance() {
        sendMessage(getString(R.string.resistance_key));
    }

    private void showVoltage() {
        sendMessage(getString(R.string.voltage_key));
    }

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.resistanceButton:
                    showResistance();
                    break;
                case R.id.voltageButton:
                    showVoltage();
                    break;
                case R.id.currentButton:
                    showCurrent();
                    break;
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        checkAdapterStatus();
    }

    private void requestBluetoothEnable() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    private void checkAdapterStatus() {
        if (!mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
        } else if (mChatService == null) {
            setupConnection();
        }
    }

    private void setupConnection() {
        mChatService = new OriginalChatService(this, mHandler);
        mOutStringBuffer = new StringBuffer("");
    }

    private void sendMessage(String message) {
        if (mChatService.getState() != OriginalChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.writeMessage(send, KEY_MULTIMETER);
            mOutStringBuffer.setLength(0);
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_DEVICE_NAME:
                    getDeviceName(msg);
                    break;
                case MESSAGE_TOAST:
                    showMessage(msg);
                    break;
                case KEY_CURRENT:
                    postCurrentResponse(msg);
                    break;
                case KEY_RESISTANCE:
                    postResistanceResponse(msg);
                    break;
                case KEY_VOLTAGE:
                    postVoltageResponse(msg);
                    break;
            }
        }
    };

    private void postCurrentResponse(Message msg) {
        String message = msg.getData().getString(CURRENT);
        meterField.setText(message);
    }

    private void postVoltageResponse(Message msg) {
        String message = msg.getData().getString(VOLTAGE);
        meterField.setText(message);
    }

    private void postResistanceResponse(Message msg) {
        String message = msg.getData().getString(RESISTANCE);
        meterField.setText(message);
    }

    private void getDeviceName(Message msg) {
        String mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
        showToast(getString(R.string.connected_to) + " " + mConnectedDeviceName);
    }

    private void showMessage(Message msg) {
        String message = msg.getData().getString(TOAST);
        if (message == null) return;
        if (message.startsWith("Unable") || message.startsWith("Device")) {
            if (mChatService.getState() == OriginalChatService.STATE_NONE) {
                mChatService.start();
            }
            if (mChatService.getState() == OriginalChatService.STATE_LISTEN) {
                connectDevice(true);
            }
        }
        showToast(msg.getData().getString(TOAST));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopBTService();
    }

    private void stopBTService() {
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeBluetoothService();
    }

    private void resumeBluetoothService() {
        if (mChatService != null) {
            startBluetoothService();
        }
    }

    private void startBluetoothService() {
        if (mChatService.getState() == OriginalChatService.STATE_NONE) {
            mChatService.start();
            while (true) {
                if (mChatService.getState() == OriginalChatService.STATE_LISTEN) {
                    connectDevice(true);
                    break;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void connectDevice(boolean secure) {
        SharedPreferences preferences = getSharedPreferences(com.example.helio.arduino.Constants.PREFS, Activity.MODE_PRIVATE);
        String mAddress = preferences.getString(com.example.helio.arduino.Constants.DEVICE_ADDRESS, null);
        if (mAddress != null) {
            mConnectedDevice = mBluetoothAdapter.getRemoteDevice(mAddress);
            mChatService.connect(mConnectedDevice, secure);
        }
    }
}
