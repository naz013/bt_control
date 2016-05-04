package com.backdoor.btsimulator;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.backdoor.shared.Constants;
import com.backdoor.shared.JMessage;
import com.backdoor.shared.OriginalChatService;

public class MainActivity extends AppCompatActivity implements MultimeterListener {

    private static final int REQUEST_ENABLE_BT = 3;

    private BluetoothAdapter mBluetoothAdapter = null;
    private OriginalChatService mChatService = null;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            showToast("KEY " + msg.what);
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    readMessage(msg);
                    break;
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
        setContentView(R.layout.activity_main);
        initActionBar();
        initBluetoothAdapter();
        checkBluetoothAvailability();
        replaceFragment(EmptyFragment.newInstance());
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        toolbar.setTitle(R.string.app_name);
    }

    private void initBluetoothAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void checkBluetoothAvailability() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_is_not_available, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void checkAdapterStatus() {
        if (!mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
        } else if (mChatService == null) {
            setupService();
        }
    }

    private void requestBluetoothEnable() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    private void resumeBluetoothService() {
        if (mChatService != null) {
            startBluetoothService();
        }
    }

    private void startBluetoothService() {
        if (mChatService.getState() == OriginalChatService.STATE_NONE) {
            mChatService.start();
            ensureDiscoverable();
        }
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            discoverRequest();
        }
    }

    private void discoverRequest() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    private void setupService() {
        mChatService = new OriginalChatService(this, mHandler);
    }

    private void readMessage(Message msg) {
        byte[] readBuff = (byte[]) msg.obj;
        String data = new String(readBuff, 0, msg.arg1);
        JMessage jMessage = new JMessage(data);
        if (jMessage.hasFlag()) {
            String flag = jMessage.getFlag();
            showToast("Has flag " + flag);
            workWithFlag(flag, msg);
        } else {
            showToast("No flag");
        }
    }

    private void workWithFlag(String flag, Message msg) {
        if (flag == null) return;
        if (flag.matches(Constants.I) || flag.matches(Constants.V) || flag.matches(Constants.R)) {
            replaceFragment(MultimeterFragment.newInstance(msg));
        } else if (flag.matches(Constants.C)) {
            replaceFragment(DSOFragment.newInstance());
        } else if (flag.matches(Constants.G)) {
            replaceFragment(SignalFragment.newInstance(msg));
        } else {
            replaceFragment(EmptyFragment.newInstance());
        }
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
        }
        showToast(msg.getData().getString(Constants.TOAST));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_fragment, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

    @Override
    public void onStart() {
        super.onStart();
        checkAdapterStatus();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeBluetoothService();
    }

    @Override
    public void obtainData(byte[] value) {
        mChatService.writeMessage(value);
    }
}
