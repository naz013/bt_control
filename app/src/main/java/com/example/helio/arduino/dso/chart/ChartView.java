package com.example.helio.arduino.dso.chart;

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

    private Context mContext;

    private List<Float> mYVals = new ArrayList<>();
    private List<Float> mXVals = new ArrayList<>();
    private boolean mIsYTracing = false;
    private boolean mIsXTracing = false;
    private float mPreviousX = 0f;
    private float mPreviousY = 0f;

    private ScatterChart mChart;
    private ChartController mControl;
    private ControlListener mListener = () -> reloadData(mYVals, mXVals);
    private ChartListener mChartCallback;

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

    public void setChartCallback(ChartListener chartCallback) {
        this.mChartCallback = chartCallback;
    }

    public ControlListener getListener() {
        return mListener;
    }

    public void setData(List<Float> mYValues, List<Float> mXValues) {
        this.mXVals = mXValues;
        this.mYVals = mYValues;
        reloadData(mYValues, mXValues);
    }

    public void setController(ChartController controller) {
        this.mControl = controller;
        refreshChart();
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
                    }
                    if (mIsYTracing) {
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
        mChart.setDescription("");
        ArrayList<Entry> entries = new ArrayList<>();
        ScatterDataSet dataSet = new ScatterDataSet(entries, "");
        dataSet.setColor(Color.BLACK);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeHoleRadius(0f);
        dataSet.setScatterShapeSize(ChartController.CHART_POINT_SIZE);
        ScatterData scatterData = new ScatterData(dataSet);
        scatterData.setDrawValues(false);
        LineDataSet lineDataSet = new LineDataSet(entries, "");
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
    }

    public void setUpClearGraph() {
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
        mChart.setAutoScaleMinMaxEnabled(false);
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
        return (float) pointD.y + ChartController.CHART_MAX_Y / 1.5f;
    }

    private void refreshChart() {
        YAxis yAxis = mChart.getAxisLeft();
        yAxis.removeAllLimitLines();
        yAxis.setAxisMaxValue(ChartController.CHART_MAX_Y);
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
        xAxis.setAxisMaxValue(ChartController.CHART_MAX_X);
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

    public void traceY() {
        mIsYTracing = !mIsYTracing;
        mChart.getXAxis().removeAllLimitLines();
        if (mIsYTracing) {
            drawHorizontalLine(ChartController.CHART_MAX_Y / 2);
        } else {
            clearHorizontalLines();
        }
        if (mIsXTracing) {
            clearVerticalLines();
            drawVerticalLine(mPreviousX);
        }
    }

    private void clearHorizontalLines() {
        mChart.getAxisLeft().removeAllLimitLines();
        mChart.invalidate();
    }

    private void drawHorizontalLine(float position) {
        mPreviousY = position;
        mChart.getAxisLeft().removeAllLimitLines();
        LimitLine yLimit = new LimitLine(position);
        yLimit.setLineColor(getResources().getColor(R.color.colorRed));
        yLimit.setLabel(getYLabelFormatted(position, true));
        yLimit.setTextSize(20f);
        if (position > ChartController.CHART_MAX_Y / 2) yLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        else yLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        yLimit.setTextColor(getResources().getColor(R.color.colorBlue));
        mChart.getAxisLeft().setDrawLimitLinesBehindData(false);
        mChart.getAxisLeft().addLimitLine(yLimit);
        mChart.invalidate();
    }

    private String getYLabelFormatted(float value, boolean trace) {
        float f = mControl.calculateYLabel(value);
        if (value == ChartController.CHART_MAX_Y && !trace) {
            return String.format(Locale.getDefault(), getYUnitLabel(), f);
        } else {
            return String.format(Locale.getDefault(), "%.2f", f);
        }
    }

    private String getYUnitLabel() {
        if (mControl.getYScaleStep() > 2) {
            return "(mv)\n%.2f";
        } else {
            return "(v)\n%.2f";
        }
    }

    private void drawVerticalLine(float position) {
        mPreviousX = position;
        mChart.getXAxis().removeAllLimitLines();
        LimitLine xLimit = new LimitLine(position);
        xLimit.setLineColor(getResources().getColor(R.color.colorRed));
        xLimit.setLabel(getXLabelFormatted(position, true));
        xLimit.setTextSize(20f);
        if (position > ChartController.CHART_MAX_X / 2) xLimit.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
        else xLimit.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        xLimit.setTextColor(getResources().getColor(R.color.colorGreen));
        mChart.getXAxis().addLimitLine(xLimit);
        mChart.getXAxis().setDrawLimitLinesBehindData(false);
        mChart.invalidate();
    }

    private String getXLabelFormatted(float value, boolean trace) {
        float f = mControl.calculateXLabel(value);
        if (value > 14500 && !trace) {
            return String.format(Locale.getDefault(), getXUnitLabel(), f);
        } else {
            return String.format(Locale.getDefault(), getXLabel(), f);
        }
    }

    private String getXLabel() {
        if (mControl.getXScaleStep() > 0) {
            return "%.0f";
        } else {
            return "%.2f";
        }
    }

    private String getXUnitLabel() {
        if (mControl.getXScaleStep() > 0) {
            return "%.0f(uS)";
        } else {
            return "%.2f(mS)";
        }
    }

    public void traceX() {
        mIsXTracing = !mIsXTracing;
        mChart.getAxisLeft().removeAllLimitLines();
        if (mIsXTracing) {
            drawVerticalLine(ChartController.CHART_MAX_X / 2);
        } else {
            clearVerticalLines();
        }
        if (mIsYTracing) {
            clearHorizontalLines();
            drawHorizontalLine(mPreviousY);
        }
    }

    private void clearVerticalLines() {
        mChart.getXAxis().removeAllLimitLines();
        mChart.invalidate();
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

    private synchronized void reloadData(List<Float> mYValues, List<Float> mXValues) {
        if (mChartCallback != null) mChartCallback.onRefreshStart();
        long start = System.currentTimeMillis();
        if (mYValues.size() == 0 || mXValues.size() == 0) {
            mChart.invalidate();
            if (mChartCallback != null) mChartCallback.onRefreshEnd();
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
        mControl.calculateNewParameters();
        float scaleX = mControl.getScaleX();
        float scaleY = mControl.getScaleY();
        float slideX = mControl.getSlideX();
        float deviationY = mControl.getDeviationY();
        float maxX = mControl.getMaxX();
        float minX = mControl.getMinX();
        float deviationCorrector = mControl.getDeviationCorrector();
        float maxY = mControl.getMaxY();
        float minY = mControl.getMinY();
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
                        if (mControl.getYScaleStep() > 0) {
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
                    if (i - 1 > 0 && mControl.getYScaleStep() > 0) {
                        float prevY = yList.get(i - 1);
                        addPreviousIfNoPoint(prevY, y, xSc, lineDataSet);
                    }
                } else {
                    initSet(lineData, index);
                    lineDataSet = lineData.getDataSetByIndex(index);
                    if (mControl.getYScaleStep() > 0 && i > 0) {
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
        if (mChartCallback != null) mChartCallback.onRefreshEnd();
    }

    private void addFakePoint(float prevY, int prevX, int prevYsc, ILineDataSet lineDataSet, int xSc, float maxY, float minY, float y) {
        if (prevY < minY && y > maxY) {
            lineDataSet.addEntry(new Entry(prevX, 0f));
            lineDataSet.addEntry(new Entry(xSc, ChartController.CHART_MAX_Y));
        } else if (prevY > maxY && y < minY) {
            lineDataSet.addEntry(new Entry(prevX, ChartController.CHART_MAX_Y));
            lineDataSet.addEntry(new Entry(xSc, 0f));
        } else if (prevY >= minY && prevY <= maxY) {
            if (y > maxY) {
                lineDataSet.addEntry(new Entry(prevX, prevYsc));
                lineDataSet.addEntry(new Entry(xSc, ChartController.CHART_MAX_Y));
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
            lineDataSet.addEntry(new Entry(xSc, ChartController.CHART_MAX_Y));
        }
    }

    private void addPreviousIfHasPoint(float prevY, float y, int xSc, ILineDataSet lineDataSet) {
        if (prevY > y) {
            lineDataSet.addEntry(new Entry(xSc, ChartController.CHART_MAX_Y));
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
        dataSet.addEntry(new Entry(0f, 0f));
        dataSet.addEntry(new Entry(ChartController.CHART_MAX_X, ChartController.CHART_MAX_Y));
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
        dataSet.setScatterShapeSize(ChartController.CHART_POINT_SIZE);
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
