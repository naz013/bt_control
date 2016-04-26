package com.example.helio.arduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Method;

public class SettingsActivity extends AppCompatActivity {

    private Button mDisconnectButton;
    private TextView mNameView;
    private TextView mAddressView;

    private String mAddress;

    private BluetoothAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mNameView = (TextView) findViewById(R.id.deviceName);
        mAddressView = (TextView) findViewById(R.id.deviceAddress);

        mDisconnectButton = (Button) findViewById(R.id.disconnectButton);
        mDisconnectButton.setOnClickListener(visibleClick);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        loadDevice();
    }

    private void loadDevice() {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        mAddress = preferences.getString(Constants.DEVICE_ADDRESS, null);
        if (mAddress != null) {
            BluetoothDevice device = mAdapter.getRemoteDevice(mAddress);
            mNameView.setText(device.getName());
            mAddressView.setText(mAddress);
        }
    }

    View.OnClickListener visibleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            unPairDevice();
        }
    };

    private void unPairDevice() {
        BluetoothDevice device = mAdapter.getRemoteDevice(mAddress);
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e("TAG", e.getMessage());
        }
        finish();
    }
}
