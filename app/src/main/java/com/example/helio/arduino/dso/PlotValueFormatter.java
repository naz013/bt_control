package com.example.helio.arduino.dso;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class PlotValueFormatter implements com.github.mikephil.charting.formatter.ValueFormatter {
    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return entry.getXIndex() + "," + Math.round(value);
    }
}
