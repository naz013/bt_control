package com.example.helio.arduino;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.helio.arduino.core.ConnectionStatus;
import com.example.helio.arduino.core.QueueItem;
import com.example.helio.arduino.core.QueueManager;
import com.example.helio.arduino.core.RequestAction;
import com.example.helio.arduino.core.StatusRequest;
import com.example.helio.arduino.databinding.ActivityLedBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Random;

public class LedActivity extends AppCompatActivity {

    private static final String TAG = "LedActivity";
    private static final long TIME = 100;

    private ActivityLedBinding binding;
    private RequestAction blueAction = new RequestAction() {
        @Override
        public void onAnswerReady(String value) {
            Log.d(TAG, "onAnswerReady: " + value);
            binding.blueStatusView.setText(getHumanReadableText(value));
        }
    };
    private RequestAction redAction = new RequestAction() {
        @Override
        public void onAnswerReady(String value) {
            Log.d(TAG, "onAnswerReady: " + value);
            binding.redStatusView.setText(getHumanReadableText(value));
        }
    };
    private RequestAction greenAction = new RequestAction() {
        @Override
        public void onAnswerReady(String value) {
            Log.d(TAG, "onAnswerReady: " + value);
            binding.greenStatusView.setText(getHumanReadableText(value));
        }
    };
    private RequestAction singleLedAction = new RequestAction() {
        @Override
        public void onAnswerReady(String value) {
            Log.d(TAG, "onAnswerReady: " + value);
            binding.greenStatusView.setText(getHumanReadableText(value));
        }
    };

    private String getHumanReadableText(String value) {
        if (value.equals("1")) {
            return "On";
        } else {
            return "Off";
        }
    }

    private boolean isAutoRunning;
    private Handler mAutoHandler = new Handler();
    private Runnable mAutoRunnable = new Runnable() {
        @Override
        public void run() {
            sendCommand();
            mAutoHandler.postDelayed(mAutoRunnable, TIME);
        }
    };

    private void sendCommand() {
        int v = new Random().nextInt(50);
        if (v % 5 == 0) {
            sendCommand("b", blueAction);
        } else if (v % 3 == 0) {
            sendCommand("g", greenAction);
        } else {
            sendCommand("r", redAction);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_led);
        initActionBar();

        binding.blueButton.setOnClickListener(view -> sendCommand("b", blueAction));
        binding.redButton.setOnClickListener(view -> sendCommand("r", redAction));
        binding.greenButton.setOnClickListener(view -> sendCommand("g", greenAction));
        binding.singleButton.setOnClickListener(view -> sendCommand("s", singleLedAction));
        binding.autoButton.setOnClickListener(view -> {
            if (isAutoRunning) {
                mAutoHandler.removeCallbacks(mAutoRunnable);
            } else {
                mAutoHandler.postDelayed(mAutoRunnable, TIME);
            }
            isAutoRunning = !isAutoRunning;
        });
        binding.disableButton.setOnClickListener(view -> {
            mAutoHandler.removeCallbacks(mAutoRunnable);
            isAutoRunning = false;
            sendCommand("0", null);
            checkStatus();
        });
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setTitle("RGB LED");
        }
    }

    private void sendCommand(String command, RequestAction action) {
        QueueManager.getInstance().insert(new QueueItem(command.getBytes(), action));
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().post(new StatusRequest());
        checkStatus();
    }

    private void checkStatus() {
        sendCommand("q", redAction);
        sendCommand("w", greenAction);
        sendCommand("e", blueAction);
        sendCommand("t", singleLedAction);
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
        } else {
            binding.statusView.setText("Connecting...");
        }
    }
}
