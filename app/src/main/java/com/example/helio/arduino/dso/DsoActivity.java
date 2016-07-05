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
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helio.arduino.R;
import com.example.helio.arduino.SettingsActivity;
import com.example.helio.arduino.core.BluetoothService;
import com.example.helio.arduino.core.ConnectionEvent;
import com.example.helio.arduino.core.Constants;
import com.example.helio.arduino.core.ControlEvent;
import com.example.helio.arduino.core.ResponseEvent;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import de.greenrobot.event.EventBus;

public class DsoActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 3;
    private static final float CHART_MAX_Y = 500f;
    private static final float CHART_MIN_Y = -500f;
    private static final float CHART_MAX_X = 1010f;
    private static final float X_SCALE_BASE = 1000f;
    private static final float CHART_POINT_SIZE = 2f;
    private static final float RANGE_DIVIDER = 2f;
    private static final String TAG = "DsoActivity";

    private boolean mCapturing = false;
    private float mXScallar = 1f;
    private float mXRangeValue = 1f;
    private int mXScaleStep = 0;
    private int mYScaleStep = 0;
    private int mXMoveStep = 0;
    private int mXParts = 0;
    private List<Float> mYVals = new ArrayList<>();
    private List<Float> mXVals = new ArrayList<>();

    private ScatterChart mChart;
    private TextView mBlockView;
    private FloatingActionButton zoomInX, zoomOutX;
    private FloatingActionButton zoomInY, zoomOutY;
    private FloatingActionButton moveRight, moveLeft;
    private FloatingActionButton moveTop, moveBottom;
    private FloatingActionButton mCaptureButton, mStopButton;

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
        mChart.setDrawGridBackground(false);
        mChart.setTouchEnabled(true);
        mChart.setScaleEnabled(false);
        mChart.setPinchZoom(false);
        mChart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        float x = motionEvent.getRawX();
                        float y = motionEvent.getRawY();
                        Entry entry = mChart.getEntryByTouchPoint(x, y);
                        Log.d(TAG, "onTouch: " + x + ", " + y);
                        if (entry != null) {
                            Log.d(TAG, "onTouch Entry: " + entry.toString());
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        break;

                    case MotionEvent.ACTION_DOWN:
                        break;
                }
                return true;
            }
        });
        mChart.setNoDataText(getString(R.string.no_data_available));
        mChart.setAutoScaleMinMaxEnabled(true);
        mChart.setDescription(getString(R.string.arduino_chart));
        ArrayList<Entry> entries = new ArrayList<>();
        ScatterDataSet dataSet = new ScatterDataSet(entries, getString(R.string.arduino_vhart));
        dataSet.setColor(Color.BLACK);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeHoleRadius(0f);
        dataSet.setScatterShapeSize(CHART_POINT_SIZE);
        ScatterData scatterData = new ScatterData(dataSet);
        scatterData.setDrawValues(false);
        mChart.setData(scatterData);
        mChart.getLegend().setEnabled(false);
        mChart.invalidate();
        refreshChart();
    }

    private void refreshChart() {
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
                float f = value / scal + getXPartSize();
                return String.format(Locale.getDefault(), "%.2f", f);
            }

            @Override
            public int getDecimalDigits() {
                return 0;
            }
        });
        mChart.invalidate();
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
        findViewById(R.id.screenshot_item).setOnClickListener(mListener);
        findViewById(R.id.clearButton).setOnClickListener(mListener);
        findViewById(R.id.stopButton).setOnClickListener(mListener);
        findViewById(R.id.captureButton).setOnClickListener(mListener);
        findViewById(R.id.gallery_item).setOnClickListener(mListener);
        findViewById(R.id.traceY).setOnClickListener(mListener);
        findViewById(R.id.traceX).setOnClickListener(mListener);
        moveBottom = (FloatingActionButton) findViewById(R.id.moveBottom);
        moveTop = (FloatingActionButton) findViewById(R.id.moveTop);
        moveLeft = (FloatingActionButton) findViewById(R.id.moveLeft);
        moveRight = (FloatingActionButton) findViewById(R.id.moveRight);
        zoomInX = (FloatingActionButton) findViewById(R.id.zoomInX);
        zoomOutX = (FloatingActionButton) findViewById(R.id.zoomOutX);
        zoomInY = (FloatingActionButton) findViewById(R.id.zoomInY);
        zoomOutY = (FloatingActionButton) findViewById(R.id.zoomOutY);
        mCaptureButton = (FloatingActionButton) findViewById(R.id.captureButton);
        mStopButton = (FloatingActionButton) findViewById(R.id.stopButton);
        zoomInX.setOnClickListener(mListener);
        zoomOutX.setOnClickListener(mListener);
        zoomInY.setOnClickListener(mListener);
        zoomOutY.setOnClickListener(mListener);
        moveBottom.setOnClickListener(mListener);
        moveRight.setOnClickListener(mListener);
        moveLeft.setOnClickListener(mListener);
        moveTop.setOnClickListener(mListener);
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
            case R.id.screenshot_item:
                takeScreenshot();
                break;
            case R.id.stopButton:
                stopCapturing();
                break;
            case R.id.clearButton:
                clearGraph();
                break;
            case R.id.gallery_item:
                showScreenshots();
                break;
            case R.id.zoomInX:
                scaleX(1);
                break;
            case R.id.zoomOutX:
                scaleX(-1);
                break;
            case R.id.zoomInY:
                scaleY(1);
                break;
            case R.id.zoomOutY:
                scaleY(-1);
                break;
            case R.id.moveRight:
                moveX(1);
                break;
            case R.id.moveLeft:
                moveX(-1);
                break;
        }
    };

    private void moveX(int i) {
        if (mXScaleStep == 0) {
            return;
        }
        if (i < 0 && mXMoveStep == 0) {
            return;
        }
        mXParts = (int) ((int) X_SCALE_BASE / getXPartSize());
        Log.d(TAG, "moveX: parts " + mXParts);
        if (i > 0 && mXMoveStep == (mXParts - 2)) {
            return;
        }
        mXMoveStep += i;
        moveLeft.setEnabled(false);
        moveRight.setEnabled(false);
        reloadData();
        moveLeft.setEnabled(true);
        moveRight.setEnabled(true);
    }

    private float getXPartSize() {
        return X_SCALE_BASE / mXScallar / RANGE_DIVIDER;
    }

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
        zoomInY.setEnabled(false);
        zoomOutY.setEnabled(false);
        reloadData();
        zoomInY.setEnabled(true);
        zoomOutY.setEnabled(true);
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
        mXMoveStep = 0;
        mXScaleStep += i;
        Log.d(TAG, "scaleX: scalar " + mXScallar);
        Log.d(TAG, "scaleX: step " + mXScaleStep);
        zoomInX.setEnabled(false);
        zoomOutX.setEnabled(false);
        reloadData();
        zoomInX.setEnabled(true);
        zoomOutX.setEnabled(true);
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
            float slideX = getSlideX();
            float maxX = CHART_MAX_X / scaleX + (slideX / scaleX);
            float minX = mXMoveStep > 0 ? (maxX - ((slideX / scaleX) * 2)) : 0f;
            float minY = CHART_MIN_Y / scaleY;
            float maxY = CHART_MAX_Y / scaleY;
            IScatterDataSet dataSet = scatterData.getDataSetByIndex(0);
            dataSet.clear();
            for (int i = 0; i < xList.size(); i++) {
                float x = xList.get(i);
                float y = yList.get(i);
                if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
                    int xSc = (int) (x * scaleX - slideX);
                    int ySc = (int) (y * scaleY);
                    dataSet.addEntry(new Entry(xSc, ySc));
                }
            }
            if (dataSet.getEntryCount() == 0) {
                dataSet.addEntry(new Entry(-1f, 0f));
            }
            mChart.notifyDataSetChanged();
        }
    }

    private float getXScale() {
        return (X_SCALE_BASE * mXScallar);
    }

    private float getSlideX() {
        return X_SCALE_BASE / 2 * mXMoveStep;
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
        dataSet.setScatterShapeSize(CHART_POINT_SIZE);
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
        float minX = 0f;
        float maxX = CHART_MAX_X / scaleX;
        float minY = CHART_MIN_Y / scaleY;
        float maxY = CHART_MAX_Y / scaleY;
        IScatterDataSet dataSet = scatterData.getDataSetByIndex(0);
        dataSet.clear();
        for (int i = 0; i < 10000; i++) {
            float x = rand.nextFloat() * (1f - 0f) + 0f;
            float y = rand.nextFloat() * (16f - (-16f)) + (-16f);
            if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
                float xSc = x * scaleX;
                float ySc = y * scaleY;
                dataSet.addEntry(new Entry(xSc, ySc));
                mYVals.add(y);
                mXVals.add(x);
            }
        }
        scatterData.addDataSet(dataSet);
        mChart.notifyDataSetChanged();
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
}
