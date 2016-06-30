package com.example.helio.arduino.core;

public class ControlEvent {
    private String msg;

    public ControlEvent(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
