package com.example.helio.arduino.dso;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.Constants;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChartView extends LinearLayout {

    private static final String TAG = "ChartView";
    public static final float CHART_MAX_Y = 1000f;
    public static final float CHART_MAX_X = 15000f;
    public static final float MAX_X = 1500f;
    public static final float X_SCALE_BASE = 10000f;
    public static final float Y_SCALE_BASE = 31.25f;
    public static final float CHART_POINT_SIZE = 1.0f;
    public static final float RANGE_DIVIDER = 2f;
    public static final float Y_MAX = 16f;
    public static final float Y_MIN = -16f;
    private Context mContext;

    private boolean mIsYTracing = false;
    private boolean mIsXTracing = false;
    private float mXScallar = 1f;
    private int mXScaleStep = 0;
    private int mYScaleStep = 0;
    private int mXMoveStep = 0;
    private int mYMoveStep = getYParts() / 2;
    private List<Float> mYVals = new ArrayList<>();
    private List<Float> mXVals = new ArrayList<>();

    private ScatterChart mChart;

    public ChartView(Context context) {
        super(context);
        init(context, null);
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {
        this.mContext = context;
        if (isInEditMode()) return;
        initChart();
        setUpClearGraph();
    }

    public void setData(List<Float> mYValues, List<Float> mXValues) {
        this.mXVals = mXValues;
        this.mYVals = mYValues;
        reloadData(mYValues, mXValues);
    }

    private void initChart() {
        Log.d(TAG, "initChart: ");
        View.inflate(mContext, R.layout.chart_view_layout, this);
        mChart = (ScatterChart) findViewById(R.id.scatterChart);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        mChart.setLayoutParams(param);
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
        mChart.setNoDataText(mContext.getString(R.string.no_data_available));
        mChart.setAutoScaleMinMaxEnabled(false);
        mChart.setDescription(mContext.getString(R.string.arduino_chart));
        ArrayList<Entry> entries = new ArrayList<>();
        ScatterDataSet dataSet = new ScatterDataSet(entries, mContext.getString(R.string.arduino_vhart));
        dataSet.setColor(Color.BLACK);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeHoleRadius(0f);
        dataSet.setScatterShapeSize(CHART_POINT_SIZE);
        ScatterData scatterData = new ScatterData(dataSet);
        scatterData.setDrawValues(false);
        LineDataSet lineDataSet = new LineDataSet(entries, mContext.getString(R.string.arduino_vhart));
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

    public void setUpClearGraph() {
        mXScallar = 1f;
        mXScaleStep = 0;
        mYScaleStep = 0;
        mXMoveStep = 0;
        mYMoveStep = getYParts() / 2;
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
        return (float) pointD.y + CHART_MAX_Y / 2;
    }

    private void refreshChart() {
        YAxis yAxis = mChart.getAxisLeft();
        yAxis.removeAllLimitLines();
        yAxis.setAxisMaxValue(CHART_MAX_Y);
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
        xAxis.setAxisMaxValue(CHART_MAX_X);
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

    public void traceY() {
        mIsYTracing = !mIsYTracing;
        mIsXTracing = false;
        mChart.getXAxis().removeAllLimitLines();
        if (mIsYTracing) {
            drawHorizontalLine(CHART_MAX_Y / 2);
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
        if (position > CHART_MAX_Y / 2) yLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
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
        float f = ((value - CHART_MAX_Y / 2) / scalar);
        if (mYScaleStep > 2  && mYMoveStep != getYParts() / 2) {
            f = f - ((deviation * (deviationCorrector - 1)) * CHART_MAX_Y);
        } else if (mYScaleStep > 0 && mYMoveStep != getYParts() / 2) {
            f = f - (deviation * (deviationCorrector - 1));
        }
        if (value == CHART_MAX_Y && !trace) {
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
        if (position > CHART_MAX_X / 2) xLimit.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
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

    public void traceX() {
        mIsXTracing = !mIsXTracing;
        mIsYTracing = false;
        mChart.getAxisLeft().removeAllLimitLines();
        if (mIsXTracing) {
            drawVerticalLine(CHART_MAX_X / 2);
        } else {
            mChart.getXAxis().removeAllLimitLines();
            mChart.invalidate();
        }
    }

    public void moveY(int i) {
        if (mYScaleStep == 0) return;
        if (i > 0 && mYMoveStep == 1) return;
        if (i < 0 && mYMoveStep == getYParts() - 1) return;
        mYMoveStep -= i;
        reloadData(mYVals, mXVals);
    }

    public void moveX(int i) {
        if (mXScaleStep == 0) return;
        if (i < 0 && mXMoveStep == 0) return;
        int mXParts = (int) ((int) X_SCALE_BASE / getXPartSize());
        if (i > 0 && mXMoveStep == (mXParts - 2)) return;
        mXMoveStep += i;
        reloadData(mYVals, mXVals);
    }

    private float getXPartSize() {
        return X_SCALE_BASE / mXScallar / RANGE_DIVIDER;
    }

    public void scaleY(int i) {
        if (i < 0 && mYScaleStep == 0) return;
        if (i > 0 && mYScaleStep == 4) return;
        int mYParts = getYParts();
        float percent = (float) mYMoveStep / (float) mYParts;
        mYScaleStep += i;
        mYParts = getYParts();
        mYMoveStep = (int) ((float) mYParts * percent);
        if (mYMoveStep >= getYParts() - 1) mYMoveStep = getYParts() - 1;
        reloadData(mYVals, mXVals);
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

    public void scaleX(int i) {
        if (i < 0 && mXScaleStep == 0) return;
        if (i > 0 && mXScaleStep == 5) return;
        float xParts = X_SCALE_BASE / getXPartSize();
        float percent = (float) mXMoveStep / xParts;
        if (i > 0) mXScallar *= 2;
        if (i < 0) mXScallar /= 2;
        mXMoveStep = 0;
        mXScaleStep += i;
        xParts = X_SCALE_BASE / getXPartSize();
        mXMoveStep = (int) (xParts * percent);
        if (mXMoveStep >= (xParts - 2)) mXMoveStep = (int) xParts - 2;
        reloadData(mYVals, mXVals);
    }

    private synchronized void reloadData(List<Float> mYValues, List<Float> mXValues) {
        long start = System.currentTimeMillis();
        if (mYValues.size() == 0 || mXValues.size() == 0) {
            mChart.invalidate();
            return;
        }
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
        float maxX = CHART_MAX_X / scaleX + (slideX / scaleX);
        float minX = mXMoveStep > 0 ? (maxX - (CHART_MAX_X / scaleX)) : 0f;
        float baseY = (CHART_MAX_Y / 2) / scaleY;
        float yMoveMiddle = getYParts() / 2;
        float deviationCorrector = getDeviationCorrector();
        float maxY = baseY + ((yMoveMiddle - mYMoveStep) * baseY);
        float minY = mYMoveStep != yMoveMiddle ? (maxY - baseY * 2) : -deviationY;
        if (mYScaleStep == 0) {
            maxY = Y_MAX;
            minY = Y_MIN;
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
            if (x >= minX && x <= maxX) {
                int xSc = (int) (x * scaleX - slideX);
                int ySc = (int) ((y + (deviationY * deviationCorrector)) * scaleY);
                if (y >= minY && y <= maxY) {
                    if (!hasPrev && i > 0) {
                        initSet(lineData, index);
                        lineDataSet = lineData.getDataSetByIndex(index);
                        if (mYScaleStep > 0) {
                            float prevY = yList.get(i - 1);
                            addPreviousIfHasPoint(prevY, y, xSc, lineDataSet);
                        }
                    }
                    Entry entry = new Entry(xSc, ySc);
                    dataSet.addEntry(entry);
                    lineDataSet.addEntry(entry);
                    hasPrev = true;
                } else if (hasPrev) {
                    index++;
                    hasPrev = false;
                    if (i - 1 > 0 && mYScaleStep > 0) {
                        float prevY = yList.get(i - 1);
                        addPreviousIfNoPoint(prevY, y, xSc, lineDataSet);
                    }
                } else {
                    initSet(lineData, index);
                    lineDataSet = lineData.getDataSetByIndex(index);
                    if (mYScaleStep > 0 && i > 0) {
                        float prevY = yList.get(i - 1);
                        int prevX = (int) (xList.get(i - 1) * scaleX - slideX);
                        int prevYsc = (int) ((prevY + (deviationY * deviationCorrector)) * scaleY);
                        addFakePoint(prevY, prevX, prevYsc, lineDataSet, xSc, maxY, minY, y);
                        if (y < minY || y > maxY) {
                            index++;
                        }
                    }
                }
            }
        }
        addEmptyPoints(dataSet);
        invalidateChart();
        reloadTraceLines();
        Log.d(TAG, "reloadData: refresh time " + (System.currentTimeMillis() - start));
    }

    private void addFakePoint(float prevY, int prevX, int prevYsc, ILineDataSet lineDataSet, int xSc, float maxY, float minY, float y) {
        if (prevY < minY && y > maxY) {
            lineDataSet.addEntry(new Entry(prevX, 0f));
            lineDataSet.addEntry(new Entry(xSc, 1500f));
        } else if (prevY > maxY && y < minY) {
            lineDataSet.addEntry(new Entry(prevX, 1500f));
            lineDataSet.addEntry(new Entry(xSc, 0f));
        } else if (prevY >= minY && prevY <= maxY) {
            if (y > maxY) {
                lineDataSet.addEntry(new Entry(prevX, prevYsc));
                lineDataSet.addEntry(new Entry(xSc, 1500f));
            } else if (y < minY) {
                lineDataSet.addEntry(new Entry(prevX, prevYsc));
                lineDataSet.addEntry(new Entry(xSc, 0f));
            }
        }
    }

    private void addPreviousIfNoPoint(float prevY, float y, int xSc, ILineDataSet lineDataSet) {
        if (prevY > y) {
            lineDataSet.addEntry(new Entry(xSc, 0f));
        } else {
            lineDataSet.addEntry(new Entry(xSc, 1500f));
        }
    }

    private void addPreviousIfHasPoint(float prevY, float y, int xSc, ILineDataSet lineDataSet) {
        if (prevY > y) {
            lineDataSet.addEntry(new Entry(xSc, 1500f));
        } else {
            lineDataSet.addEntry(new Entry(xSc, 0f));
        }
    }

    private void invalidateChart() {
        mChart.getData().notifyDataChanged();
        mChart.getLineData().notifyDataChanged();
        mChart.notifyDataSetChanged();
        mChart.invalidate();
    }

    private void addEmptyPoints(IScatterDataSet dataSet) {
        if (dataSet.getEntryCount() == 0) {
            dataSet.addEntry(new Entry(0f, 0f));
            dataSet.addEntry(new Entry(15000f, 1000f));
        } else if (mYScaleStep > 0 || mXScaleStep > 0) {
            dataSet.addEntry(new Entry(0f, 0f));
            dataSet.addEntry(new Entry(15000f, 1000f));
        }
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
        if (mYScaleStep == 0) return Y_MAX;
        else return Y_MAX / (float) Math.pow(4, mYScaleStep);
    }

    private float getXScale() {
        return (X_SCALE_BASE * mXScallar);
    }

    private float getSlideX() {
        return CHART_MAX_X / 2 * mXMoveStep;
    }

    private float getYScale() {
        float increment = 1f;
        if (mYScaleStep > 0) increment = (float) Math.pow(4, mYScaleStep);
        return Y_SCALE_BASE * increment;
    }

    private void clearGraph() {
        mChart.getScatterData().clearValues();
        mChart.getLineData().clearValues();
        mChart.invalidate();
        mXVals.clear();
        mYVals.clear();
    }

    public void saveChartToImageFile() {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Constants.SCREENS_FOLDER);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return;
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat(mContext.getString(R.string.date_time_sdf), Locale.getDefault());
        String fileName = sdf.format(new Date());
        mChart.saveToPath(fileName, "/" + Constants.SCREENS_FOLDER);
        Toast.makeText(mContext, mContext.getString(R.string.screenshot_saved), Toast.LENGTH_SHORT).show();
    }

    private ScatterDataSet createSet() {
        ScatterDataSet dataSet = new ScatterDataSet(null, mContext.getString(R.string.arduino_vhart));
        dataSet.setColor(Color.BLACK);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeHoleRadius(0f);
        dataSet.setScatterShapeSize(CHART_POINT_SIZE);
        dataSet.setDrawValues(false);
        return dataSet;
    }

    private LineDataSet createLineSet() {
        LineDataSet lineDataSet = new LineDataSet(null, mContext.getString(R.string.arduino_vhart));
        lineDataSet.setColor(Color.BLACK);
        lineDataSet.setCircleColor(Color.BLACK);
        lineDataSet.setCircleRadius(0.1f);
        lineDataSet.setLineWidth(0.1f);
        lineDataSet.setMode(LineDataSet.Mode.LINEAR);
        return lineDataSet;
    }
}
