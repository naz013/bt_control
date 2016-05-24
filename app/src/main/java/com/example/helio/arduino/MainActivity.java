package com.example.helio.arduino;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.example.helio.arduino.dso.DSOActivity;
import com.example.helio.arduino.signal.SignalActivity;

public class MainActivity extends AppCompatActivity {

    private static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_main);
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        initActionBar();
        initButtons();
    }

    public static Activity getActivity() {
        return activity;
    }

    private void initButtons() {
        findViewById(R.id.multimeterButton).setOnClickListener(mListener);
        findViewById(R.id.dsoButton).setOnClickListener(mListener);
        findViewById(R.id.signalButton).setOnClickListener(mListener);
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
        }
        toolbar.setTitle(R.string.main_menu);
    }

    private void openSignal() {
        startActivity(new Intent(this, SignalActivity.class));
    }

    private void openDSO() {
        startActivity(new Intent(this, DSOActivity.class));
    }

    private void openMultimeter() {
        startActivity(new Intent(this, MultimeterActivity.class));
    }

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.multimeterButton:
                    openMultimeter();
                    break;
                case R.id.dsoButton:
                    openDSO();
                    break;
                case R.id.signalButton:
                    openSignal();
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
