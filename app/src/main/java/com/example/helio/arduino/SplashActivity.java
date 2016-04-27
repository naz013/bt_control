package com.example.helio.arduino;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (loadDevice() != null && !loadDevice().matches("")) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, StartActivity.class));
        }

        finish();
    }

    private String loadDevice() {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        String address = preferences.getString(Constants.DEVICE_ADDRESS, null);
        return address;
    }
}
