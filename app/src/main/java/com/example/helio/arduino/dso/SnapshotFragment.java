package com.example.helio.arduino.dso;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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

import java.io.IOException;
import java.util.ArrayList;
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

    public void setData(List<Float> xVals, List<Float> yVals) {
        if (xVals.size() == 0 || yVals.size() == 0) return;
        try {
            DsoWriter.writeToFile(yVals);
        } catch (IOException e) {
            e.printStackTrace();
        }
        hideProgressDialog();
        mChartView.setData(yVals, xVals);
    }

    public void setExtraData(float voltage, float frequency) {
        if (voltage < 0.4) {
            freqView.setText(getString(R.string.f_) + " " + getString(R.string.undefined));
        } else {
            freqView.setText(getString(R.string.f_) + " " + frequency);
        }
        voltageView.setText(getString(R.string.v_p_p) + " " + voltage);
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
        view.findViewById(R.id.screenshot_item).setOnClickListener(mListener);
        view.findViewById(R.id.clearButton).setOnClickListener(mListener);
        view.findViewById(R.id.stopButton).setOnClickListener(mListener);
        view.findViewById(R.id.captureButton).setOnClickListener(mListener);
        view.findViewById(R.id.gallery_item).setOnClickListener(mListener);
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
        traceY.setOnClickListener(mListener);
        traceX.setOnClickListener(mListener);
    }

    private void loadFromFile() {
        List<Float> xVals = new ArrayList<>();
        List<Float> yVals = DsoWriter.readDsoAsArray();
        for (int i = 0; i < yVals.size(); i++) {
            float x = ((float) i / ((float) yVals.size() / 1500f)) * (1f / 1000f);
            xVals.add(x);
        }
        mChartView.setData(yVals, xVals);
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
                mChartView.setUpClearGraph();
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

    private void showScreenshots() {
        if (checkReadPermission()) {
            startActivity(new Intent(getActivity(), ImagesActivity.class));
        }
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
            case 102:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(getActivity(), ImagesActivity.class));
                }
                break;
        }
    }
}
