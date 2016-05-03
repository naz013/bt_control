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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.backdoor.shared.Constants;
import com.backdoor.shared.OriginalChatService;
import com.backdoor.shared.SignalObject;

public class SignalActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 3;

    private BluetoothDevice mConnectedDevice = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private OriginalChatService mChatService = null;

    private Spinner waveType;
    private Spinner freqSelector;
    private EditText freqField;
    private EditText magnitudeField;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_DEVICE_NAME:
                    getDeviceName(msg);
                    break;
                case Constants.MESSAGE_TOAST:
                    showMessage(msg);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signal);
        initBluetoothAdapter();
        initActionBar();
        initViews();
        initButtons();
    }

    private void initViews() {
        waveType = (Spinner) findViewById(R.id.waveType);
        freqSelector = (Spinner) findViewById(R.id.freqSelector);
        freqField = (EditText) findViewById(R.id.freqField);
        magnitudeField = (EditText) findViewById(R.id.magnitudeField);
    }

    private void initBluetoothAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initButtons() {
        findViewById(R.id.generateButton).setOnClickListener(mListener);
        findViewById(R.id.terminateButton).setOnClickListener(mListener);
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle(R.string.signal_generator);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    }

    private void generateSignal() {
        sendSignal();
    }

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.generateButton:
                    generateSignal();
                    break;
                case R.id.terminateButton:
                    terminateSignal();
                    break;
            }
        }
    };

    private void terminateSignal() {
        sendMessage();
    }

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
    }

    private void sendMessage() {
        if (mChatService.getState() != OriginalChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(Constants.FLAG, Constants.T);
        mChatService.writeBundle(bundle);
    }

    private void sendSignal() {
        if (mChatService.getState() != OriginalChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        String freqString = freqField.getText().toString().trim();
        if (freqString.matches("")) {
            showToast(getString(R.string.empty_frequency));
            return;
        }
        int freq = Integer.parseInt(freqString);

        String magnitudeString = magnitudeField.getText().toString().trim();
        if (magnitudeString.matches("")) {
            showToast(getString(R.string.empty_magnitude));
            return;
        }
        int magn = Integer.parseInt(magnitudeString);

        SignalObject object = new SignalObject(waveType.getSelectedItemPosition(), freq, freqSelector.getSelectedItemPosition(), magn);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.SIGNAL, object.toJson().toString());
        mChatService.writeBundle(bundle);
    }

    private void getDeviceName(Message msg) {
        String mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
        showToast(getString(R.string.connected_to) + " " + mConnectedDeviceName);
    }

    private void showMessage(Message msg) {
        String message = msg.getData().getString(Constants.TOAST);
        if (message == null) return;
        if (message.startsWith("Unable") || message.startsWith("Device")) {
            if (mChatService.getState() == OriginalChatService.STATE_NONE) {
                mChatService.start();
            }
            if (mChatService.getState() == OriginalChatService.STATE_LISTEN) {
                connectDevice(true);
            }
        } else {
            showToast(msg.getData().getString(Constants.TOAST));
        }
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void connectDevice(boolean secure) {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Activity.MODE_PRIVATE);
        String mAddress = preferences.getString(Constants.DEVICE_ADDRESS, null);
        if (mAddress != null) {
            mConnectedDevice = mBluetoothAdapter.getRemoteDevice(mAddress);
            mChatService.connect(mConnectedDevice, secure);
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
}
