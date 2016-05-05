package com.example.helio.arduino;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.backdoor.shared.Constants;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectScreen();
        finish();
    }

    private void selectScreen() {
        if (Build.FINGERPRINT.contains("generic")) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (loadDevice() != null && !loadDevice().matches("")) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, StartActivity.class));
        }
    }

    private String loadDevice() {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        return preferences.getString(Constants.DEVICE_ADDRESS, null);
    }
}
