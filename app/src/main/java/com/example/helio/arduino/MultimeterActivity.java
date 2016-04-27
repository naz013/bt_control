package com.example.helio.arduino;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MultimeterActivity extends AppCompatActivity implements View.OnClickListener {

    private Button resistanceButton;
    private Button voltageButton;
    private Button currentButton;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multimeter);
        initActionBar();
        initButtons();
    }

    private void initButtons() {
        resistanceButton = (Button) findViewById(R.id.resistanceButton);
        voltageButton = (Button) findViewById(R.id.voltageButton);
        currentButton = (Button) findViewById(R.id.currentButton);

        resistanceButton.setOnClickListener(this);
        voltageButton.setOnClickListener(this);
        currentButton.setOnClickListener(this);
    }

    private void initActionBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setTitle(R.string.multimeter);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    }

    private void showCurrent() {

    }

    private void showResistance() {

    }

    private void showVoltage() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.resistanceButton:
                showResistance();
                break;
            case R.id.voltageButton:
                showVoltage();
                break;
            case R.id.currentButton:
                showCurrent();
                break;
        }
    }

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
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
