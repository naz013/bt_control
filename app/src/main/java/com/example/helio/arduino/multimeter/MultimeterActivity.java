package com.example.helio.arduino.multimeter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helio.arduino.R;
import com.example.helio.arduino.SettingsActivity;
import com.example.helio.arduino.core.BluetoothService;
import com.example.helio.arduino.core.ConnectionEvent;
import com.example.helio.arduino.core.Constants;
import com.example.helio.arduino.core.ControlEvent;
import com.example.helio.arduino.core.ResponseEvent;

import java.io.IOException;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import jxl.write.WriteException;

public class MultimeterActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 3;
    private static final int VOLTAGE = R.id.voltageButton;
    private static final int CURRENT = R.id.currentButton;
    private static final int RESISTANCE = R.id.resistanceButton;
    private static final int SCT = R.id.sctButton;
    private static final String TAG = "MultimeterActivity";

    private TextView mMeterField;
    private TextView mBlockView;
    private EditText mRefreshRateField;
    private Button mResetButton;
    private ImageButton mExportButton;
    private View mSctStatus;

    private int mSelectedId;
    private boolean isReading;

    private BluetoothAdapter mBtAdapter = null;
    private static Activity activity;
    private WriteExcel mWriteExcel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_multimeter);
        initBluetoothAdapter();
        initActionBar();
        initButtons();
        mRefreshRateField = (EditText) findViewById(R.id.refreshRateField);
        mMeterField = (TextView) findViewById(R.id.meterField);
        mSctStatus = findViewById(R.id.sctStatus);
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
        mBlockView.setOnTouchListener((v, event) -> true);
    }

    private void initBluetoothAdapter() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initButtons() {
        findViewById(RESISTANCE).setOnClickListener(mListener);
        findViewById(VOLTAGE).setOnClickListener(mListener);
        findViewById(CURRENT).setOnClickListener(mListener);
        findViewById(SCT).setOnClickListener(mListener);
        findViewById(R.id.setRateButton).setOnClickListener(mListener);
        findViewById(R.id.filesButton).setOnClickListener(mListener);
        mExportButton = (ImageButton) findViewById(R.id.exportButton);
        mExportButton.setOnClickListener(mListener);
        mExportButton.setSelected(false);
        mResetButton = (Button) findViewById(R.id.resetButton);
        mResetButton.setOnClickListener(mListener);
        mResetButton.setEnabled(false);
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
        refreshExcel();
    }

    private void showResistance() {
        sendMessage(Constants.R);
        refreshExcel();
    }

    private void refreshExcel() {
        if (mWriteExcel != null) {
            closeExcelFile();
        }
        if (!mExportButton.isSelected()) return;
        if (mSelectedId == VOLTAGE) {
            initExcel(Constants.V);
        } else if (mSelectedId == CURRENT) {
            initExcel(Constants.I);
        } else if (mSelectedId == RESISTANCE) {
            initExcel(Constants.R);
        }
    }

    private void initExcel(String type) {
        mWriteExcel = new WriteExcel(this);
        mWriteExcel.setOutput(type);
    }

    private void showVoltage() {
        sendMessage(Constants.V);
        refreshExcel();
    }

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.exportButton) {
                switchExport();
            } else if (mSelectedId != v.getId() || mSelectedId == -1) {
                checkButton(v);
            } else {
                return;
            }
            switch (v.getId()) {
                case RESISTANCE:
                    showResistance();
                    break;
                case VOLTAGE:
                    showVoltage();
                    break;
                case CURRENT:
                    showCurrent();
                    break;
                case SCT:
                    showSct();
                    break;
                case R.id.resetButton:
                    reset();
                    break;
                case R.id.setRateButton:
                    sendRefreshRate();
                    break;
                case R.id.filesButton:
                    showSavedFiles();
                    break;
            }
        }
    };

    private void switchExport() {
        if (mWriteExcel != null || mExportButton.isSelected()) {
            mExportButton.setSelected(false);
            closeExcelFile();
        } else {
            mExportButton.setSelected(true);
            refreshExcel();
        }
    }

    private void checkButton(View v) {
        if (v.getId() != R.id.resetButton && v.getId() != R.id.setRateButton && v.getId() != R.id.filesButton) {
            selectButton(v);
        }
    }

    private void showSavedFiles() {
        startActivity(new Intent(this, FileListActivity.class));
    }

    private void sendRefreshRate() {
        String rateRaw = mRefreshRateField.getText().toString().trim();
        if (rateRaw.isEmpty()) {
            showToast(getString(R.string.empty_refresh_rate));
            return;
        }
        int rate = Integer.parseInt(rateRaw);
        if (rate == 0) {
            showToast(getString(R.string.refresh_cannot_be_zero));
            return;
        }
        sendMessage(Constants.W + ": " + rate);
    }

    private void showSct() {
        sendMessage(Constants.Q);
    }

    private void selectButton(View v) {
        deselectAll();
        v.setSelected(true);
        mSelectedId = v.getId();
        disableAll(mSelectedId);
        mResetButton.setEnabled(true);
        isReading = true;
    }

    private void reset() {
        closeExcelFile();
        sendMessage(Constants.D);
        deselectAll();
        enableAll();
        mMeterField.setText("");
        isReading = false;
        mSelectedId = -1;
        mSctStatus.setBackgroundResource(R.drawable.gray_circle);
    }

    private void closeExcelFile() {
        if (mWriteExcel != null) {
            try {
                mWriteExcel.write();
                mWriteExcel.close();
            } catch (IOException | WriteException e) {
                e.printStackTrace();
            }
            mWriteExcel = null;
        }
    }

    private void enableAll() {
        findViewById(RESISTANCE).setEnabled(true);
        findViewById(VOLTAGE).setEnabled(true);
        findViewById(CURRENT).setEnabled(true);
        findViewById(SCT).setEnabled(true);
        mResetButton.setEnabled(false);
    }

    private void disableAll(int id) {
        if (id != RESISTANCE) findViewById(RESISTANCE).setEnabled(false);
        if (id != VOLTAGE) findViewById(VOLTAGE).setEnabled(false);
        if (id != CURRENT) findViewById(CURRENT).setEnabled(false);
        if (id != SCT) findViewById(SCT).setEnabled(false);
    }

    private void deselectAll() {
        findViewById(RESISTANCE).setSelected(false);
        findViewById(VOLTAGE).setSelected(false);
        findViewById(CURRENT).setSelected(false);
        findViewById(SCT).setSelected(false);
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
        if (!isReading) {
            return;
        }
        String data = (String) msg.obj;
        data = data.replaceAll("\n", "");
        data = data.replaceAll(" ", "");
        String v = "";
        if (data.startsWith(Constants.rV)) {
            v = extractV(data) + " " + getString(R.string.v_low);
            saveToExcel(v);
            mSctStatus.setBackgroundResource(R.drawable.gray_circle);
        } else if (data.startsWith(Constants.rI)) {
            v = extractI(data) + " " + getString(R.string.a);
            saveToExcel(v);
            mSctStatus.setBackgroundResource(R.drawable.gray_circle);
        } else if (data.startsWith(Constants.rR)) {
            v = extractR(data);
            saveToExcel(v);
            mSctStatus.setBackgroundResource(R.drawable.gray_circle);
        } else if (data.startsWith(Constants.sCT)) {
            int value = extractSct(data);
            if (value == 1) {
                mSctStatus.setBackgroundResource(R.drawable.red_circle);
            } else {
                mSctStatus.setBackgroundResource(R.drawable.gray_circle);
            }
            v = "";
        }
        mMeterField.setText(v);
    }

    private void saveToExcel(String value) {
        if (mWriteExcel != null) {
            try {
                mWriteExcel.addValue(value);
            } catch (WriteException e) {
                e.printStackTrace();
            }
        }
    }

    private int extractSct(String data) {
        data = data.replace(Constants.sCT, "");
        int i = 0;
        try {
            i = Integer.parseInt(data.trim());
        } catch (NumberFormatException e) {
            e.getLocalizedMessage();
        }
        return i;
    }

    private String extractR(String data) {
        data = data.replace(Constants.rR, "");
        data = data.replace(" ", "").trim();
        try {
            float resistance = Float.parseFloat(data);
            return convertResistance(resistance) + "";
        } catch (NumberFormatException e) {
            return data;
        }
    }

    private String convertResistance(float resistance) throws NumberFormatException{
        Log.d(TAG, "convertResistance: " + resistance);
        String res = "" + resistance;
        if (resistance <= 590) {
            res = Math.round(resistance) + " " + getString(R.string.omega);
        } else if (resistance <= 950) {
            float tmp = resistance / 1000;
            res = String.format(Locale.getDefault(), "%.3f kΩ", tmp);
        } else if (resistance <= 9500) {
            float tmp = resistance / 1000;
            res = String.format(Locale.getDefault(), "%.2f kΩ", tmp);
        } else if (resistance <= 95000) {
            float tmp = resistance / 1000;
            res = String.format(Locale.getDefault(), "%.1f kΩ", tmp);
        } else if (resistance <= 590000) {
            float tmp = resistance / 1000;
            res = Math.round(tmp) + " kΩ";
        } else if (resistance <= 950000) {
            float tmp = resistance / 1000000;
            res = String.format(Locale.getDefault(), "%.3f MΩ", tmp);
        } else if (resistance > 950000){
            float tmp = resistance / 1000000;
            res = String.format(Locale.getDefault(), "%.2f MΩ", tmp);
        }
        return res;
    }

    private String extractI(String data) {
        data = data.replace(Constants.rI, "");
        return data.trim();
    }

    private String extractV(String data) {
        data = data.replace(Constants.rV, "");
        return data.trim();
    }

    private void closeScreen() {
        sendMessage(Constants.D);
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
        mBlockView.setVisibility(View.VISIBLE);
        stopService(new Intent(this, BluetoothService.class));
        closeExcelFile();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sendMessage(Constants.D);
        EventBus.getDefault().unregister(this);
        closeExcelFile();
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
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus) {
            reset();
        }
    }

    @Override
    public void onBackPressed() {
        closeScreen();
    }
}
