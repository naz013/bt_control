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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class StartActivity extends AppCompatActivity implements DeviceClickListener, ThreadListener {

    private static final int REQUEST_ENABLE_BT = 15;
    private Button pairButton;
    private Button testButton;
    private RecyclerView deviceList;
    private ProgressDialog mDialog;
    private BluetoothAdapter bluetoothAdapter = null;
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    private List<String> mDeviceNames = new ArrayList<>();
    private DevicesRecyclerAdapter mAdapter;
    private BroadcastReceiver mPairReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (loadDevice() != null && !loadDevice().matches("")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        setContentView(R.layout.activity_start);

        deviceList = (RecyclerView) findViewById(R.id.deviceList);
        deviceList.setHasFixedSize(true);
        deviceList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DevicesRecyclerAdapter(StartActivity.this, mDeviceNames, StartActivity.this);
        deviceList.setAdapter(mAdapter);

        pairButton = (Button) findViewById(R.id.pairButton);
        testButton = (Button) findViewById(R.id.testButton);
        pairButton.setOnClickListener(visibleClick);
        testButton.setOnClickListener(chatClick);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    View.OnClickListener chatClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openChat();
        }
    };

    private void openChat() {
        startActivity(new Intent(this, com.example.helio.arduino.chat_test.bluetoothchat.MainActivity.class));
    }

    View.OnClickListener visibleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            selectDevice();
        }
    };

    private void selectDevice() {
        if (!checkLocationPermission()) {
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            switchView();
        }
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
        pairButton.setVisibility(View.GONE);
        testButton.setVisibility(View.GONE);
        deviceList.setVisibility(View.VISIBLE);
    }

    private void startSearching() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        bluetoothAdapter.startDiscovery();
    }

    private void pairDevice(BluetoothDevice device) {
        bluetoothAdapter.cancelDiscovery();
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
        mPairReceiver = new PairingRequest();
        registerReceiver(mPairReceiver, filter);
        try {
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e("pairDevice()", e.getMessage());
        }
    }

    private String loadDevice() {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        String address = preferences.getString(Constants.DEVICE_ADDRESS, null);
        return address;
    }

    private void saveDevice(String address) {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.DEVICE_ADDRESS, address);
        editor.commit();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceNames.add(device.getName() + "\n" + device.getAddress());
                mDevices.add(device);

                Log.i("BT", device.getName() + "\n" + device.getAddress());
                mAdapter = new DevicesRecyclerAdapter(StartActivity.this, mDeviceNames, StartActivity.this);
                deviceList.setAdapter(mAdapter);
            }
        }
    };

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
    protected void onResume() {
        super.onResume();
        Log.d("TAG", "onResume");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            switchView();
        }
    }

    @Override
    protected void onDestroy() {
        bluetoothAdapter.cancelDiscovery();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View view, int position) {
        BluetoothDevice device = mDevices.get(position);
        mDialog = ProgressDialog.show(this, "Bluetooth", "Connecting to " + device.getName(), true, false);
        pairDevice(device);
    }

    @Override
    public void onFinish(String address) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        if (mPairReceiver != null) {
            unregisterReceiver(mPairReceiver);
        }
        saveDevice(address);
        startActivity(new Intent(this, MainActivity.class));
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        finish();
    }

    public class PairingRequest extends BroadcastReceiver {
        public PairingRequest() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
                    Log.d("PIN", " " + pin);
                    Log.d("Bonded", device.getName());
                    try {
                        BluetoothDevice.class.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, false);
                        BluetoothDevice.class.getClass().getMethod("cancelPairingUserInput").invoke(device);
                        byte[] pinBytes = (byte[]) BluetoothDevice.class.getClass().getMethod("convertPinToBytes", String.class).invoke(device, "" + pin);
                        BluetoothDevice.class.getClass().getMethod("setPin", byte[].class).invoke(device, pinBytes);
                        onFinish(device.getAddress());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
