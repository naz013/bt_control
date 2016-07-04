package com.example.helio.arduino.dso;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import de.greenrobot.event.EventBus;

public class DsoActivity extends AppCompatActivity implements OnChartValueSelectedListener {

    private static final int REQUEST_ENABLE_BT = 3;
    private static final float CHART_MAX_Y = 500f;
    private static final float CHART_MIN_Y = -500f;
    private static final float CHART_MAX_X = 1010;
    private static final float X_SCALE_BASE = 1000f;
    private static final String TAG = "DsoActivity";

    private boolean mCapturing = false;
    private float mXScallar = 1f;
    private int mXScaleStep = 0;
    private int mYScaleStep = 0;
    private List<Float> mYVals = new ArrayList<>();
    private List<Float> mXVals = new ArrayList<>();

    private ScatterChart mChart;
    private TextView mBlockView;
    private TextView mStopButton;
    private TextView mCaptureButton;
    private ImageButton xZoomIn, xZoomOut;
    private ImageButton yZoomIn, yZoomOut;

    private BluetoothAdapter mBtAdapter = null;

    private static Activity activity;

    public void onEvent(ResponseEvent responseEvent) {
        readDso(responseEvent.getMsg());
    }

    public void onEvent(ConnectionEvent responseEvent) {
        if (responseEvent.isConnected()) {
            mBlockView.setVisibility(View.GONE);
        } else {
            mBlockView.setVisibility(View.VISIBLE);
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
        //mBlockView.setVisibility(View.VISIBLE);
        mBlockView.setOnTouchListener((v, event) -> true);
    }

    private void initBtAdapter() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initChart() {
        mChart = (ScatterChart) findViewById(R.id.chart1);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDrawGridBackground(false);
        mChart.setTouchEnabled(true);
        mChart.setScaleEnabled(false);
        mChart.setPinchZoom(false);
        mChart.setNoDataText(getString(R.string.no_data_available));
        mChart.setAutoScaleMinMaxEnabled(true);
        mChart.setDescription(getString(R.string.arduino_chart));
        ArrayList<Entry> entries = new ArrayList<>();
        ScatterDataSet dataSet = new ScatterDataSet(entries, getString(R.string.arduino_vhart));
        dataSet.setColor(Color.BLACK);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeHoleRadius(0f);
        dataSet.setScatterShapeSize(2f);
        ScatterData scatterData = new ScatterData(dataSet);
        scatterData.setDrawValues(false);
        mChart.setData(scatterData);
        mChart.getLegend().setEnabled(false);
        mChart.invalidate();
        refreshChart();
    }

    private void refreshChart() {
        Log.d(TAG, "refreshChart: start");
        YAxis yAxis = mChart.getAxisLeft();
        yAxis.removeAllLimitLines();
        yAxis.setAxisMaxValue(CHART_MAX_Y);
        yAxis.setAxisMinValue(CHART_MIN_Y);
        yAxis.setLabelCount(11, true);
        yAxis.setDrawZeroLine(true);
        yAxis.setValueFormatter(new AxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                float scal = getYFormatScale();
                float f = value / scal;
                return String.format(Locale.getDefault(), "%.2f", f);
            }

            @Override
            public int getDecimalDigits() {
                return 0;
            }
        });
        mChart.getAxisRight().setEnabled(false);
        XAxis xAxis = mChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMaxValue(CHART_MAX_X);
        xAxis.setValueFormatter(new AxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                float scal = getXFormatScale();
                float f = value / scal;
                return String.format(Locale.getDefault(), "%.2f", f);
            }

