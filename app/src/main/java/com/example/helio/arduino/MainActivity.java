package com.example.helio.arduino;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.example.helio.arduino.core.BluetoothService;
import com.example.helio.arduino.dso.DsoActivity;
import com.example.helio.arduino.signal.SignalActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT_AUTO = 16;

    private static Activity activity;
    private BluetoothAdapter mBtAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        initActionBar();
        initButtons();
    }

    public static Activity getActivity() {
        return activity;
    }

    private void initButtons() {
        findViewById(R.id.multimeterButton).setOnClickListener(mListener);
        findViewById(R.id.dsoButton).setOnClickListener(mListener);
        findViewById(R.id.signalButton).setOnClickListener(mListener);
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setTitle(R.string.main_menu);
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
        }
    };

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

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
