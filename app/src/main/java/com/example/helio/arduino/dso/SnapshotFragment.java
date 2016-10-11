package com.example.helio.arduino.dso;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.Constants;
import com.example.helio.arduino.dso.chart.ChartController;
import com.example.helio.arduino.dso.chart.ChartListener;
import com.example.helio.arduino.dso.chart.ChartView;
import com.example.helio.arduino.signal.FragmentListener;

import java.util.List;

public class SnapshotFragment extends Fragment {

    private static final String TAG = "SnapshotFragment";
    private FragmentListener mFragmentListener;

    private TextView freqView, voltageView;
    private ImageButton zoomInX, zoomOutX;
    private ImageButton zoomInY, zoomOutY;
    private ImageButton moveRight, moveLeft;
    private ImageButton moveTop, moveBottom;
    private ImageButton traceX, traceY;
    private ProgressDialog mProgressDialog;

    private ChartView mChartView;
    private ChartController mController;
    private ChartListener mChartCallback = new ChartListener() {
        @Override
        public void onRefreshStart() {
            setButtonEnabled(false);
        }

        @Override
        public void onRefreshEnd() {
            setButtonEnabled(true);
        }
    };

    private void setButtonEnabled(boolean b) {
        moveTop.setEnabled(b);
        moveBottom.setEnabled(b);
        moveLeft.setEnabled(b);
        moveRight.setEnabled(b);
        zoomInY.setEnabled(b);
        zoomOutY.setEnabled(b);
        zoomInX.setEnabled(b);
        zoomOutX.setEnabled(b);
        traceX.setEnabled(b);
        traceY.setEnabled(b);
    }

    public SnapshotFragment() {
    }

    public static SnapshotFragment newInstance() {
        return new SnapshotFragment();
    }

    public void makeScreenshot() {
        takeScreenshot();
    }

    public void setData(List<Float> xVals, List<Float> yVals) {
        if (xVals.size() == 0 || yVals.size() == 0) return;
        hideProgressDialog();
        mChartView.setData(yVals, xVals);
    }

    public void setExtraData(float voltage, float frequency) {
        if (voltage < 0.4) {
            freqView.setText(getString(R.string.f_) + " " + getString(R.string.undefined));
        } else {
            freqView.setText(DsoUtil.getFrequencyFormatted(getActivity(), frequency));
        }
        if (frequency < 500) {
            freqView.setText(getString(R.string.f_) + " " + getString(R.string.undefined));
        }
        voltageView.setText(DsoUtil.getVoltageFormatted(getActivity(), voltage));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_snapshot, container, false);
        initTextViews(view);
        initButtons(view);
        initChart(view);
        return view;
    }

    private void initTextViews(View view) {
        freqView = (TextView) view.findViewById(R.id.freqView);
        voltageView = (TextView) view.findViewById(R.id.voltageView);
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        mProgressDialog = ProgressDialog.show(getActivity(), getString(R.string.receiving_data),
                getString(R.string.please_wait), false, false);
    }

    private void initChart(View v) {
        mController = new ChartController();
        mChartView = (ChartView) v.findViewById(R.id.chart1);
        mChartView.setController(mController);
        mChartView.setChartCallback(mChartCallback);
        mController.setListener(mChartView.getListener());
    }

    private void initButtons(View view) {
        view.findViewById(R.id.clearButton).setOnClickListener(mListener);
        view.findViewById(R.id.stopButton).setOnClickListener(mListener);
        view.findViewById(R.id.captureButton).setOnClickListener(mListener);
        traceY = (ImageButton) view.findViewById(R.id.traceY);
        traceX = (ImageButton) view.findViewById(R.id.traceX);
        moveBottom = (ImageButton) view.findViewById(R.id.moveBottom);
        moveTop = (ImageButton) view.findViewById(R.id.moveTop);
        moveLeft = (ImageButton) view.findViewById(R.id.moveLeft);
        moveRight = (ImageButton) view.findViewById(R.id.moveRight);
        zoomInX = (ImageButton) view.findViewById(R.id.zoomInX);
        zoomOutX = (ImageButton) view.findViewById(R.id.zoomOutX);
        zoomInY = (ImageButton) view.findViewById(R.id.zoomInY);
        zoomOutY = (ImageButton) view.findViewById(R.id.zoomOutY);
        zoomInX.setOnClickListener(mListener);
        zoomOutX.setOnClickListener(mListener);
        zoomInY.setOnClickListener(mListener);
        zoomOutY.setOnClickListener(mListener);
        moveBottom.setOnClickListener(mListener);
        moveRight.setOnClickListener(mListener);
        moveLeft.setOnClickListener(mListener);
        moveTop.setOnClickListener(mListener);
        traceY.setOnClickListener(mListener);
        traceX.setOnClickListener(mListener);
    }

    private View.OnClickListener mListener = v -> {
        switch (v.getId()) {
            case R.id.captureButton:
                capture();
                break;
            case R.id.stopButton:
                stopCapturing();
                break;
            case R.id.clearButton:
                mChartView.setUpClearGraph();
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
                mChartView.traceX();
                break;
            case R.id.traceY:
                mChartView.traceY();
                break;
        }
    };

    private void moveY(int i) {
        mController.moveY(i);
    }

    private void moveX(int i) {
        mController.moveX(i);
    }

    private void scaleY(int i) {
        mController.scaleY(i);
    }

    private void scaleX(int i) {
        mController.scaleX(i);
    }

    private void stopCapturing() {
        sendMessage(Constants.S);
        setButtonEnabled(true);
    }

    private void capture() {
        showProgressDialog();
        mChartView.setUpClearGraph();
        sendMessage(Constants.C);
        setButtonEnabled(false);
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

    private void takeScreenshot() {
        if (checkPermission()) {
            mChartView.saveChartToImageFile();
        }
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
        }
    }
}
