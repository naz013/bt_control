package com.example.helio.arduino.dso;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
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
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.utils.MPPointD;

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
    private static final float CHART_MAX_Y = 1000f;
    private static final float CHART_MAX_X = 1000f;
    private static final float X_SCALE_BASE = 1000f;
    private static final float Y_SCALE_BASE = 31.25f;
    private static final float CHART_POINT_SIZE = 2f;
    private static final float RANGE_DIVIDER = 2f;
    private static final String TAG = "DsoActivity";

    private boolean xProcessing;
    private boolean yProcessing;
    private boolean mIsYTracing = false;
    private boolean mIsXTracing = false;
    private float mXScallar = 1f;
    private int mXScaleStep = 0;
    private int mYScaleStep = 0;
    private int mXMoveStep = 0;
    private int mYMoveStep = 4;
    private List<Float> mYVals = new ArrayList<>();
    private List<Float> mXVals = new ArrayList<>();

    private Point mScreenSize;

    private ScatterChart mChart;
    private TextView mBlockView;
    private FloatingActionButton zoomInX, zoomOutX;
    private FloatingActionButton zoomInY, zoomOutY;
    private FloatingActionButton moveRight, moveLeft;
    private FloatingActionButton moveTop, moveBottom;
    private FloatingActionButton mCaptureButton, mStopButton;

    private BluetoothAdapter mBtAdapter = null;

    private static Activity activity;
    private boolean mXReceived;

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
        setScreenSize();
        initBtAdapter();
        initActionBar();
        initButtons();
        initChart();
        initBlockView();
    }

    private void setScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        mScreenSize = new Point();
        display.getSize(mScreenSize);
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

    private float getXPositionByTouch(float x) {
        int width = mScreenSize.x;
        return CHART_MAX_X * (x / (float) width);
    }

    private float getYPositionByTouch(float y) {
        MPPointD pointHigh = mChart.getPixelsForValues(0, 0, YAxis.AxisDependency.LEFT);
        MPPointD pointLow = mChart.getPixelsForValues(0, 1000, YAxis.AxisDependency.LEFT);
        int height = mScreenSize.y;
        y = height - y - (height - ((float) pointHigh.y - (float) pointLow.y));
        float tmp = (float) ((y - pointLow.y) / (pointHigh.y - pointLow.y));
        return CHART_MAX_Y * tmp + (mChart.getHeight());
    }

    private void initChart() {
        mChart = (ScatterChart) findViewById(R.id.chart1);
        mChart.setDrawGridBackground(false);
        mChart.setTouchEnabled(true);
        mChart.setScaleEnabled(false);
        mChart.setPinchZoom(false);
        mChart.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    float x = motionEvent.getRawX();
                    float y = motionEvent.getRawY();
                    if (mIsXTracing) {
                        drawVerticalLine(getXPositionByTouch(x));
                    } else if (mIsYTracing) {
                        drawHorizontalLine(getYPositionByTouch(y));
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    break;

                case MotionEvent.ACTION_DOWN:
                    break;
            }
            return true;
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
        yAxis.setLabelCount(11, true);
        yAxis.setDrawZeroLine(false);
        yAxis.setValueFormatter(new AxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return getYLabelFormatted(value);
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
                return getXLabelFormatted(value);
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
        String[] arrays = data.split(";");
        if (arrays[0].startsWith("����x:")) {
            String xArray = arrays[0].replace("����x:", "");
            String[] parts = xArray.split(Constants.COMMA);
            mXVals.clear();
            for (String xVal : parts) {
                if (TextUtils.isEmpty(xVal.trim())) continue;
                float x = Float.parseFloat(xVal.trim());
                mXVals.add(x);
            }
            mXReceived = true;
        } else if (arrays[0].startsWith(Constants.rX)) {
            String xArray = arrays[0].replace(Constants.rX, "");
            String[] parts = xArray.split(Constants.COMMA);
            mXVals.clear();
            for (String xVal : parts) {
                if (TextUtils.isEmpty(xVal.trim())) continue;
                float x = Float.parseFloat(xVal.trim());
                mXVals.add(x);
            }
            mXReceived = true;
        }
        if (arrays[1].startsWith(Constants.rY)) {
            String yArray = arrays[1].replace(Constants.rY, "");
            String[] parts = yArray.split(Constants.COMMA);
            mYVals.clear();
            for (String yVal : parts) {
                if (TextUtils.isEmpty(yVal.trim())) continue;
                float y = Float.parseFloat(yVal.trim());
                mYVals.add(y);
            }
            if (mXReceived) {
                mXReceived = false;
                reloadData(mYVals, mXVals);
            }
        }
//        Log.d(TAG, "readDso: " + data);
//        if (data.matches(Constants.OPEN_X)) {
//            mXVals.clear();
//            xProcessing = true;
//        } else if (data.matches(Constants.CLOSE_X)) {
//            xProcessing = false;
//        } else if (data.matches(Constants.OPEN_Y)) {
//            mYVals.clear();
//            yProcessing = true;
//        } else if (data.matches(Constants.CLOSE_Y)) {
//            yProcessing = false;
//        }
//        if (xProcessing) {
//            if (TextUtils.isEmpty(data.trim())) return;
//            float x = Float.parseFloat(data.trim());
//            mXVals.add(x);
//        } else if (yProcessing) {
//            if (TextUtils.isEmpty(data.trim())) return;
//            float x = Float.parseFloat(data.trim());
//            mXVals.add(x);
//        }
//        if (!xProcessing && !yProcessing) {
//            reloadData(mYVals, mXVals);
//        }
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
            case R.id.moveTop:
                moveY(1);
                break;
            case R.id.moveBottom:
                moveY(-1);
                break;
            case R.id.traceX:
                traceX();
                break;
            case R.id.traceY:
                traceY();
                break;
        }
    };

    private void traceY() {
        mIsYTracing = !mIsYTracing;
        mIsXTracing = false;
        mChart.getXAxis().removeAllLimitLines();
        if (mIsYTracing) {
            drawHorizontalLine(500f);
        } else {
            refreshChart();
        }
    }

    private void drawHorizontalLine(float position) {
        mChart.getAxisLeft().removeAllLimitLines();
        LimitLine yLimit = new LimitLine(position);
        yLimit.setLineColor(getResources().getColor(R.color.colorRed));
        yLimit.setLabel(getYLabelFormatted(position));
        yLimit.setTextSize(20f);
        if (position > CHART_MAX_Y / 2) yLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        else yLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        yLimit.setTextColor(getResources().getColor(R.color.colorBlue));
        mChart.getAxisLeft().addLimitLine(yLimit);
        mChart.invalidate();
    }

    private String getYLabelFormatted(float value) {
        float scal = getYFormatScale();
        float deviation = getYDeviation();
        float deviationCorrector = getDeviationCorrector();
        float f = ((value - CHART_MAX_Y / 2) / scal);
        if (mYScaleStep > 0 && mYMoveStep != getYParts() / 2) {
            f = f - (deviation * (deviationCorrector - 1));
        }
        return String.format(Locale.getDefault(), "%.2f", f);
    }

    private void drawVerticalLine(float position) {
        mChart.getXAxis().removeAllLimitLines();
        LimitLine xLimit = new LimitLine(position);
        xLimit.setLineColor(getResources().getColor(R.color.colorRed));
        xLimit.setLabel(getXLabelFormatted(position));
        xLimit.setTextSize(20f);
        if (position > CHART_MAX_X / 2) xLimit.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
        else xLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        xLimit.setTextColor(getResources().getColor(R.color.colorGreen));
        mChart.getXAxis().addLimitLine(xLimit);
        mChart.invalidate();
    }

    private String getXLabelFormatted(float value) {
        float scal = getXFormatScale();
        float f;
        if (mXScaleStep > 0 && mXScaleStep < 4) {
            float scaleX = getXScale();
            float slideX = getSlideX();
            float maxX = CHART_MAX_X / scaleX + (slideX / scaleX);
            float minX = mXMoveStep > 0 ? (maxX - (CHART_MAX_X / scaleX)) : 0f;
            f = value / (scal / (scal * maxX));
            if (mXMoveStep > 0 && f == 0.0) f = minX * CHART_MAX_X;
        } else {
            f = value / scal;
        }
        return String.format(Locale.getDefault(), "%.2f", f);
    }

    private void traceX() {
        mIsXTracing = !mIsXTracing;
        mIsYTracing = false;
        mChart.getAxisLeft().removeAllLimitLines();
        if (mIsXTracing) {
            drawVerticalLine(500f);
        } else {
            mChart.getXAxis().removeAllLimitLines();
        }
    }

    private void moveY(int i) {
        if (mYScaleStep == 0) return;
        if (i > 0 && mYMoveStep == 1) return;
        if (i < 0 && mYMoveStep == getYParts() - 1) return;
        mYMoveStep -= i;
        moveTop.setEnabled(false);
        moveBottom.setEnabled(false);
        reloadData(mYVals, mXVals);
        moveTop.setEnabled(true);
        moveBottom.setEnabled(true);
    }

    private void moveX(int i) {
        if (mXScaleStep == 0) return;
        if (i < 0 && mXMoveStep == 0) return;
        int mXParts = (int) ((int) X_SCALE_BASE / getXPartSize());
        if (i > 0 && mXMoveStep == (mXParts - 2)) return;
        mXMoveStep += i;
        moveLeft.setEnabled(false);
        moveRight.setEnabled(false);
        reloadData(mYVals, mXVals);
        moveLeft.setEnabled(true);
        moveRight.setEnabled(true);
    }

    private float getXPartSize() {
        return X_SCALE_BASE / mXScallar / RANGE_DIVIDER;
    }

    private void scaleY(int i) {
        if (i < 0 && mYScaleStep == 0) return;
        if (i > 0 && mYScaleStep == 4) return;
        mYScaleStep += i;
        int mYParts = getYParts();
        mYMoveStep = mYParts / 2;
        zoomInY.setEnabled(false);
        zoomOutY.setEnabled(false);
        reloadData(mYVals, mXVals);
        zoomInY.setEnabled(true);
        zoomOutY.setEnabled(true);
    }

    private void reloadTraceLines() {
        if (mIsXTracing) {
            List<LimitLine> lines = mChart.getXAxis().getLimitLines();
            if (lines.size() == 0) return;
            LimitLine line = lines.get(0);
            drawVerticalLine(line.getLimit());
        }
        if (mIsYTracing) {
            List<LimitLine> lines = mChart.getAxisLeft().getLimitLines();
            if (lines.size() == 0) return;
            LimitLine line = lines.get(0);
            drawHorizontalLine(line.getLimit());
        }
    }

    private int getYParts() {
        return ((int) Math.round(Math.pow(4, mYScaleStep))) * 2;
    }

    private void scaleX(int i) {
        if (i < 0 && mXScaleStep == 0) return;
        if (i > 0 && mXScaleStep == 6) return;
        if (i > 0) mXScallar *= 10;
        if (i < 0) mXScallar /= 10;
        mXMoveStep = 0;
        mXScaleStep += i;
        int mYParts = getYParts();
        mYMoveStep = mYParts / 2;
        zoomInX.setEnabled(false);
        zoomOutX.setEnabled(false);
        reloadData(mYVals, mXVals);
        zoomInX.setEnabled(true);
        zoomOutX.setEnabled(true);
    }

    private synchronized void reloadData(List<Float> mYVals, List<Float> mXVals) {
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
            float deviationY = getYDeviation();
            float maxX = CHART_MAX_X / scaleX + (slideX / scaleX);
            float minX = mXMoveStep > 0 ? (maxX - (CHART_MAX_X / scaleX)) : 0f;
            float baseY = (CHART_MAX_Y / 2) / scaleY;
            float yMoveMiddle = getYParts() / 2;
            float deviationCorrector = getDeviationCorrector();
            float maxY = baseY + ((yMoveMiddle - mYMoveStep) * baseY);
            float minY = mYMoveStep != yMoveMiddle ? (maxY - baseY * 2) : -deviationY;
            if (mYScaleStep == 0) {
                maxY = 16.0f;
                minY = -16.0f;
            }
            IScatterDataSet dataSet = scatterData.getDataSetByIndex(0);
            dataSet.clear();
            Log.d(TAG, "reloadData: minX " + minX + ", maxX " + maxX);
            Log.d(TAG, "reloadData: minY " + minY + ", maxY " + maxY);
            for (int i = 0; i < xList.size(); i++) {
                float x = xList.get(i);
                float y = yList.get(i);
                if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
                    int xSc = (int) (x * scaleX - slideX);
                    int ySc = (int) ((y + (deviationY * deviationCorrector)) * scaleY);
                    dataSet.addEntry(new Entry(xSc, ySc));
                }
            }
            dataSet.addEntry(new Entry(-0f, 0f));
            mChart.notifyDataSetChanged();
            reloadTraceLines();
        }
    }

    private float getDeviationCorrector() {
        return mYMoveStep - (getYParts() / 2 - 1);
    }

    private float getYDeviation() {
        if (mYScaleStep == 0) return 16f;
        else return 16f / (float) Math.pow(4, mYScaleStep);
    }

    private float getXScale() {
        return (X_SCALE_BASE * mXScallar);
    }

    private float getSlideX() {
        return X_SCALE_BASE / 2 * mXMoveStep;
    }

    private float getYScale() {
        float incr = 1f;
        if (mYScaleStep > 0) incr = (float) Math.pow(4, mYScaleStep);
        return Y_SCALE_BASE * incr;
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
        float deviationY = getYDeviation();
        IScatterDataSet dataSet = scatterData.getDataSetByIndex(0);
        dataSet.clear();
        for (int i = 0; i < 10000; i++) {
            float x = rand.nextFloat() * (1f - 0f) + 0f;
            float y = rand.nextFloat() * (16f - (-16f)) + (-16f);
            float xSc = x * scaleX;
            float ySc = (y + deviationY) * scaleY;
            dataSet.addEntry(new Entry(xSc, ySc));
            mYVals.add(y);
            mXVals.add(x);
        }
        dataSet.addEntry(new Entry(-0f, 0f));
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
