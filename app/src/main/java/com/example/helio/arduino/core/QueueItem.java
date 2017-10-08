package com.example.helio.arduino.core;

import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

public class QueueItem {
    private byte[] data;
    private String uuId;
    private RequestAction action;

    public QueueItem(byte[] data, RequestAction action) {
        this.action = action;
        this.data = data;
        Log.d("QueueItem ", "QueueItem: " + Arrays.toString(data));
        this.uuId = UUID.randomUUID().toString();
    }

    public RequestAction getAction() {
        return action;
    }

    public byte[] getData() {
        return data;
    }

    public String getUuId() {
        return uuId;
    }
}