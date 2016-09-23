package com.example.helio.arduino.dso;

import android.content.Context;

import com.example.helio.arduino.R;

import java.util.Locale;

class DsoUtil {
    static String getFrequencyFormatted(Context context, float frequency) {
        if (frequency < 1000) {
            return context.getString(R.string.f_) + " " + ((int) frequency) + " " + context.getString(R.string.hz);
        } else {
            frequency /= 1000f;
            return String.format(Locale.getDefault(), context.getString(R.string.f_) + " %.3f " + context.getString(R.string.khz), frequency);
        }
    }

    static String getVoltageFormatted(Context context, float voltage) {
        return String.format(Locale.getDefault(), context.getString(R.string.v_p_p) + " %.2f " + context.getString(R.string.v_low), voltage);
    }
}