            @Override
            public int getDecimalDigits() {
                return 0;
            }
        });
        mChart.invalidate();
        Log.d(TAG, "refreshChart: end");
    }

    private float getXFormatScale() {
        if (mXScallar > 1000000) {
            return mXScallar / 1000000;
        } else if (mXScallar > 1000) {
            return mXScallar / 1000;
        } else if (mXScallar == 1000 || mXScallar == 1) {
            return 1000;
        } else {
            return mXScallar;
        }
    }

    private float getYFormatScale() {
        if (mYScaleStep == 1) {
            return 125f;
        } else if (mYScaleStep == 2) {
            return 500f;
        } else if (mYScaleStep == 3) {
            return 2f;
        } else if (mYScaleStep == 4) {
            return 8f;
        } else {
            return 31.25f;
        }
    }

    private void initButtons() {
        mCaptureButton = (TextView) findViewById(R.id.captureButton);
        mStopButton = (TextView) findViewById(R.id.stopButton);
        mCaptureButton.setOnClickListener(mListener);
        mStopButton.setOnClickListener(mListener);
        findViewById(R.id.screenshotButton).setOnClickListener(mListener);
        findViewById(R.id.clearButton).setOnClickListener(mListener);
        findViewById(R.id.galleryButton).setOnClickListener(mListener);
        findViewById(R.id.yZoomOut).setOnClickListener(mListener);
        findViewById(R.id.yZoomIn).setOnClickListener(mListener);
        findViewById(R.id.yTrace).setOnClickListener(mListener);
        findViewById(R.id.xTrace).setOnClickListener(mListener);
        xZoomIn = (ImageButton) findViewById(R.id.xZoomIn);
        xZoomOut = (ImageButton) findViewById(R.id.xZoomOut);
        yZoomIn = (ImageButton) findViewById(R.id.yZoomIn);
        yZoomOut = (ImageButton) findViewById(R.id.yZoomOut);
        xZoomOut.setOnClickListener(mListener);
        xZoomIn.setOnClickListener(mListener);
        yZoomIn.setOnClickListener(mListener);
        yZoomOut.setOnClickListener(mListener);
        mStopButton.setEnabled(false);
        mCaptureButton.setEnabled(true);
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.dso);
        }
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

    private void readDso(Message msg) {
        String data = (String) msg.obj;
        if (data.contains(Constants.rX) && data.contains(Constants.rY)) {
            String[] parts = data.split(Constants.DIV);
            String xVal = parts[0].replace(Constants.rX, "");
            float x = Float.parseFloat(xVal.trim());
            String yVal = parts[1].replace(Constants.rY, "");
            float y = Float.parseFloat(yVal.trim());
            if (mCapturing) addNewEntry(x, y);
        }
    }

    private View.OnClickListener mListener = v -> {
        switch (v.getId()) {
            case R.id.captureButton:
                capture();
                break;
            case R.id.screenshotButton:
                takeScreenshot();
                break;
            case R.id.stopButton:
                stopCapturing();
                break;
            case R.id.clearButton:
                clearGraph();
                break;
            case R.id.galleryButton:
                showScreenshots();
                break;
            case R.id.xZoomIn:
                scaleX(1);
                break;
            case R.id.xZoomOut:
                scaleX(-1);
                break;
            case R.id.yZoomIn:
                scaleY(1);
                break;
            case R.id.yZoomOut:
                scaleY(-1);
                break;
        }
    };

    private void scaleY(int i) {
        Log.d(TAG, "scaleY: " + i);
        if (i < 0 && mYScaleStep == 0) {
            return;
        }
        if (i > 0 && mYScaleStep == 4) {
            return;
        }
        mYScaleStep += i;
        Log.d(TAG, "scaleY: y step " + mYScaleStep);
        yZoomIn.setEnabled(false);
        yZoomOut.setEnabled(false);
        reloadData();
        yZoomIn.setEnabled(true);
        yZoomOut.setEnabled(true);
    }

    private void scaleX(int i) {
        Log.d(TAG, "scaleX: " + i);
        if (i < 0 && mXScaleStep == 0) {
            return;
        }
        if (i > 0 && mXScaleStep == 6) {
            return;
        }
        if (i > 0) {
            mXScallar *= 10;
        }
        if (i < 0) {
            mXScallar /= 10;
        }
        mXScaleStep += i;
        Log.d(TAG, "scaleX: scalar " + mXScallar);
        Log.d(TAG, "scaleX: step " + mXScaleStep);
        xZoomIn.setEnabled(false);
        xZoomOut.setEnabled(false);
        reloadData();
        xZoomIn.setEnabled(true);
        xZoomOut.setEnabled(true);
    }

    private synchronized void reloadData() {
        if (mYVals.size() > 0 && mXVals.size() > 0) {
            List<Float> xList = new ArrayList<>(mXVals);
            List<Float> yList = new ArrayList<>(mYVals);
            mChart.getScatterData().clearValues();
            mChart.invalidate();
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
            }
            if (scatterData == null) return;
            float scaleX = getXScale();
            float scaleY = getYScale();
            for (int i = 0; i < xList.size(); i++) {
                float x = xList.get(i);
                float y = yList.get(i);
                int xSc = (int) (x * scaleX);
                int ySc = (int) (y * scaleY);
                if (xSc < CHART_MAX_X && ySc > CHART_MIN_Y && ySc < CHART_MAX_Y) {
                    Entry entry = new Entry(xSc, ySc);
                    scatterData.addEntry(entry, 0);
                }
            }
            mChart.notifyDataSetChanged();
            mChart.invalidate();
        }
    }

    private float getXScale() {
        return X_SCALE_BASE * mXScallar;
    }

    private float getYScale() {
        if (mYScaleStep == 1) {
            return 125f;
        } else if (mYScaleStep == 2) {
            return 500f;
        } else if (mYScaleStep == 3) {
            return 2000f;
        } else if (mYScaleStep == 4) {
            return 8000f;
        } else {
            return 31.25f;
        }
    }

    private void clearGraph() {
        mChart.getScatterData().clearValues();
        mChart.invalidate();
        mXVals.clear();
        mYVals.clear();
    }

    private void showBlockView() {
        mBlockView.setVisibility(View.VISIBLE);
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

    private void addNewEntry(float x, float y) {
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
            scatterData.setDrawValues(false);
            float scaleX = getXScale();
            float scaleY = getYScale();
            float xSc = x * scaleX;
            float ySc = y * scaleY;
            if (xSc < CHART_MAX_X) {
                Entry entry = new Entry(xSc, ySc);
                scatterData.addEntry(entry, 0);
                mYVals.add(y);
                mXVals.add(x);
                mChart.notifyDataSetChanged();
                mChart.invalidate();
            }
        }
    }

    private ScatterDataSet createSet() {
        ScatterDataSet dataSet = new ScatterDataSet(null, getString(R.string.arduino_vhart));
        dataSet.setColor(Color.BLACK);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeHoleRadius(0f);
        dataSet.setScatterShapeSize(1f);
        dataSet.setDrawValues(false);
        return dataSet;
    }

    private void sendCancelMessage() {
        String msg = Constants.S;
        EventBus.getDefault().post(new ControlEvent(msg));
        showToast(getString(R.string.request_sent));
    }

    private void closeScreen() {
        sendCancelMessage();
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
        stopCapturing();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearGraph();
        Random rand = new Random();
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
        }
        if (scatterData == null) return;
        mChart.notifyDataSetChanged();
        mChart.invalidate();
        float scaleX = getXScale();
        float scaleY = getYScale();
        for (int i = 0; i < 10000; i++) {
            float x = rand.nextFloat() * (1f - 0f) + 0f;
            float y = rand.nextFloat() * (16f - (-16f)) + (-16f);
            float xSc = x * scaleX;
            float ySc = y * scaleY;
            if (xSc < CHART_MAX_X && ySc > CHART_MIN_Y && ySc < CHART_MAX_Y) {
                Entry entry = new Entry(xSc, ySc);
                scatterData.addEntry(entry, 0);
                mYVals.add(y);
                mXVals.add(x);
            }
        }
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
        } else {
            startService(new Intent(this, BluetoothService.class));
        }
    }

    @Override
    public void onBackPressed() {
        closeScreen();
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }
}
