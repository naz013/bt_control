package com.example.helio.arduino.transferring;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.example.helio.arduino.R;

public class ChatActivity extends AppCompatActivity {

    private boolean mServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_main);
        mServer = getIntent().getBooleanExtra(getString(R.string.intent_server_key), true);
        initActionBar();

        if (savedInstanceState == null) {
            showChatFragment();
        }
    }

    private void showChatFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        BluetoothChatFragment fragment = BluetoothChatFragment.newInstance(mServer);
        transaction.replace(R.id.sample_content_fragment, fragment);
        transaction.commit();
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle(R.string.data_transferring);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    }

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
