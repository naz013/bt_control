package com.example.helio.arduino.core;

public class ConnectionEvent {
    private boolean connected;

    public ConnectionEvent(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }
}
