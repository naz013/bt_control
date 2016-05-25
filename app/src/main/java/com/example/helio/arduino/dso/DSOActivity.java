package com.example.helio.arduino.dso;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helio.arduino.R;
import com.example.helio.arduino.SettingsActivity;
import com.example.helio.arduino.core.Constants;
import com.example.helio.arduino.core.DeviceData;
import com.example.helio.arduino.core.OriginalChatService;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DsoActivity extends AppCompatActivity implements OnChartGestureListener, OnChartValueSelectedListener {

    private static final int REQUEST_ENABLE_BT = 3;
    private static final float CHART_MAX_Y = 220f;
    private static final float CHART_MIN_Y = -50f;
    private static final float CHART_MAX_X = 100;

    private boolean mCapturing = false;

    private ScatterChart mChart;
    private TextView mBlockView;
    private Button mStopButton;
    private Button mCaptureButton;

    private BluetoothAdapter mBtAdapter = null;
    private OriginalChatService mBtService = null;

    private static Activity activity;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    obtainConnectionMessage(msg);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    mBlockView.setVisibility(View.GONE);
                    break;
                case Constants.MESSAGE_READ:
                    readDso(msg);
                    break;
            }
        }
    };

    private void obtainConnectionMessage(Message msg) {
        switch (msg.arg1) {
            case OriginalChatService.STATE_CONNECTED:
                mBlockView.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_dso);
        initBtAdapter();
        initActionBar();
        initButtons();
        initChart();
        initBlockView();
    }

    public static Activity getActivity() {
        return activity;
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

    private void initBtAdapter() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initChart() {
        mChart = (ScatterChart) findViewById(R.id.chart1);
        mChart.setOnChartGestureListener(this);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDrawGridBackground(false);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setPinchZoom(true);
        mChart.setNoDataText(getString(R.string.no_data_available));
        mChart.setAutoScaleMinMaxEnabled(true);
        mChart.setDescription(getString(R.string.arduino_chart));

        ArrayList<String> xVals = new ArrayList<>();
        for (int i = 0; i < CHART_MAX_X + 1; i++) {
            xVals.add((i) + "");
        }
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0f, 0));
        ScatterDataSet dataSet = new ScatterDataSet(entries, getString(R.string.arduino_vhart));
        dataSet.setColor(Color.BLACK);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeHoleRadius(0f);
        dataSet.setScatterShapeSize(5f);
        dataSet.setValueTextSize(9f);
        ScatterData scatterData = new ScatterData(xVals, dataSet);
        scatterData.setValueFormatter(new MyValueFormatter());
        mChart.setData(scatterData);

        mChart.getLegend().setEnabled(false);

        YAxis yAxis = mChart.getAxisLeft();
        yAxis.removeAllLimitLines();
        yAxis.setAxisMaxValue(CHART_MAX_Y);
        yAxis.setAxisMinValue(CHART_MIN_Y);
        yAxis.setDrawZeroLine(true);

        mChart.getAxisRight().setEnabled(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMaxValue(CHART_MAX_X);
        mChart.invalidate();
    }

    private void initButtons() {
        mCaptureButton = (Button) findViewById(R.id.captureButton);
        mStopButton = (Button) findViewById(R.id.stopButton);
        mCaptureButton.setOnClickListener(mListener);
        mStopButton.setOnClickListener(mListener);
        findViewById(R.id.screenshotButton).setOnClickListener(mListener);
        findViewById(R.id.viewScreenshot).setOnClickListener(mListener);
        mStopButton.setEnabled(false);
        mCaptureButton.setEnabled(true);
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        if (toolbar != null) {
            toolbar.setTitle(R.string.dso);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        }
    }

    private void requestBtEnable() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    private void checkBtAdapterStatus() {
        if (!mBtAdapter.isEnabled()) {
            requestBtEnable();
        }
    }

    private void sendMessage(String message) {
        if (mBtService.getState() != OriginalChatService.STATE_CONNECTED) {
            setupConnector();
        }
        mBtService.writeMessage(message.getBytes());
    }

    private void readDso(Message msg) {
        String data = (String) msg.obj;
        if (data.contains(Constants.rX) && data.contains(Constants.rY)) {
            String[] parts = data.split(Constants.DIV);
            String xVal = parts[0].replace(Constants.rX, "");
            int x = Integer.parseInt(xVal.trim());
            String yVal = parts[1].replace(Constants.rY, "");
            float y = Float.parseFloat(yVal.trim());
            if (mCapturing) addNewEntry(x, y);
        }
    }

    private View.OnClickListener mListener = new View.OnClickListener() {
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
    };

    private void stopConnection() {
        if (mBtService != null) {
            mBtService.stop();
            mBtService = null;
        }
    }

    private void setupConnector() {
        stopConnection();
        try {
            String emptyName = "None";
            SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Activity.MODE_PRIVATE);
            String mAddress = preferences.getString(Constants.DEVICE_ADDRESS, null);
            if (mAddress != null) {
                BluetoothDevice mConnectedDevice = mBtAdapter.getRemoteDevice(mAddress);
                DeviceData data = new DeviceData(mConnectedDevice, emptyName);
                mBtService = new OriginalChatService(data, mHandler);
                mBtService.connect();
            }
        } catch (IllegalArgumentException e) {
            Log.d("TAG", "setupConnector failed: " + e.getMessage());
        }
    }

    private void stopCapturing() {
        sendMessage(Constants.S);
        mCapturing = false;
        mCaptureButton.setEnabled(true);
        mStopButton.setEnabled(false);
    }

    private void showScreenshots() {
        if (checkReadPermission()) {
            startActivity(new Intent(this, ImagesActivity.class));
        }
    }

    private void capture() {
        sendMessage(Constants.C);
        mCapturing = true;
        mCaptureButton.setEnabled(false);
        mStopButton.setEnabled(true);
        mChart.getScatterData().clearValues();
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

    private void addNewEntry(int x, float y) {
        ScatterData scatterData = mChart.getScatterData();
        if (scatterData != null) {
            IScatterDataSet scatterDataSet;
            try {
                scatterDataSet = scatterData.getDataSetByIndex(0);
                if (scatterDataSet == null) {
                    scatterDataSet = createSet();
                    scatterData.addDataSet(scatterDataSet);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                scatterDataSet = createSet();
                scatterData.addDataSet(scatterDataSet);
            }
            scatterData.setValueFormatter(new MyValueFormatter());
            Entry entry = new Entry(y, x);
            scatterData.addEntry(entry, 0);
            mChart.notifyDataSetChanged();
            mChart.invalidate();
        }
    }

    private ScatterDataSet createSet() {
        ScatterDataSet dataSet = new ScatterDataSet(null, getString(R.string.arduino_vhart));
        dataSet.setColor(Color.BLACK);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeHoleRadius(0f);
        dataSet.setScatterShapeSize(5f);
        dataSet.setValueTextSize(9f);
        return dataSet;
    }

    private void sendCancelMessage() {
        if (mBtService.getState() != OriginalChatService.STATE_CONNECTED) {
            setupConnector();
        }
        String msg = Constants.S;
        mBtService.writeMessage(msg.getBytes());
    }

    private void closeScreen() {
        sendCancelMessage();
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        checkBtAdapterStatus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        stopConnection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupConnector();
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
        }
    }

    class MyValueFormatter implements ValueFormatter {
        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return Math.round(value)+"";
        }
    }

    @Override
    public void onBackPressed() {
        closeScreen();
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
