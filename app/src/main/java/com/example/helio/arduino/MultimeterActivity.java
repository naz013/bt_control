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
import android.widget.TextView;
import android.widget.Toast;

import com.backdoor.shared.Constants;
import com.backdoor.shared.JMessage;
import com.backdoor.shared.OriginalChatService;

public class MultimeterActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 3;

    private TextView meterField;
    private TextView blockView;
    private int mSelectedId;

    private BluetoothAdapter mBluetoothAdapter = null;
    private OriginalChatService mChatService = null;

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
                case Constants.MESSAGE_READ:
                    postResponse(msg);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multimeter);
        initBluetoothAdapter();
        initActionBar();
        initButtons();
        meterField = (TextView) findViewById(R.id.meterField);
        initBlockView();
    }

    private void initBlockView() {
        blockView = (TextView) findViewById(R.id.blockView);
        blockView.setVisibility(View.VISIBLE);
        blockView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
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
        sendMessage(Constants.I);
    }

    private void showResistance() {
        sendMessage(Constants.R);
    }

    private void showVoltage() {
        sendMessage(Constants.V);
    }

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mSelectedId == v.getId()) {
                v.setSelected(false);
                mSelectedId = -1;
                return;
            } else {
                deselectAll();
                v.setSelected(true);
                mSelectedId = v.getId();
            }
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

    private void deselectAll() {
        findViewById(R.id.resistanceButton).setSelected(false);
        findViewById(R.id.voltageButton).setSelected(false);
        findViewById(R.id.currentButton).setSelected(false);
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

    private void sendMessage(String message) {
        if (mChatService.getState() != OriginalChatService.STATE_CONNECTED) {
            resumeBluetoothService();
        }

        String msg = new JMessage().putFlag(message).asString();
        mChatService.writeMessage(msg.getBytes());
    }

    private void postResponse(Message msg) {
        byte[] readBuff = (byte[]) msg.obj;
        String data = new String(readBuff, 0, msg.arg1);
        JMessage jMessage = new JMessage(data);
        String v;
        if (jMessage.hasVoltage()) {
            v = jMessage.getVoltage();
        } else if (jMessage.hasCurrent()) {
            v = jMessage.getCurrent();
        } else if (jMessage.hasResistance()) {
            v = jMessage.getResistance();
        } else {
            v = getString(R.string.no_key);
        }
        meterField.setText(v);
    }

    private void getDeviceName(Message msg) {
        String mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
        showToast(getString(R.string.connected_to) + " " + mConnectedDeviceName);
        blockView.setVisibility(View.GONE);
    }

    private void showMessage(Message msg) {
        String message = msg.getData().getString(Constants.TOAST);
        if (message == null) {
            return;
        }
        if (message.startsWith(Constants.UNABLE)) {
            if (mChatService.getState() == OriginalChatService.STATE_NONE) {
                mChatService.start();
            }
            if (mChatService.getState() == OriginalChatService.STATE_LISTEN) {
                connectDevice(true);
            }
        }
    }

    private void stopBTService() {
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    private void resumeBluetoothService() {
        if (mChatService != null) {
            startBluetoothService();
        } else {
            setupConnection();
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
            BluetoothDevice mConnectedDevice = mBluetoothAdapter.getRemoteDevice(mAddress);
            mChatService.connect(mConnectedDevice, secure);
        }
    }

    private void sendCancelMessage() {
        if (mChatService.getState() != OriginalChatService.STATE_CONNECTED) {
            resumeBluetoothService();
        }

        String msg = new JMessage().putFlag(Constants.T).asString();
        mChatService.writeMessage(msg.getBytes());
    }

    @Override
    public void onStart() {
        super.onStart();
        checkAdapterStatus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        stopBTService();
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeBluetoothService();
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
                sendCancelMessage();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        sendCancelMessage();
        finish();
    }
}
