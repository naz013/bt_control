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

public class MultimeterActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 3;

    private TextView mMeterField;
    private TextView mBlockView;
    private int mSelectedId;

    private BluetoothAdapter mBtAdapter = null;
    private OriginalChatService mBtService = null;

    private static Activity activity;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_DEVICE_NAME:
                    showConnectedDeviceName(msg);
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
        activity = this;
        setContentView(R.layout.activity_multimeter);
        initBluetoothAdapter();
        initActionBar();
        initButtons();
        mMeterField = (TextView) findViewById(R.id.meterField);
        initBlockView();
    }

    public static Activity getActivity() {
        return activity;
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

    private void initBluetoothAdapter() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initButtons() {
        findViewById(R.id.resistanceButton).setOnClickListener(mListener);
        findViewById(R.id.voltageButton).setOnClickListener(mListener);
        findViewById(R.id.currentButton).setOnClickListener(mListener);
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        if (toolbar != null) {
            toolbar.setTitle(R.string.multimeter);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        }
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

    private void sendMessage(String message) {
        if (mBtService.getState() != OriginalChatService.STATE_CONNECTED) {
            resumeBtService();
        }
        mBtService.writeMessage(message.getBytes());
    }

    private void postResponse(Message msg) {
        byte[] readBuff = (byte[]) msg.obj;
        String data = new String(readBuff, 0, msg.arg1);
        String v;
        if (data.startsWith(Constants.rV)) {
            data = data.replace(Constants.rV, "");
            v = data.trim();
        } else if (data.startsWith(Constants.rI)) {
            data = data.replace(Constants.rI, "");
            v = data.trim();
        } else if (data.startsWith(Constants.rR)) {
            data = data.replace(Constants.rR, "");
            v = data.trim();
        } else {
            v = getString(R.string.no_key);
        }
        mMeterField.setText(v);
    }

    private void showConnectedDeviceName(Message msg) {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != RESULT_OK) {
            requestBtEnable();
        }
    }

    @Override
    public void onBackPressed() {
        closeScreen();
    }
}
