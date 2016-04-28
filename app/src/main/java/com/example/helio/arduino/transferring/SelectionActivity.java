package com.example.helio.arduino.transferring;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.example.helio.arduino.R;

public class SelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_type);
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        initActionBar();
        initButtons();
    }

    private void initButtons() {
        findViewById(R.id.serverButton).setOnClickListener(mListener);
        findViewById(R.id.clientButton).setOnClickListener(mListener);
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle(R.string.select_connection_type);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    }

    private void openAsServer() {
        startActivity(new Intent(this, ChatActivity.class)
                .putExtra(getString(R.string.intent_server_key), true));
    }

    private void openAsClient() {
        startActivity(new Intent(this, DeviceListActivity.class));
    }

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.clientButton:
                    openAsClient();
                    break;
                case R.id.serverButton:
                    openAsServer();
                    break;
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
