package com.example.helio.arduino.transferring;

public class Constants {

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final int KEY_MULTIMETER = 7;
    public static final int KEY_VOLTAGE = 8;
    public static final int KEY_RESISTANCE = 9;
    public static final int KEY_CURRENT = 10;

    public static final int KEY_CAPTURE = 11;
    public static final int KEY_DSO = 15;
    public static final int KEY_STOP = 12;

    public static final int KEY_TERMINATE = 13;
    public static final int KEY_GENERATE = 14;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String RESISTANCE = "resistance";
    public static final String CURRENT = "current";
    public static final String VOLTAGE = "voltage";
    public static final String DSO = "dso";

}
