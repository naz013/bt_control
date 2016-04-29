package com.example.helio.arduino;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.helio.arduino.transferring.BluetoothChatService;
import com.example.helio.arduino.transferring.SelectionActivity;

import java.util.ArrayList;
import java.util.List;

public class StartActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 15;
    private String mDeviceAddress;
    private String mDeviceName;

    private Button mPairButton;
    private Button mTestButton;
    private RecyclerView mDeviceList;
    private ProgressDialog mDialog;
    private final BluetoothAdapter mBtAdapter = null;
    private final List<BluetoothDevice> mDevices = new ArrayList<>();
    private DevicesRecyclerAdapter mAdapter;
    private BluetoothChatService mChatService = null;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case com.example.helio.arduino.transferring.Constants.MESSAGE_STATE_CHANGE:
                    workWithServiceState(msg);
                    break;
                case com.example.helio.arduino.transferring.Constants.MESSAGE_TOAST:
                    showMessageToast(msg);
                    break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDevices.add(device);
                Log.i("BT", device.getName() + "\n" + device.getAddress());
                mAdapter.addDevice(device.getName() + "\n" + device.getAddress());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        initDeviceList();
        initButtons();
    }

    private void initButtons() {
        mPairButton = (Button) findViewById(R.id.pairButton);
        mTestButton = (Button) findViewById(R.id.testButton);
        mPairButton.setOnClickListener(visibleClick);
        mTestButton.setOnClickListener(chatClick);
    }

    private void initDeviceList() {
        mDeviceList = (RecyclerView) findViewById(R.id.deviceList);
        mDeviceList.setHasFixedSize(true);
        mDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DevicesRecyclerAdapter(StartActivity.this, mListener);
        mDeviceList.setAdapter(mAdapter);
    }

    private void setupService() {
        mChatService = new BluetoothChatService(this, mHandler);
    }

    private final View.OnClickListener chatClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openChat();
        }
    };

    private void openChat() {
        startActivity(new Intent(this, SelectionActivity.class));
    }

    private final View.OnClickListener visibleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            selectDevice();
        }
    };

    private void selectDevice() {
        startActivity(new Intent(this, MainActivity.class));
    }

    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 102);
                return false;
            }
            return true;
        }
        return true;
    }

    private void switchView() {
        startSearching();
        mPairButton.setVisibility(View.GONE);
        mTestButton.setVisibility(View.GONE);
        mDeviceList.setVisibility(View.VISIBLE);
    }

    private void startSearching() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        mBtAdapter.startDiscovery();
    }

    private void pairDevice(BluetoothDevice device) {
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        if (mDeviceAddress != null) {
            while (true) {
                if (mChatService.getState() == BluetoothChatService.STATE_LISTEN) {
                    mChatService.connect(device, false);
                    break;
                }
            }
        }
    }

    private void saveDevice(String address) {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.DEVICE_ADDRESS, address);
        editor.commit();
    }

    private void showMessageToast(Message msg) {
        Toast.makeText(this, msg.getData().getString(com.example.helio.arduino.transferring.Constants.TOAST),
                Toast.LENGTH_SHORT).show();
    }

    private void workWithServiceState(Message msg) {
        switch (msg.arg1) {
            case BluetoothChatService.STATE_CONNECTED:
                onFinish(mDeviceAddress);
                break;
            case BluetoothChatService.STATE_CONNECTING:
                mDialog = ProgressDialog.show(this, getString(R.string.bluetooth),
                        getString(R.string.title_connecting) + " " + mDeviceName, true, false);
                break;
            case BluetoothChatService.STATE_LISTEN:
            case BluetoothChatService.STATE_NONE:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 102:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectDevice();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            switchView();
        }
    }

    private final DeviceClickListener mListener = new DeviceClickListener() {
        @Override
        public void onClick(View view, int position) {
            BluetoothDevice device = mDevices.get(position);
            mDialog = ProgressDialog.show(StartActivity.this, getString(R.string.bluetooth),
                    getString(R.string.title_connecting) + " " + device.getName(), true, false);
            pairDevice(device);
        }
    };

    public void onFinish(String address) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        saveDevice(address);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
