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
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.backdoor.shared.Constants;
import com.backdoor.shared.JMessage;
import com.backdoor.shared.OriginalChatService;
import com.backdoor.shared.SignalObject;

public class SignalActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 3;

    private BluetoothAdapter mBtAdapter = null;
    private OriginalChatService mBtService = null;

    private Spinner mWaveSelector;
    private Spinner mFrequencySelector;
    private EditText mFrequencyField;
    private EditText mMagnitudeField;
    private TextView mBlockView;

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
        initBtAdapter();
        initActionBar();
        initViews();
        initButtons();
        initBlockView();
    }

    private void initViews() {
        mWaveSelector = (Spinner) findViewById(R.id.waveType);
        mFrequencySelector = (Spinner) findViewById(R.id.freqSelector);
        mFrequencyField = (EditText) findViewById(R.id.freqField);
        mMagnitudeField = (EditText) findViewById(R.id.magnitudeField);
    }

    private void initBlockView() {
        mBlockView = (TextView) findViewById(R.id.blockView);
        mBlockView.setVisibility(View.VISIBLE);
        mBlockView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    private void initBtAdapter() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initButtons() {
        findViewById(R.id.generateButton).setOnClickListener(mListener);
        findViewById(R.id.terminateButton).setOnClickListener(mListener);
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        if (toolbar != null) {
            toolbar.setTitle(R.string.signal_generator);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        }
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
                    sendTerminateMessage();
                    break;
            }
        }
    };

    private void requestBtEnable() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    private void checkBtAdapterStatus() {
        if (!mBtAdapter.isEnabled()) {
            requestBtEnable();
        } else if (mBtService == null) {
            setupBtService();
        }
    }

    private void setupBtService() {
        mBtService = new OriginalChatService(this, mHandler);
    }

    private void sendTerminateMessage() {
        if (mBtService.getState() != OriginalChatService.STATE_CONNECTED) {
            resumeBtService();
        }
        String msg = new JMessage().putFlag(Constants.T).asString();
        mBtService.writeMessage(msg.getBytes());
    }

    private void sendSignal() {
        if (mBtService.getState() != OriginalChatService.STATE_CONNECTED) {
            resumeBtService();
        }
        String freqString = mFrequencyField.getText().toString().trim();
        if (freqString.matches("")) {
            showToast(getString(R.string.empty_frequency));
            return;
        }
        String magnitudeString = mMagnitudeField.getText().toString().trim();
        if (magnitudeString.matches("")) {
            showToast(getString(R.string.empty_magnitude));
            return;
        }
        int frequency = Integer.parseInt(freqString);
        int magnitude = Integer.parseInt(magnitudeString);
        SignalObject object = new SignalObject(mWaveSelector.getSelectedItemPosition(), frequency,
                mFrequencySelector.getSelectedItemPosition(), magnitude);
        String msg = new JMessage()
                .putSignal(object)
                .putFlag(Constants.G)
                .asString();
        mBtService.writeMessage(msg.getBytes());
    }

    private void getDeviceName(Message msg) {
        String mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
        showToast(getString(R.string.connected_to) + " " + mConnectedDeviceName);
        mBlockView.setVisibility(View.GONE);
    }

    private void showMessage(Message msg) {
        String message = msg.getData().getString(Constants.TOAST);
        if (message == null) {
            return;
        }
        if (message.startsWith(Constants.UNABLE)) {
            if (mBtService.getState() == OriginalChatService.STATE_NONE) {
                mBtService.start();
            }
            if (mBtService.getState() == OriginalChatService.STATE_LISTEN) {
                connectToBtDevice(true);
            }
        }
    }

    private void stopBtService() {
        if (mBtService != null) {
            mBtService.stop();
        }
    }

    private void resumeBtService() {
        if (mBtService != null) {
            startBtService();
        } else {
            setupBtService();
            startBtService();
        }
    }

    private void startBtService() {
        if (mBtService.getState() == OriginalChatService.STATE_NONE) {
            mBtService.start();
            while (true) {
                if (mBtService.getState() == OriginalChatService.STATE_LISTEN) {
                    connectToBtDevice(true);
                    break;
                }
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void connectToBtDevice(boolean secure) {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Activity.MODE_PRIVATE);
        String mAddress = preferences.getString(Constants.DEVICE_ADDRESS, null);
        if (mAddress != null) {
            BluetoothDevice mConnectedDevice = mBtAdapter.getRemoteDevice(mAddress);
            mBtService.connect(mConnectedDevice, secure);
        }
    }

    private void closeScreen() {
        sendTerminateMessage();
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        checkBtAdapterStatus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        stopBtService();
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeBtService();
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
                closeScreen();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        closeScreen();
    }
}
