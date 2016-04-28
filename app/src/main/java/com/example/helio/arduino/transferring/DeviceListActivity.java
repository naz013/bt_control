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
import android.widget.Toast;

import com.example.helio.arduino.DeviceClickListener;
import com.example.helio.arduino.DevicesRecyclerAdapter;
import com.example.helio.arduino.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 3;
    private String mDeviceAddress;
    private String mDeviceName;

    private final List<BluetoothDevice> mDevices = new ArrayList<>();
    private DevicesRecyclerAdapter mAdapter;
    private OriginalChatService mChatService = null;
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
        findViewById(R.id.button_scan).setOnClickListener(new View.OnClickListener() {
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
        mAdapter = new DevicesRecyclerAdapter(this, mListener);
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

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    }

    private void doDiscovery() {
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
                addDeviceToList(device);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                addNoDevicesToList();
            }
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
        Log.i("BT", device.getName() + "\n" + device.getAddress());
        mAdapter.addDevice(device.getName() + "\n" + device.getAddress());
    }

    private void openChatRoom() {
        startActivity(new Intent(this, ChatActivity.class)
                .putExtra(getString(R.string.intent_server_key), false));
        finish();
    }

    private void saveDevice(String address) {
        SharedPreferences preferences = getSharedPreferences(com.example.helio.arduino.Constants.PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(com.example.helio.arduino.Constants.DEVICE_ADDRESS, address);
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
            case Constants.MESSAGE_TOAST:
                Toast.makeText(this, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
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
                openChatRoom();
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
            requestBluetoothEnabling();
        } else if (mChatService == null) {
            setupService();
        }
    }

    private void requestBluetoothEnabling() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopChatService();
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
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
                if (mChatService.getState() == OriginalChatService.STATE_LISTEN) {
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
