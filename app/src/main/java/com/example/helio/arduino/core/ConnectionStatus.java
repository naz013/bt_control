package com.example.helio.arduino.core;

public class ConnectionStatus {
    private boolean connected;

    public ConnectionStatus(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }
}
