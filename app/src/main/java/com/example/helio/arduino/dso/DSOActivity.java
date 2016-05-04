package com.example.helio.arduino.dso;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.backdoor.shared.Constants;
import com.backdoor.shared.JMessage;
import com.backdoor.shared.OriginalChatService;
import com.example.helio.arduino.R;
import com.example.helio.arduino.SettingsActivity;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DSOActivity extends AppCompatActivity implements View.OnClickListener, OnChartGestureListener,
        OnChartValueSelectedListener {

    private static final int CHART_VISIBLE = 50;
    private static final int REQUEST_ENABLE_BT = 3;

    private boolean mCapturing = false;

    private LineChart mChart;

    private BluetoothDevice mConnectedDevice = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private OriginalChatService mChatService = null;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_DEVICE_NAME:
                    getDeviceName(msg);
                    break;
                case Constants.MESSAGE_TOAST:
                    showMessage(msg);
                    break;
                case Constants.MESSAGE_READ:
                    readDso(msg);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dso);
        initBluetoothAdapter();
        initActionBar();
        initButtons();
        initChart();
    }

    private void initBluetoothAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initChart() {
        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.setOnChartGestureListener(this);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDrawGridBackground(false);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setPinchZoom(true);
        mChart.setDescription(getString(R.string.arduino_chart));

        LimitLine llXAxis = new LimitLine(10f, "Index 10");
        llXAxis.setLineWidth(4f);
        llXAxis.enableDashedLine(10f, 10f, 0f);
        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llXAxis.setTextSize(10f);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.setAxisMaxValue(220f);
        leftAxis.setAxisMinValue(-50f);
        leftAxis.setDrawZeroLine(true);

        mChart.getAxisRight().setEnabled(false);
        mChart.getLegend().setEnabled(false);
        mChart.setVisibleXRangeMaximum(CHART_VISIBLE);
        mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);
    }

    private void initButtons() {
        findViewById(R.id.captureButton).setOnClickListener(this);
        findViewById(R.id.screenshotButton).setOnClickListener(this);
        findViewById(R.id.viewScreenshot).setOnClickListener(this);
        findViewById(R.id.stopButton).setOnClickListener(this);
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setTitle(R.string.dso);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    }

    @Override
    public void onStart() {
        super.onStart();
        checkAdapterStatus();
    }

    private void requestBluetoothEnable() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    private void checkAdapterStatus() {
        if (!mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
        } else if (mChatService == null) {
            setupConnection();
        }
    }

    private void setupConnection() {
        mChatService = new OriginalChatService(this, mHandler);
    }

    private void sendMessage(String message) {
        if (mChatService.getState() != OriginalChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            String msg = new JMessage().putFlag(message).asString();
            mChatService.writeMessage(msg.getBytes());
        }
    }

    private void readDso(Message msg) {
        byte[] readBuff = (byte[]) msg.obj;
        String data = new String(readBuff, 0, msg.arg1);
        JMessage jMessage = new JMessage(data);
        if (jMessage.hasYValue()) {
            float value = Float.parseFloat(jMessage.getYValue());
            if (mCapturing) addNewEntry(value);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.captureButton:
                capture();
                break;
            case R.id.screenshotButton:
                takeScreenshot();
                break;
            case R.id.viewScreenshot:
                showScreenshots();
                break;
            case R.id.stopButton:
                stopCapturing();
                break;
        }
    }

    private void getDeviceName(Message msg) {
        String mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
        showToast(getString(R.string.connected_to) + " " + mConnectedDeviceName);
    }

    private void showMessage(Message msg) {
        String message = msg.getData().getString(Constants.TOAST);
        if (message == null) return;
        if (message.startsWith("Unable") || message.startsWith("Device")) {
            if (mChatService.getState() == OriginalChatService.STATE_NONE) {
                mChatService.start();
            }
            if (mChatService.getState() == OriginalChatService.STATE_LISTEN) {
                connectDevice(true);
            }
        } else {
            showToast(msg.getData().getString(Constants.TOAST));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopBTService();
    }

    private void stopBTService() {
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    private void resumeBluetoothService() {
        if (mChatService != null) {
            startBluetoothService();
        }
    }

    private void startBluetoothService() {
        if (mChatService.getState() == OriginalChatService.STATE_NONE) {
            mChatService.start();
            while (true) {
                if (mChatService.getState() == OriginalChatService.STATE_LISTEN) {
                    connectDevice(true);
                    break;
                }
            }
        }
    }

    private void stopCapturing() {
        sendMessage(Constants.S);
        mCapturing = false;
    }

    private void showScreenshots() {
        if (checkReadPermission()) {
            startActivity(new Intent(this, ImagesActivity.class));
        }
    }

    private void capture() {
        sendMessage(Constants.C);
        mCapturing = true;
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                return false;
            }
            return true;
        }
        return true;
    }

    private boolean checkReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
                return false;
            }
            return true;
        }
        return true;
    }

    private void takeScreenshot() {
        if (checkPermission()) {
            saveChartToImageFile();
        }
    }

    private void saveChartToImageFile() {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Constants.SCREENS_FOLDER);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return;
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.date_time_sdf), Locale.getDefault());
        String fileName = sdf.format(new Date());
        mChart.saveToPath(fileName, "/" + Constants.SCREENS_FOLDER);
        Toast.makeText(this, R.string.screenshot_saved, Toast.LENGTH_SHORT).show();
    }

    private void addNewEntry(float value) {
        LineData lineData = mChart.getLineData();
        ILineDataSet set = lineData.getDataSetByIndex(0);
        lineData.addXValue(lineData.getXValCount() + " ");
        lineData.addEntry(new Entry(value, set.getEntryCount()), 0);
        updateChart(lineData.getXValCount());
    }

    private void updateChart(int xValCount) {
        mChart.notifyDataSetChanged();
        mChart.setVisibleXRangeMaximum(CHART_VISIBLE);
        mChart.moveViewToX(xValCount - CHART_VISIBLE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void connectDevice(boolean secure) {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Activity.MODE_PRIVATE);
        String mAddress = preferences.getString(Constants.DEVICE_ADDRESS, null);
        if (mAddress != null) {
            mConnectedDevice = mBluetoothAdapter.getRemoteDevice(mAddress);
            mChatService.connect(mConnectedDevice, secure);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeBluetoothService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takeScreenshot();
                }
                break;
            case 102:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(this, ImagesActivity.class));
                }
                break;
        }
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
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        if(lastPerformedGesture != ChartTouchListener.ChartGesture.SINGLE_TAP)
            mChart.highlightValues(null);
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }
}
