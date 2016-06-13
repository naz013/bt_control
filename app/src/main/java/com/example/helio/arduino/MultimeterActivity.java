package com.example.helio.arduino;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helio.arduino.core.BluetoothService;
import com.example.helio.arduino.core.ConnectionEvent;
import com.example.helio.arduino.core.Constants;
import com.example.helio.arduino.core.ControlEvent;
import com.example.helio.arduino.core.ResponseEvent;

import de.greenrobot.event.EventBus;

public class MultimeterActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 3;

    private TextView mMeterField;
    private TextView mBlockView;
    private int mSelectedId;

    private BluetoothAdapter mBtAdapter = null;

    private static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_multimeter);
        initBluetoothAdapter();
        initActionBar();
        initButtons();
        mMeterField = (TextView) findViewById(R.id.meterField);
        initBlockView();
    }

    public static Activity getActivity() {
        return activity;
    }

    public void onEvent(ResponseEvent responseEvent) {
        postResponse(responseEvent.getMsg());
    }

    public void onEvent(ConnectionEvent responseEvent) {
        if (responseEvent.isConnected()) {
            mBlockView.setVisibility(View.GONE);
        } else {
            mBlockView.setVisibility(View.VISIBLE);
        }
    }

    private void initBlockView() {
        mBlockView = (TextView) findViewById(R.id.blockView);
        mBlockView.setVisibility(View.VISIBLE);
        mBlockView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    private void initBluetoothAdapter() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initButtons() {
        findViewById(R.id.resistanceButton).setOnClickListener(mListener);
        findViewById(R.id.voltageButton).setOnClickListener(mListener);
        findViewById(R.id.currentButton).setOnClickListener(mListener);
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.multimeter);
        }
    }

    private void showCurrent() {
        sendMessage(Constants.I);
    }

    private void showResistance() {
        sendMessage(Constants.R);
    }

    private void showVoltage() {
        sendMessage(Constants.V);
    }

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mSelectedId == v.getId()) {
                v.setSelected(false);
                mSelectedId = -1;
                return;
            } else {
                deselectAll();
                v.setSelected(true);
                mSelectedId = v.getId();
            }
            switch (v.getId()) {
                case R.id.resistanceButton:
                    showResistance();
                    break;
                case R.id.voltageButton:
                    showVoltage();
                    break;
                case R.id.currentButton:
                    showCurrent();
                    break;
            }
        }
    };

    private void deselectAll() {
        findViewById(R.id.resistanceButton).setSelected(false);
        findViewById(R.id.voltageButton).setSelected(false);
        findViewById(R.id.currentButton).setSelected(false);
    }

    private void requestBtEnable() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    private void checkBtAdapterStatus() {
        if (!mBtAdapter.isEnabled()) {
            requestBtEnable();
        } else {
            startService(new Intent(this, BluetoothService.class));
        }
    }

    private void sendMessage(String message) {
        EventBus.getDefault().post(new ControlEvent(message));
        showToast(getString(R.string.request_sent));
    }

    private void postResponse(Message msg) {
        String data = (String) msg.obj;
        String v;
        if (data.startsWith(Constants.rV)) {
            data = data.replace(Constants.rV, "");
            v = data.trim();
        } else if (data.startsWith(Constants.rI)) {
            data = data.replace(Constants.rI, "");
            v = data.trim();
        } else if (data.startsWith(Constants.rR)) {
            data = data.replace(Constants.rR, "");
            v = data.trim();
        } else {
            v = getString(R.string.no_key);
        }
        mMeterField.setText(v);
    }

    private void showBlockView() {
        mBlockView.setVisibility(View.VISIBLE);
    }

    private void closeScreen() {
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        checkBtAdapterStatus();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        showBlockView();
        stopService(new Intent(this, BluetoothService.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case android.R.id.home:
                closeScreen();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != RESULT_OK) {
            requestBtEnable();
        } else {
            startService(new Intent(this, BluetoothService.class));
        }
    }

    @Override
    public void onBackPressed() {
        closeScreen();
    }
}
