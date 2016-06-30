package com.example.helio.arduino.dso;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.Locale;

public class PlotValueFormatter implements ValueFormatter {
    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        float f = entry.getXIndex() / 100f;
        return String.format(Locale.getDefault(), "%.2f, %.2f", f, value);
    }
}
