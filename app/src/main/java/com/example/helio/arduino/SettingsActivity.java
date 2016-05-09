package com.example.helio.arduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.backdoor.shared.Constants;
import com.example.helio.arduino.dso.DSOActivity;

public class SettingsActivity extends AppCompatActivity {

    private TextView mNameView;
    private TextView mAddressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initActionBar();
        initViews();

        loadBtDevice();
    }

    private void initViews() {
        mNameView = (TextView) findViewById(R.id.deviceName);
        mAddressView = (TextView) findViewById(R.id.deviceAddress);
        findViewById(R.id.disconnectButton).setOnClickListener(buttonClick);
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        if (toolbar != null) {
            toolbar.setTitle(R.string.settings);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        }
    }

    private void loadBtDevice() {
        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        String mAddress = preferences.getString(Constants.DEVICE_ADDRESS, null);
        if (mAddress != null) {
            BluetoothDevice device = mAdapter.getRemoteDevice(mAddress);
            mNameView.setText(device.getName());
            mAddressView.setText(mAddress);
        }
    }

    private final View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            removeBtDevice();
        }
    };

    private void removeBtDevice() {
        removePrefs();
        if (DSOActivity.getActivity() != null) {
            DSOActivity.getActivity().finish();
        }
        if (MainActivity.getActivity() != null) {
            MainActivity.getActivity().finish();
        }
        if (SignalActivity.getActivity() != null) {
            SignalActivity.getActivity().finish();
        }
        if (MultimeterActivity.getActivity() != null) {
            MultimeterActivity.getActivity().finish();
        }
        startActivity(new Intent(getApplicationContext(), SplashActivity.class));
        finish();
    }

    private void removePrefs() {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        if (preferences.contains(Constants.DEVICE_ADDRESS)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(Constants.DEVICE_ADDRESS);
            editor.commit();
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
}
