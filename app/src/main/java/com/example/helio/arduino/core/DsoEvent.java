package com.example.helio.arduino.core;

public class DsoEvent {
    private short[] array;

    public DsoEvent(short[] array) {
        this.array = array;
    }

    public short[] getArray() {
        return array;
    }
}
