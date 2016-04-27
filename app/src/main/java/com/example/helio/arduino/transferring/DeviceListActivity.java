package com.example.helio.arduino.transferring;

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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.example.helio.arduino.DeviceClickListener;
import com.example.helio.arduino.DevicesRecyclerAdapter;
import com.example.helio.arduino.R;

import java.util.ArrayList;
import java.util.List;

public class DeviceListActivity extends AppCompatActivity implements DeviceClickListener {

    private static final String TAG = "DeviceListActivity";
    private static final int REQUEST_ENABLE_BT = 3;
    private String mDeviceAddress;
    private String mDeviceName;

    private Toolbar toolbar;

    private List<BluetoothDevice> mDevices = new ArrayList<>();
    private DevicesRecyclerAdapter mAdapter;
    private BluetoothChatService mChatService = null;
    private BluetoothAdapter mBtAdapter;

    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);
        initActionBar();
        initButton();
        initReceiver();

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    private void initButton() {
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });
        initDeviceList();
    }

    private void initDeviceList() {
        RecyclerView mDeviceList = (RecyclerView) findViewById(R.id.new_devices);
        mDeviceList.setHasFixedSize(true);
        mDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DevicesRecyclerAdapter(this, this);
        mDeviceList.setAdapter(mAdapter);
    }

    private void initActionBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    }

    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");
        if (!checkLocationPermission()) {
            return;
        }

        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        mBtAdapter.startDiscovery();
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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDevices.add(device);
                Log.i("BT", device.getName() + "\n" + device.getAddress());
                mAdapter.addDevice(device.getName() + "\n" + device.getAddress());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mAdapter.getItemCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mAdapter.addDevice(noDevices);
                }
            }
        }
    };

    private void openChatRoom() {
        startActivity(new Intent(this, ChatActivity.class));
    }

    private void saveDevice(String address) {
        SharedPreferences preferences = getSharedPreferences(com.example.helio.arduino.Constants.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(com.example.helio.arduino.Constants.DEVICE_ADDRESS, address);
        editor.commit();
    }

    private void setupService() {
        mChatService = new BluetoothChatService(this, mHandler);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AppCompatActivity activity = DeviceListActivity.this;
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            if (mDialog != null && mDialog.isShowing()) {
                                mDialog.dismiss();
                            }
                            saveDevice(mDeviceAddress);
                            openChatRoom();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            mDialog = ProgressDialog.show(activity,
                                    activity.getString(R.string.bluetooth),
                                    activity.getString(R.string.title_connecting) + " " + mDeviceName, true, false);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                            if (mDialog != null && mDialog.isShowing()) {
                                mDialog.dismiss();
                            }
                            saveDevice(mDeviceAddress);
                            openChatRoom();
                            break;
                        case BluetoothChatService.STATE_NONE:
                            break;
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mChatService == null) {
            setupService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }

        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    @Override
    public void onClick(View view, int position) {
        mBtAdapter.cancelDiscovery();
        BluetoothDevice device = mDevices.get(position);
        mDeviceAddress = device.getAddress();
        mDeviceName = device.getName();
        connectDevice(device);
    }

    private void connectDevice(BluetoothDevice device) {
        if (mDeviceAddress != null) {
            while (true) {
                if (mChatService.getState() == BluetoothChatService.STATE_LISTEN) {
                    mChatService.connect(device, true);
                    break;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            doDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 102:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doDiscovery();
                }
                break;
        }
    }
}
