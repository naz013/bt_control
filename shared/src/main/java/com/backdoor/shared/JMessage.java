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
