package com.example.helio.arduino.dso;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.Constants;
import com.example.helio.arduino.signal.FragmentListener;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.utils.MPPointD;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SnapshotFragment extends Fragment {

    private FragmentListener mFragmentListener;

    private static final String TAG = "SnapshotFragment";

    private boolean mIsYTracing = false;
    private boolean mIsXTracing = false;
    private float mXScallar = 1f;
    private int mXScaleStep = 0;
    private int mYScaleStep = 0;
    private int mXMoveStep = 0;
    private int mYMoveStep = getYParts() / 2;
    private List<Float> mYVals = new ArrayList<>();
    private List<Float> mXVals = new ArrayList<>();

    private long startTime = 0;

    private ScatterChart mChart;
    private ImageButton zoomInX, zoomOutX;
    private ImageButton zoomInY, zoomOutY;
    private ImageButton moveRight, moveLeft;
    private ImageButton moveTop, moveBottom;
    private ProgressDialog mProgressDialog;

    public SnapshotFragment() {
    }

    public static SnapshotFragment newInstance() {
        return new SnapshotFragment();
    }

    public void setData(List<Float> xVals, List<Float> yVals) {
        if (xVals.size() == 0 || yVals.size() == 0) return;
        this.mXVals = new ArrayList<>(xVals);
        this.mYVals = new ArrayList<>(yVals);
        try {
            DsoWriter.writeToFile(mYVals);
        } catch (IOException e) {
            e.printStackTrace();
        }
        hideProgressDialog();
        reloadData(mYVals, mXVals);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_snapshot, container, false);
        initButtons(view);
        initChart(view);
        setUpClearGraph();
        return view;
    }

    private void hideProgressDialog() {
        Log.d(TAG, "hideProgressDialog: wait time " + (System.currentTimeMillis() - startTime));
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        startTime = System.currentTimeMillis();
        mProgressDialog = ProgressDialog.show(getActivity(), getString(R.string.receiving_data),
                getString(R.string.please_wait), false, false);
    }

    private void setUpClearGraph() {
        clearGraph();
        ScatterData scatterData = mChart.getScatterData();
        if (scatterData != null) {
            initSet(scatterData, 0);
        }
        if (scatterData == null) return;
        LineData lineData = mChart.getLineData();
        if (lineData != null) {
            initSet(lineData, 0);
        }
        if (lineData == null) return;
        IScatterDataSet dataSet = scatterData.getDataSetByIndex(0);
        dataSet.clear();
        ILineDataSet lineDataSet = lineData.getDataSetByIndex(0);
        lineDataSet.clear();
        Entry entry = new Entry(0f, 0f);
        dataSet.addEntry(entry);
        lineDataSet.addEntry(entry);
        mChart.getLineData().notifyDataChanged();
        mChart.getScatterData().notifyDataChanged();
        mChart.notifyDataSetChanged();
        mChart.invalidate();
    }

    private float getXPositionByTouch(float x, float y) {
        MPPointD pointD = mChart.getValuesByTouchPoint(x, y, YAxis.AxisDependency.LEFT);
        return (float) pointD.x;
    }

    private float getYPositionByTouch(float x, float y) {
        MPPointD pointD = mChart.getValuesByTouchPoint(x, y, YAxis.AxisDependency.LEFT);
        return (float) pointD.y + DsoActivity.CHART_MAX_Y / 2;
    }

    private void initChart(View v) {
        mChart = (ScatterChart) v.findViewById(R.id.chart1);
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
                        drawVerticalLine(getXPositionByTouch(x, y));
                    } else if (mIsYTracing) {
                        drawHorizontalLine(getYPositionByTouch(x, y));
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
        mChart.setAutoScaleMinMaxEnabled(false);
        mChart.setDescription(getString(R.string.arduino_chart));
        ArrayList<Entry> entries = new ArrayList<>();
        ScatterDataSet dataSet = new ScatterDataSet(entries, getString(R.string.arduino_vhart));
        dataSet.setColor(Color.BLACK);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeHoleRadius(0f);
        dataSet.setScatterShapeSize(DsoActivity.CHART_POINT_SIZE);
        ScatterData scatterData = new ScatterData(dataSet);
        scatterData.setDrawValues(false);
        LineDataSet lineDataSet = new LineDataSet(entries, getString(R.string.arduino_vhart));
        lineDataSet.setColor(Color.BLACK);
        lineDataSet.setCircleColor(Color.BLACK);
        lineDataSet.setCircleRadius(0.1f);
        lineDataSet.setLineWidth(0.1f);
        lineDataSet.setMode(LineDataSet.Mode.LINEAR);
        LineData lineData = new LineData(lineDataSet);
        lineData.setDrawValues(false);
        mChart.setData(scatterData);
        mChart.setLineData(lineData);
        mChart.getLegend().setEnabled(false);
        mChart.invalidate();
        refreshChart();
    }

    private void refreshChart() {
        YAxis yAxis = mChart.getAxisLeft();
        yAxis.removeAllLimitLines();
        yAxis.setAxisMaxValue(DsoActivity.CHART_MAX_Y);
        yAxis.setDrawZeroLine(false);
        yAxis.setDrawLimitLinesBehindData(false);
        yAxis.setValueFormatter(new AxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return getYLabelFormatted(value, false);
            }

            @Override
            public int getDecimalDigits() {
                return 0;
            }
        });
        mChart.getAxisRight().setEnabled(false);
        XAxis xAxis = mChart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLimitLinesBehindData(false);
        xAxis.setAxisMaxValue(DsoActivity.CHART_MAX_X);
        xAxis.setValueFormatter(new AxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return getXLabelFormatted(value, false);
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
            return mXScallar / 10000;
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

    private void initButtons(View view) {
        view.findViewById(R.id.screenshot_item).setOnClickListener(mListener);
        view.findViewById(R.id.clearButton).setOnClickListener(mListener);
        view.findViewById(R.id.stopButton).setOnClickListener(mListener);
        view.findViewById(R.id.captureButton).setOnClickListener(mListener);
        view.findViewById(R.id.gallery_item).setOnClickListener(mListener);
        view.findViewById(R.id.traceY).setOnClickListener(mListener);
        view.findViewById(R.id.traceX).setOnClickListener(mListener);
        moveBottom = (ImageButton) view.findViewById(R.id.moveBottom);
        moveTop = (ImageButton) view.findViewById(R.id.moveTop);
        moveLeft = (ImageButton) view.findViewById(R.id.moveLeft);
        moveRight = (ImageButton) view.findViewById(R.id.moveRight);
        zoomInX = (ImageButton) view.findViewById(R.id.zoomInX);
        zoomOutX = (ImageButton) view.findViewById(R.id.zoomOutX);
        zoomInY = (ImageButton) view.findViewById(R.id.zoomInY);
        zoomOutY = (ImageButton) view.findViewById(R.id.zoomOutY);
        ImageButton readButton = (ImageButton) view.findViewById(R.id.readButton);
        if (DsoWriter.hasDsoData()) readButton.setVisibility(View.VISIBLE);
        readButton.setOnClickListener(view1 -> loadFromFile());
        zoomInX.setOnClickListener(mListener);
        zoomOutX.setOnClickListener(mListener);
        zoomInY.setOnClickListener(mListener);
        zoomOutY.setOnClickListener(mListener);
        moveBottom.setOnClickListener(mListener);
        moveRight.setOnClickListener(mListener);
        moveLeft.setOnClickListener(mListener);
        moveTop.setOnClickListener(mListener);
    }

    private void loadFromFile() {
        mXVals.clear();
        mYVals = DsoWriter.readDsoAsArray();
        for (int i = 0; i < mYVals.size(); i++) {
            float x = ((float) i / ((float) mYVals.size() / 1500f)) * (1f / 1000f);
            mXVals.add(x);
        }
        reloadData(mYVals, mXVals);
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
                setUpClearGraph();
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
            drawHorizontalLine(DsoActivity.CHART_MAX_Y / 2);
        } else {
            mChart.getAxisLeft().removeAllLimitLines();
            mChart.invalidate();
        }
    }

    private void drawHorizontalLine(float position) {
        mChart.getAxisLeft().removeAllLimitLines();
        LimitLine yLimit = new LimitLine(position);
        yLimit.setLineColor(getResources().getColor(R.color.colorRed));
        yLimit.setLabel(getYLabelFormatted(position, true));
        yLimit.setTextSize(20f);
        if (position > DsoActivity.CHART_MAX_Y / 2) yLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        else yLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        yLimit.setTextColor(getResources().getColor(R.color.colorBlue));
        mChart.getAxisLeft().setDrawLimitLinesBehindData(false);
        mChart.getAxisLeft().addLimitLine(yLimit);
        mChart.invalidate();
    }

    private String getYLabelFormatted(float value, boolean trace) {
        float scalar = getYFormatScale();
        float deviation = getYDeviation();
        float deviationCorrector = getDeviationCorrector();
        float f = ((value - DsoActivity.CHART_MAX_Y / 2) / scalar);
        if (mYScaleStep > 2  && mYMoveStep != getYParts() / 2) {
            f = f - ((deviation * (deviationCorrector - 1)) * DsoActivity.CHART_MAX_Y);
        } else if (mYScaleStep > 0 && mYMoveStep != getYParts() / 2) {
            f = f - (deviation * (deviationCorrector - 1));
        }
        if (value == DsoActivity.CHART_MAX_Y && !trace) {
            return String.format(Locale.getDefault(), getYUnitLabel(), f);
        } else {
            return String.format(Locale.getDefault(), "%.2f", f);
        }
    }

    private String getYUnitLabel() {
        if (mYScaleStep > 2) {
            return "(mv)\n%.2f";
        } else {
            return "(v)\n%.2f";
        }
    }

    private void drawVerticalLine(float position) {
        mChart.getXAxis().removeAllLimitLines();
        LimitLine xLimit = new LimitLine(position);
        xLimit.setLineColor(getResources().getColor(R.color.colorRed));
        xLimit.setLabel(getXLabelFormatted(position, true));
        xLimit.setTextSize(20f);
        if (position > DsoActivity.CHART_MAX_X / 2) xLimit.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
        else xLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        xLimit.setTextColor(getResources().getColor(R.color.colorGreen));
        mChart.getXAxis().addLimitLine(xLimit);
        mChart.getXAxis().setDrawLimitLinesBehindData(false);
        mChart.invalidate();
    }

    private String getXLabelFormatted(float value, boolean trace) {
        float scalar = getXFormatScale();
        float f = (value + getSlideX()) / (scalar * 10);
        if (mXScaleStep == 0) {
            f = (value + getSlideX()) / (scalar * 10);
        }
        if (value > 14500 && !trace) {
            return String.format(Locale.getDefault(), getXUnitLabel(), f);
        } else {
            return String.format(Locale.getDefault(), getXLabel(), f);
        }
    }

    private String getXLabel() {
        if (mXScaleStep > 0) {
            return "%.0f";
        } else {
            return "%.2f";
        }
    }

    private String getXUnitLabel() {
        if (mXScaleStep > 0) {
            return "%.0f(uS)";
        } else {
            return "%.2f(mS)";
        }
    }

    private void traceX() {
        mIsXTracing = !mIsXTracing;
        mIsYTracing = false;
        mChart.getAxisLeft().removeAllLimitLines();
        if (mIsXTracing) {
            drawVerticalLine(DsoActivity.CHART_MAX_X / 2);
        } else {
            mChart.getXAxis().removeAllLimitLines();
            mChart.invalidate();
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
        int mXParts = (int) ((int) DsoActivity.X_SCALE_BASE / getXPartSize());
        if (i > 0 && mXMoveStep == (mXParts - 2)) return;
        mXMoveStep += i;
        moveLeft.setEnabled(false);
        moveRight.setEnabled(false);
        reloadData(mYVals, mXVals);
        moveLeft.setEnabled(true);
        moveRight.setEnabled(true);
    }

    private float getXPartSize() {
        return DsoActivity.X_SCALE_BASE / mXScallar / DsoActivity.RANGE_DIVIDER;
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
        if (i > 0 && mXScaleStep == 5) return;
        if (i > 0) mXScallar *= 2;
        if (i < 0) mXScallar /= 2;
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

    private synchronized void reloadData(List<Float> mYValues, List<Float> mXValues) {
        long start = System.currentTimeMillis();
        if (mYValues.size() == 0 || mXValues.size() == 0) return;
        Log.d(TAG, "reloadData: x size " + mXValues.size());
        Log.d(TAG, "reloadData: y size " + mYValues.size());
        List<Float> xList = new ArrayList<>(mXValues);
        List<Float> yList = new ArrayList<>(mYValues);
        mChart.getScatterData().clearValues();
        mChart.getLineData().clearValues();
        ScatterData scatterData = mChart.getScatterData();
        if (scatterData != null) {
            initSet(scatterData, 0);
        }
        if (scatterData == null) return;
        LineData lineData = mChart.getLineData();
        if (lineData != null) {
            initSet(lineData, 0);
        }
        if (lineData == null) return;
        float scaleX = getXScale();
        float scaleY = getYScale();
        float slideX = getSlideX();
        float deviationY = getYDeviation();
        float maxX = DsoActivity.CHART_MAX_X / scaleX + (slideX / scaleX);
        float minX = mXMoveStep > 0 ? (maxX - (DsoActivity.CHART_MAX_X / scaleX)) : 0f;
        float baseY = (DsoActivity.CHART_MAX_Y / 2) / scaleY;
        float yMoveMiddle = getYParts() / 2;
        float deviationCorrector = getDeviationCorrector();
        float maxY = baseY + ((yMoveMiddle - mYMoveStep) * baseY);
        float minY = mYMoveStep != yMoveMiddle ? (maxY - baseY * 2) : -deviationY;
        if (mYScaleStep == 0) {
            maxY = DsoActivity.Y_MAX;
            minY = DsoActivity.Y_MIN;
        }
        IScatterDataSet dataSet = scatterData.getDataSetByIndex(0);
        dataSet.clear();
        ILineDataSet lineDataSet = lineData.getDataSetByIndex(0);
        Log.d(TAG, "reloadData: minX " + minX + ", maxX " + maxX);
        Log.d(TAG, "reloadData: minY " + minY + ", maxY " + maxY);
        boolean hasPrev = false;
        int index = 0;
        for (int i = 0; i < xList.size(); i++) {
            float x = xList.get(i);
            float y = yList.get(i);
            if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
                int xSc = (int) (x * scaleX - slideX);
                int ySc = (int) ((y + (deviationY * deviationCorrector)) * scaleY);
                if (!hasPrev && i > 0) {
                    initSet(lineData, index);
                    lineDataSet = lineData.getDataSetByIndex(index);
                    if (mYScaleStep > 1) {
                        float prevY = yList.get(i - 1);
                        if (prevY > y) {
                            lineDataSet.addEntry(new Entry(xSc, 1500f));
                        } else {
                            lineDataSet.addEntry(new Entry(xSc, 0f));
                        }
                    }
                }
                Entry entry = new Entry(xSc, ySc);
                dataSet.addEntry(entry);
                lineDataSet.addEntry(entry);
                hasPrev = true;
            } else if (hasPrev && x >= minX && x <= maxX) {
                index++;
                hasPrev = false;
                if (i + 1 < xList.size() && mYScaleStep > 1) {
                    float nextY = yList.get(i + 1);
                    int xSc = (int) (x * scaleX - slideX);
                    if (nextY > y) {
                        lineDataSet.addEntry(new Entry(xSc, 1500f));
                    } else {
                        lineDataSet.addEntry(new Entry(xSc, 0f));
                    }
                }
            }
        }
        if (dataSet.getEntryCount() == 0) {
            Entry entry = new Entry(0f, 0f);
            Entry entry1 = new Entry(15000f, 1000f);
            dataSet.addEntry(entry);
            dataSet.addEntry(entry1);
        }
        if (mYScaleStep > 0 || mXScaleStep > 0) {
            dataSet.addEntry(new Entry(0f, 0f));
            dataSet.addEntry(new Entry(15000f, 1000f));
        }
        mChart.getData().notifyDataChanged();
        mChart.getLineData().notifyDataChanged();
        mChart.notifyDataSetChanged();
        mChart.invalidate();
        reloadTraceLines();
        Log.d(TAG, "reloadData: refresh time " + (System.currentTimeMillis() - start));
    }

    private void initSet(ScatterData scatterData, int i) {
        IScatterDataSet scatterDataSet;
        try {
            scatterDataSet = scatterData.getDataSetByIndex(i);
            if (scatterDataSet == null) {
                scatterDataSet = createSet();
                scatterData.addDataSet(scatterDataSet);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            scatterDataSet = createSet();
            scatterData.addDataSet(scatterDataSet);
        }
    }

    private void initSet(LineData lineData, int i) {
        ILineDataSet lineDataSet;
        try {
            lineDataSet = lineData.getDataSetByIndex(i);
            if (lineDataSet == null) {
                lineDataSet = createLineSet();
                lineData.addDataSet(lineDataSet);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            lineDataSet = createLineSet();
            lineData.addDataSet(lineDataSet);
        }
    }

    private float getDeviationCorrector() {
        return mYMoveStep - (getYParts() / 2 - 1);
    }

    private float getYDeviation() {
        if (mYScaleStep == 0) return DsoActivity.Y_MAX;
        else return DsoActivity.Y_MAX / (float) Math.pow(4, mYScaleStep);
    }

    private float getXScale() {
        return (DsoActivity.X_SCALE_BASE * mXScallar);
    }

    private float getSlideX() {
        return DsoActivity.CHART_MAX_X / 2 * mXMoveStep;
    }

    private float getYScale() {
        float increment = 1f;
        if (mYScaleStep > 0) increment = (float) Math.pow(4, mYScaleStep);
        return DsoActivity.Y_SCALE_BASE * increment;
    }

    private void clearGraph() {
        mChart.getScatterData().clearValues();
        mChart.getLineData().clearValues();
        mChart.invalidate();
        mXVals.clear();
        mYVals.clear();
    }

    private void stopCapturing() {
        sendMessage(Constants.S);
    }

    private void showScreenshots() {
        if (checkReadPermission()) {
            startActivity(new Intent(getActivity(), ImagesActivity.class));
        }
    }

    private void capture() {
        showProgressDialog();
        mXScallar = 1f;
        mXScaleStep = 0;
        mYScaleStep = 0;
        mXMoveStep = 0;
        mYMoveStep = getYParts() / 2;
        setUpClearGraph();
        sendMessage(Constants.C);
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                return false;
            }
            return true;
        }
        return true;
    }

    private boolean checkReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
        Toast.makeText(getActivity(), getString(R.string.screenshot_saved), Toast.LENGTH_SHORT).show();
    }

    private ScatterDataSet createSet() {
        ScatterDataSet dataSet = new ScatterDataSet(null, getString(R.string.arduino_vhart));
        dataSet.setColor(Color.BLACK);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeHoleRadius(0f);
        dataSet.setScatterShapeSize(DsoActivity.CHART_POINT_SIZE);
        dataSet.setDrawValues(false);
        return dataSet;
    }

    private LineDataSet createLineSet() {
        LineDataSet lineDataSet = new LineDataSet(null, getString(R.string.arduino_vhart));
        lineDataSet.setColor(Color.BLACK);
        lineDataSet.setCircleColor(Color.BLACK);
        lineDataSet.setCircleRadius(0.1f);
        lineDataSet.setLineWidth(0.1f);
        lineDataSet.setMode(LineDataSet.Mode.LINEAR);
        return lineDataSet;
    }

    private void sendMessage(String message) {
        if (mFragmentListener == null) {
            mFragmentListener = (FragmentListener) getActivity();
        }
        mFragmentListener.onAction(message);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentListener) {
            mFragmentListener = (FragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof FragmentListener && mFragmentListener == null) {
            mFragmentListener = (FragmentListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentListener = null;
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
                    startActivity(new Intent(getActivity(), ImagesActivity.class));
                }
                break;
        }
    }
}
