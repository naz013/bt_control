package com.example.helio.arduino;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Locale;

public class DigitalActivity extends AppCompatActivity implements QueueManager.QueueObserver {

    private static final String TAG = "LedActivity";
    private static final long TIME = 500;
    private static final long SECOND = 1000;
    private static final long MINUTE = SECOND * 60;

    private ActivityDigitalInBinding binding;

    private RequestAction statusAction = new RequestAction() {
        @Override
        public void onAnswerReady(String value) {
            Log.d(TAG, "onAnswerReady: " + value);
            if (value.equalsIgnoreCase("1")) {
                TextView tv = new TextView(DigitalActivity.this);
                tv.setText("Hold");
                binding.contentTable.addView(tv);
            }
        }
    };

    private String getParsedTime(String value) {
        long mills = 0;
        try {
            mills = Long.parseLong(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (mills > 0) {
            long mins = mills / MINUTE;
            long secs = (mills - (MINUTE * mins)) / SECOND;
            long lefs = (mills - (MINUTE * mins) - (SECOND * secs));
            if (mins > 0) {
                return String.format(Locale.getDefault(), "%d.%d.%d", mins, secs, lefs);
            } else {
                return String.format(Locale.getDefault(), "%d.%d", secs, lefs);
            }
        }
        return "0";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        QueueManager.getInstance().addObserver(this);
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
        QueueManager.getInstance().removeObserver(this);
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(ConnectionStatus event) {
        if (event.isConnected()) {
            binding.statusView.setText("Connected");
            sendCommand("z", statusAction);
        } else {
            binding.statusView.setText("Connecting...");
        }
    }

    @Override
    public void onDeQueue() {
        sendCommand("z", statusAction);
    }
}
