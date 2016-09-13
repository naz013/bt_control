package com.example.helio.arduino.dso.chart;

import android.util.Log;

public class ChartController {
    private static final String TAG = "ChartController";
    public static final float CHART_MAX_Y = 1000f;
    public static final float CHART_MAX_X = 15000f;
    public static final float MAX_X = 1500f;
    public static final float X_SCALE_BASE = 10000f;
    public static final float Y_SCALE_BASE = 31.25f;
    public static final float CHART_POINT_SIZE = 1.0f;
    public static final float RANGE_DIVIDER = 2f;
    public static final float Y_MAX = 16f;
    public static final float Y_MIN = -16f;

    private float mXScalar = 1f;
    private int mXScaleStep = 0;
    private int mYScaleStep = 0;
    private int mXMoveStep = 0;
    private int mYMoveStep = getYParts() / 2;

    private float scaleX;
    private float scaleY;
    private float slideX;
    private float deviationY;
    private float maxX;
    private float minX;
    private float deviationCorrector;
    private float maxY;
    private float minY;

    private ControlListener mListener;

    public ChartController() {
        setInitialState();
    }

    public ChartController(ControlListener mListener) {
        this.mListener = mListener;
    }

    public void setListener(ControlListener listener) {
        this.mListener = listener;
    }

    public float getSlideX() {
        return slideX;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getDeviationCorrector() {
        return deviationCorrector;
    }

    public float getDeviationY() {
        return deviationY;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMinX() {
        return minX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxY() {
        return maxY;
    }

    public void calculateNewParameters() {
        scaleX = getXScale();
        scaleY = getYScale();
        slideX = calculateSlideX();
        deviationY = getYDeviation();
        maxX = CHART_MAX_X / scaleX + (slideX / scaleX);
        minX = mXMoveStep > 0 ? (maxX - (CHART_MAX_X / scaleX)) : 0f;
        float baseY = (CHART_MAX_Y / 2) / scaleY;
        float yMoveMiddle = getYParts() / 2;
        deviationCorrector = calculateDeviationCorrector();
        maxY = baseY + ((yMoveMiddle - mYMoveStep) * baseY);
        minY = mYMoveStep != yMoveMiddle ? (maxY - baseY * 2) : -deviationY;
        if (mYScaleStep == 0) {
            maxY = Y_MAX;
            minY = Y_MIN;
        }
    }

    public int getXScaleStep() {
        return mXScaleStep;
    }

    public int getXMoveStep() {
        return mXMoveStep;
    }

    public int getYScaleStep() {
        return mYScaleStep;
    }

    public int getYMoveStep() {
        return mYMoveStep;
    }

    public void setInitialState() {
        mXScalar = 1f;
        mXScaleStep = 0;
        mYScaleStep = 0;
        mXMoveStep = 0;
        mYMoveStep = getYParts() / 2;
    }

    public float getXFormatScale() {
        if (mXScalar > 1000000) {
            return mXScalar / 1000000;
        } else if (mXScalar > 1000) {
            return mXScalar / 10000;
        } else if (mXScalar == 1000 || mXScalar == 1) {
            return 1000;
        } else {
            return mXScalar;
        }
    }

    public float getYFormatScale() {
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

    public void moveY(int i) {
        if (mYScaleStep == 0) return;
        if (i > 0 && mYMoveStep == 1) return;
        if (i < 0 && mYMoveStep == getYParts() - 1) return;
        mYMoveStep -= i;
        if (mListener != null) mListener.onChange();
        Log.d(TAG, "moveY: " + mYMoveStep);
    }

    public void moveX(int i) {
        if (mXScaleStep == 0) return;
        if (i < 0 && mXMoveStep == 0) return;
        int mXParts = getXPartsCount();
        if (i > 0 && mXMoveStep == (mXParts - 2)) return;
        mXMoveStep += i;
        if (mListener != null) mListener.onChange();
        Log.d(TAG, "moveX: " + mXMoveStep);
    }

    private int getXPartsCount() {
        return (int) ((int) X_SCALE_BASE / getXPartSize());
    }

    private float getXPartSize() {
        return X_SCALE_BASE / mXScalar / RANGE_DIVIDER;
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
        else if (mYMoveStep == 0) mYMoveStep = 1;
        if (mListener != null) mListener.onChange();
        Log.d(TAG, "scaleY: " + mYScaleStep);
    }

    private int getYParts() {
        return ((int) Math.round(Math.pow(4, mYScaleStep))) * 2;
    }

    public void scaleX(int i) {
        if (i < 0 && mXScaleStep == 0) return;
        if (i > 0 && mXScaleStep == 5) return;
        float xParts = X_SCALE_BASE / getXPartSize();
        float percent = (float) mXMoveStep / xParts;
        if (i > 0) mXScalar *= 2;
        if (i < 0) mXScalar /= 2;
        mXMoveStep = 0;
        mXScaleStep += i;
        xParts = X_SCALE_BASE / getXPartSize();
        mXMoveStep = (int) (xParts * percent);
        if (mXMoveStep >= (xParts - 2)) mXMoveStep = (int) xParts - 2;
        if (mListener != null) mListener.onChange();
        Log.d(TAG, "scaleX: " + mXScaleStep);
    }

    public float calculateDeviationCorrector() {
        return mYMoveStep - (getYParts() / 2 - 1);
    }

    public float getYDeviation() {
        if (mYScaleStep == 0) return Y_MAX;
        else return Y_MAX / (float) Math.pow(4, mYScaleStep);
    }

    public float getXScale() {
        return (X_SCALE_BASE * mXScalar);
    }

    public float calculateSlideX() {
        return CHART_MAX_X / 2 * mXMoveStep;
    }

    public float getYScale() {
        float increment = 1f;
        if (mYScaleStep > 0) increment = (float) Math.pow(4, mYScaleStep);
        return Y_SCALE_BASE * increment;
    }

    public float calculateYLabel(float value) {
        float scalar = getYFormatScale();
        float deviation = getYDeviation();
        float deviationCorrector = calculateDeviationCorrector();
        float f = ((value - ChartController.CHART_MAX_Y / 2) / scalar);
        if (mYScaleStep > 2  && mYMoveStep != getYParts() / 2) {
            f = f - ((deviation * (deviationCorrector - 1)) * ChartController.CHART_MAX_Y);
        } else if (mYScaleStep > 0 && mYMoveStep != getYParts() / 2) {
            f = f - (deviation * (deviationCorrector - 1));
        }
        return f;
    }

    public float calculateXLabel(float value) {
        float scalar = getXFormatScale();
        float f = (value + calculateSlideX()) / (scalar * 10);
        if (mXScaleStep == 0) {
            f = (value + calculateSlideX()) / (scalar * 10);
        }
        return f;
    }
}
