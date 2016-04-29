package com.example.helio.arduino.dso;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.helio.arduino.Constants;
import com.example.helio.arduino.R;
import com.example.helio.arduino.SettingsActivity;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DSOActivity extends AppCompatActivity implements View.OnClickListener, OnChartGestureListener,
        OnChartValueSelectedListener {

    private static final int CHART_VISIBLE = 50;

    private LineChart mChart;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dso);
        initActionBar();
        initButtons();
        initChart();
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
        generateDataForChart(70, 100);
        mChart.setVisibleXRangeMaximum(CHART_VISIBLE);
        mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);
        mChart.moveViewToX(mChart.getLineData().getXValCount() - CHART_VISIBLE);
    }

    private void initButtons() {
        findViewById(R.id.captureButton).setOnClickListener(this);
        findViewById(R.id.screenshotButton).setOnClickListener(this);
        findViewById(R.id.viewScreenshot).setOnClickListener(this);
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
        }
    }

    private void showScreenshots() {
        if (checkReadPermission()) {
            startActivity(new Intent(this, ImagesActivity.class));
        }
    }

    private void capture() {

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

    private void generateDataForChart(int count, float range) {
        ArrayList<String> xVals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            xVals.add((i) + "");
        }
        ArrayList<Entry> yVals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float mult = (range + 1);
            float val = (float) (Math.random() * mult) + 3;
            yVals.add(new Entry(val, i));
        }

        LineDataSet set1 = new LineDataSet(yVals, "DataSet 1");
        set1.setColor(Color.BLACK);
        set1.setLineWidth(1f);
        set1.setCircleRadius(1f);
        set1.getCircleColor(Color.BLUE);
        set1.setDrawCircleHole(false);
        set1.setValueTextSize(9f);
        set1.setDrawFilled(true);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);
        LineData data = new LineData(xVals, dataSets);
        mChart.setData(data);
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            addNewEntry();
        }
    };

    private void addNewEntry() {
        LineData lineData = mChart.getLineData();
        float mult = (100 + 1);
        float val = (float) (Math.random() * mult) + 3;
        ILineDataSet set = lineData.getDataSetByIndex(0);
        lineData.addXValue(lineData.getXValCount() + " ");
        lineData.addEntry(new Entry(val, set.getEntryCount()), 0);
        updateChart(lineData.getXValCount());
    }

    private void updateChart(int xValCount) {
        mChart.notifyDataSetChanged();
        mChart.setVisibleXRangeMaximum(CHART_VISIBLE);
        mChart.moveViewToX(xValCount - CHART_VISIBLE);
        mHandler.postDelayed(mRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler = new Handler();
        mHandler.postDelayed(mRunnable, 1000);
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
