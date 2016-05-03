package com.backdoor.shared;

import org.json.JSONException;
import org.json.JSONObject;

public class JMessage {

    private JSONObject mObject;
    private boolean isNull;

    public JMessage(String message) {
        try {
            mObject = new JSONObject(message);
            isNull = false;
        } catch (JSONException e) {
            e.printStackTrace();
            isNull = true;
        }
    }

    public JMessage() {
        mObject = new JSONObject();
    }

    public JMessage putFlag(String message) {
        try {
            mObject.put(Constants.FLAG, message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JMessage putYValue(String message) {
        try {
            mObject.put(Constants.Y, message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JMessage putVoltage(String message) {
        try {
            mObject.put(Constants.V, message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JMessage putCurrent(String message) {
        try {
            mObject.put(Constants.I, message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JMessage putResistance(String message) {
        try {
            mObject.put(Constants.R, message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public String asString() {
        return mObject.toString();
    }

    public String getFlag() {
        if (!isNull) {
            try {
                if (mObject.has(Constants.FLAG)) {
                    return mObject.getString(Constants.FLAG);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean hasVoltage() {
        return mObject.has(Constants.V);
    }

    public boolean hasFlag() {
        return mObject.has(Constants.FLAG);
    }

    public boolean hasCurrent() {
        return mObject.has(Constants.I);
    }

    public boolean hasResistance() {
        return mObject.has(Constants.R);
    }

    public String getVoltage() {
        if (!isNull) {
            try {
                if (mObject.has(Constants.V)) {
                    return mObject.getString(Constants.V);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getResistance() {
        if (!isNull) {
            try {
                if (mObject.has(Constants.R)) {
                    return mObject.getString(Constants.R);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getCurrent() {
        if (!isNull) {
            try {
                if (mObject.has(Constants.I)) {
                    return mObject.getString(Constants.I);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getYValue() {
        if (!isNull) {
            try {
                if (mObject.has(Constants.Y)) {
                    return mObject.getString(Constants.Y);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public JMessage putSignal(SignalObject object) {
        try {
            mObject.put(Constants.SIGNAL, object.toJson().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public SignalObject getSignal() {
        if (!isNull) {
            if (mObject.has(Constants.SIGNAL)) {
                try {
                    return new SignalObject(mObject.getJSONObject(Constants.SIGNAL));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
