package com.example.helio.arduino;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.SeekBar;

import com.example.helio.arduino.core.ConnectionStatus;
import com.example.helio.arduino.core.QueueItem;
import com.example.helio.arduino.core.QueueManager;
import com.example.helio.arduino.core.RequestAction;
import com.example.helio.arduino.core.StatusRequest;
import com.example.helio.arduino.databinding.ActivityLightSensorBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class LightActivity extends AppCompatActivity implements QueueManager.QueueObserver {

    private static final String TAG = "LedActivity";
    private static final long TIME = 100;

    private ActivityLightSensorBinding binding;

    private boolean auto = true;
    private RequestAction statusAction = new RequestAction() {
        @Override
        public void onAnswerReady(String value) {
            Log.d(TAG, "onAnswerReady: " + value);
            binding.lightLevelView.setText(value);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        QueueManager.getInstance().addObserver(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_light_sensor);
        initActionBar();

        binding.autoButton.setOnClickListener(view -> sendCommand("l", statusAction));
        binding.levelSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sendCommand("c" + i, statusAction);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                auto = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                auto = true;
            }
        });
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setTitle("Light sensor");
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
            sendCommand("k", statusAction);
        } else {
            binding.statusView.setText("Connecting...");
        }
    }

    @Override
    public void onDeQueue() {
        if (auto) sendCommand("k", statusAction);
    }
}
