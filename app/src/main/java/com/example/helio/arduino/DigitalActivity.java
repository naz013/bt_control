package com.example.helio.arduino;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.example.helio.arduino.core.ConnectionStatus;
import com.example.helio.arduino.core.QueueItem;
import com.example.helio.arduino.core.QueueManager;
import com.example.helio.arduino.core.RequestAction;
import com.example.helio.arduino.core.StatusRequest;
import com.example.helio.arduino.databinding.ActivityDigitalInBinding;
import com.example.helio.arduino.databinding.ActivityLedBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Random;

public class DigitalActivity extends AppCompatActivity {

    private static final String TAG = "LedActivity";
    private static final long TIME = 100;

    private ActivityDigitalInBinding binding;

    private RequestAction statusAction = new RequestAction() {
        @Override
        public void onAnswerReady(String value) {
            Log.d(TAG, "onAnswerReady: " + value);
            TextView tv = new TextView(DigitalActivity.this);
            tv.setText(value);
            binding.contentTable.addView(tv);
        }
    };

    private Handler mAutoHandler = new Handler();
    private Runnable mAutoRunnable = new Runnable() {
        @Override
        public void run() {
            sendCommand("z", statusAction);
            mAutoHandler.postDelayed(mAutoRunnable, TIME);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_digital_in);
        initActionBar();

        binding.clearButton.setOnClickListener(view -> binding.contentTable.removeAllViewsInLayout());
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setTitle("Digital In");
        }
    }

    private void sendCommand(String command, RequestAction action) {
        QueueManager.getInstance().insert(new QueueItem(command.getBytes(), action));
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().post(new StatusRequest());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAutoHandler.removeCallbacks(mAutoRunnable);
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(ConnectionStatus event) {
        if (event.isConnected()) {
            binding.statusView.setText("Connected");
            mAutoHandler.postDelayed(mAutoRunnable, TIME);
        } else {
            binding.statusView.setText("Connecting...");
        }
    }
}
