package com.example.helio.arduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.example.helio.arduino.core.BluetoothService;
import com.example.helio.arduino.core.Constants;
import com.example.helio.arduino.dso.DsoActivity;
import com.example.helio.arduino.multimeter.MultimeterActivity;
import com.example.helio.arduino.signal.SignalActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT_AUTO = 16;

    private BluetoothAdapter mBtAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        initActionBar();
        initButtons();
    }

    private void initButtons() {
        findViewById(R.id.multimeterButton).setOnClickListener(mListener);
        findViewById(R.id.dsoButton).setOnClickListener(mListener);
        findViewById(R.id.signalButton).setOnClickListener(mListener);
        findViewById(R.id.fab).setOnClickListener(mListener);
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setTitle(R.string.app_name);
        }
    }

    private void openSignal() {
        startActivity(new Intent(this, SignalActivity.class));
    }

    private void openDSO() {
        startActivity(new Intent(this, DsoActivity.class));
    }

    private void openMultimeter() {
        startActivity(new Intent(this, MultimeterActivity.class));
    }

    private final View.OnClickListener mListener = v -> {
        switch (v.getId()) {
            case R.id.multimeterButton:
                openMultimeter();
                break;
            case R.id.dsoButton:
                openDSO();
                break;
            case R.id.signalButton:
                openSignal();
                break;
            case R.id.fab:
                openManual();
                break;
        }
    };

    private void openManual() {
        startActivity(new Intent(this, ManualActivity.class));
    }

    private void requestBtEnabling(int requestCode) {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, requestCode);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBtAdapter.isEnabled()) {
            requestBtEnabling(REQUEST_ENABLE_BT_AUTO);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, BluetoothService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT_AUTO && resultCode != RESULT_OK) {
            requestBtEnabling(REQUEST_ENABLE_BT_AUTO);
        }
    }

    private void removeBtDevice() {
        removePrefs();
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
            case R.id.action_disconnect:
                showConfirmationDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String getDeviceName() {
        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        String mAddress = preferences.getString(Constants.DEVICE_ADDRESS, null);
        if (mAddress != null) {
            BluetoothDevice device = mAdapter.getRemoteDevice(mAddress);
            return device.getName();
        }
        return "";
    }

    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(getString(R.string.are_you_sure_disconnect) + " " + getDeviceName());
        builder.setPositiveButton(getString(R.string.forgot), (dialog, which) -> {
            dialog.dismiss();
            removeBtDevice();
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
