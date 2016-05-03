package com.backdoor.shared;

import org.json.JSONException;
import org.json.JSONObject;

public class SignalObject {

    private int waveType;
    private int frequency;
    private int frequencyModifier;
    private int magnitude;

    public SignalObject(int waveType, int frequency, int frequencyModifier, int magnitude) {
        this.waveType = waveType;
        this.frequency = frequency;
        this.frequencyModifier = frequencyModifier;
        this.magnitude = magnitude;
    }

    public SignalObject(JSONObject jsonObject) {
        try {
            if (jsonObject.has(Constants.SIGNAL_TYPE)) {
                waveType = jsonObject.getInt(Constants.SIGNAL_TYPE);
            }
            if (jsonObject.has(Constants.FREQUENCY)) {
                frequency = jsonObject.getInt(Constants.FREQUENCY);
            }
            if (jsonObject.has(Constants.FREQUENCY_M)) {
                frequencyModifier = jsonObject.getInt(Constants.FREQUENCY_M);
            }
            if (jsonObject.has(Constants.MAGNITUDE)) {
                magnitude = jsonObject.getInt(Constants.MAGNITUDE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getWaveType() {
        return waveType;
    }

    public int getFrequency() {
        return frequency;
    }

    public int getFrequencyModifier() {
        return frequencyModifier;
    }

    public int getMagnitude() {
        return magnitude;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(Constants.SIGNAL_TYPE, waveType);
            jsonObject.put(Constants.FREQUENCY, frequency);
            jsonObject.put(Constants.FREQUENCY_M, frequencyModifier);
            jsonObject.put(Constants.MAGNITUDE, magnitude);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
