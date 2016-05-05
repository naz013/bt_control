package com.backdoor.btsimulator;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.backdoor.shared.Constants;
import com.backdoor.shared.JMessage;
import com.backdoor.shared.OriginalChatService;

public class MainActivity extends AppCompatActivity implements MultimeterListener {

    private static final int REQUEST_ENABLE_BT = 3;

    private BluetoothAdapter mBtAdapter = null;
    private OriginalChatService mBtService = null;

    private boolean mIsCreateCheck;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    readMessage(msg);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    getDeviceName(msg);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initActionBar();
        if (checkLocationPermission(103)) {
            initBtAdapter();
            checkBtAvailability();
            if (!checkBtAdapterStatus()) {
                requestBtEnable();
            }
            mIsCreateCheck = false;
        } else mIsCreateCheck = true;
        replaceFragment(EmptyFragment.newInstance());
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        if (toolbar != null) {
            toolbar.setTitle(R.string.app_name);
        }
    }

    private void initBtAdapter() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void checkBtAvailability() {
        if (mBtAdapter == null) {
            showToast(getString(R.string.bluetooth_is_not_available));
            finish();
        }
    }

    private boolean checkBtAdapterStatus() {
        return mBtAdapter.isEnabled();
    }

    private void requestBtEnable() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    private void startBtService() {
        if (mBtService == null) {
            setupBtService();
        }
        if (!checkBtAdapterStatus()) {
            return;
        }
        if (mBtService.getState() == OriginalChatService.STATE_NONE) {
            mBtService.start();
            ensureDiscoverable();
        }
    }

    private void ensureDiscoverable() {
        if (mBtAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            discoverRequest();
        }
    }

    private void discoverRequest() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    private void setupBtService() {
        mBtService = new OriginalChatService(this, mHandler);
    }

    private void readMessage(Message msg) {
        byte[] readBuff = (byte[]) msg.obj;
        String data = new String(readBuff, 0, msg.arg1);
        JMessage jMessage = new JMessage(data);
        if (jMessage.hasFlag()) {
            String flag = jMessage.getFlag();
            workWithFlag(flag, msg);
        }
    }

    private void workWithFlag(String flag, Message msg) {
        if (flag == null) {
            return;
        }
        if (flag.matches(Constants.I) || flag.matches(Constants.V) || flag.matches(Constants.R)) {
            replaceFragment(MultimeterFragment.newInstance(msg));
        } else if (flag.matches(Constants.C)) {
            replaceFragment(DSOFragment.newInstance());
        } else if (flag.matches(Constants.G)) {
            replaceFragment(SignalFragment.newInstance(msg));
        } else {
            replaceFragment(EmptyFragment.newInstance());
            startBtService();
        }
    }

    private void getDeviceName(Message msg) {
        String mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
        showToast(getString(R.string.connected_to) + " " + mConnectedDeviceName);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_fragment, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        try {
            ft.commitAllowingStateLoss();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private boolean checkLocationPermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
                return false;
            }
            return true;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtService != null) {
            mBtService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mIsCreateCheck && checkLocationPermission(102)) {
            startBtService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 102:
                startBtService();
                break;
            case 103:
                initBtAdapter();
                checkBtAvailability();
                if (!checkBtAdapterStatus()) {
                    requestBtEnable();
                } else {
                    ensureDiscoverable();
                }
                mIsCreateCheck = false;
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    if (checkBtAdapterStatus()) {
                        setupBtService();
                    }
                }
                break;
        }
    }

    @Override
    public void obtainData(byte[] value) {
        if (mBtService != null) {
            if (mBtService.getState() == OriginalChatService.STATE_CONNECTED) {
                mBtService.writeMessage(value);
            } else {
                mBtService.start();
            }
        } else {
            startBtService();
        }
    }
}
