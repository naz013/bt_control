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
import android.view.View;
import android.widget.Button;

import com.backdoor.shared.Constants;
import com.backdoor.shared.OriginalChatService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StartActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 15;
    private static final int REQUEST_ENABLE_BT_AUTO = 16;
    private static final int REQUEST_CLICK = 105;
    private static final int REQUEST_AUTO = 102;
    private String mDeviceAddress;
    private String mDeviceName;

    private Button mPairButton;
    private RecyclerView mDeviceList;
    private ProgressDialog mDialog;
    private final List<BluetoothDevice> mDevices = new ArrayList<>();
    private DevicesRecyclerAdapter mAdapter;
    private OriginalChatService mChatService = null;
    private BluetoothAdapter mBtAdapter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addDeviceToList(device);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                addNoDevicesToList();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        initDeviceList();
        initButtons();
        initReceiver();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    private void initButtons() {
        mPairButton = (Button) findViewById(R.id.pairButton);
        mPairButton.setOnClickListener(visibleClick);
    }

    private void initDeviceList() {
        mDeviceList = (RecyclerView) findViewById(R.id.deviceList);
        mDeviceList.setHasFixedSize(true);
        mDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DevicesRecyclerAdapter(StartActivity.this, mListener);
        mDeviceList.setAdapter(mAdapter);
        if (mBtAdapter != null) {
            Set<BluetoothDevice> devices = mBtAdapter.getBondedDevices();
            for (BluetoothDevice device : devices) {
                String name = device.getName();
                String address = device.getAddress();
                mAdapter.addDevice(name + "\n" + address);
            }
        }
    }

    private void doDiscovery(int code) {
        if (!checkLocationPermission(code)) {
            return;
        }
        if (!mBtAdapter.isEnabled()) {
            requestBluetoothEnabling(REQUEST_ENABLE_BT);
        }
        cancelDiscovering();
        if (code == REQUEST_CLICK) {
            mBtAdapter.startDiscovery();
            mPairButton.setVisibility(View.GONE);
            mDeviceList.setVisibility(View.VISIBLE);
        }
    }

    private final View.OnClickListener visibleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.setVisibility(View.GONE);
            doDiscovery(REQUEST_CLICK);
        }
    };

    private void addNoDevicesToList() {
        if (mAdapter.getItemCount() == 0) {
            String noDevices = getString(R.string.none_found);
            mAdapter.addDevice(noDevices);
        }
    }

    private void addDeviceToList(BluetoothDevice device) {
        mDevices.add(device);
        mAdapter.addDevice(device.getName() + "\n" + device.getAddress());
    }

    private void saveDevice(String address) {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.DEVICE_ADDRESS, address);
        editor.commit();
    }

    private void setupService() {
        mChatService = new OriginalChatService(this, mHandler);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            splitByMessageType(msg);
        }
    };

    private void splitByMessageType(Message msg) {
        switch (msg.what) {
            case Constants.MESSAGE_STATE_CHANGE:
                obtainConnectionMessage(msg);
                break;
        }
    }

    private void obtainConnectionMessage(Message msg) {
        switch (msg.arg1) {
            case OriginalChatService.STATE_CONNECTED:
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                }
                saveDevice(mDeviceAddress);
                onFinish();
                break;
            case OriginalChatService.STATE_CONNECTING:
                mDialog = ProgressDialog.show(this, getString(R.string.bluetooth),
                        getString(R.string.title_connecting) + " " + mDeviceName, true, false);
                break;
            case OriginalChatService.STATE_LISTEN:
                break;
            case OriginalChatService.STATE_NONE:
                break;
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
    public void onStart() {
        super.onStart();
        if (!mBtAdapter.isEnabled()) {
            requestBluetoothEnabling(REQUEST_ENABLE_BT_AUTO);
        } else if (mChatService == null) {
            setupService();
        }
    }

    private void requestBluetoothEnabling(int requestCode) {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, requestCode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopChatService();
        cancelDiscovering();
        this.unregisterReceiver(mReceiver);
    }

    private void cancelDiscovering() {
        if (mBtAdapter != null && mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
    }

    private void stopChatService() {
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startChatService();
    }

    private void startChatService() {
        if (mChatService != null) {
            if (mChatService.getState() == OriginalChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    private final DeviceClickListener mListener = new DeviceClickListener() {
        @Override
        public void onClick(View view, int position) {
            mBtAdapter.cancelDiscovery();
            BluetoothDevice device = mDevices.get(position);
            mDeviceAddress = device.getAddress();
            mDeviceName = device.getName();
            connectDevice(device);
        }
    };

    private void connectDevice(BluetoothDevice device) {
        if (mDeviceAddress != null) {
            while (true) {
                if (mChatService != null) {
                    if (mChatService.getState() == OriginalChatService.STATE_LISTEN) {
                        mChatService.connect(device, true);
                        break;
                    }
                    if (mChatService.getState() == OriginalChatService.STATE_NONE) {
                        mChatService.start();
                    }
                } else {
                    setupService();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUTO:
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    checkLocationPermission(REQUEST_AUTO);
                }
                break;
            case REQUEST_CLICK:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doDiscovery(REQUEST_CLICK);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            doDiscovery(REQUEST_AUTO);
        }
    }

    private final View.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(StartActivity.this, MainActivity.class));
            finish();
        }
    };

    private void onFinish() {
        mDeviceList.setVisibility(View.GONE);
        mPairButton.setVisibility(View.VISIBLE);
        mPairButton.setText(R.string.go_to_main_menu);
        mPairButton.setOnClickListener(mClick);
    }
}
