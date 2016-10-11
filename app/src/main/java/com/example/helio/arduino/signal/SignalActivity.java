package com.example.helio.arduino.signal;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.helio.arduino.BuildConfig;
import com.example.helio.arduino.R;
import com.example.helio.arduino.core.BluetoothService;
import com.example.helio.arduino.core.ConnectionEvent;
import com.example.helio.arduino.core.Constants;
import com.example.helio.arduino.core.ControlEvent;
import com.example.helio.arduino.core.ResponseEvent;

import de.greenrobot.event.EventBus;

public class SignalActivity extends AppCompatActivity implements FragmentListener {

    private static final int REQUEST_ENABLE_BT = 15;
    private static final String TAG = "SignalActivity";
    private static final boolean D = BuildConfig.DEBUG;

    private BluetoothAdapter mBtAdapter = null;

    private TextView mBlockView;

    private ViewPager.OnPageChangeListener mPageListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            sendCancelSignal();
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    public void onEvent(ConnectionEvent responseEvent) {
        if (responseEvent.isConnected()) {
            mBlockView.setVisibility(View.GONE);
        } else {
            mBlockView.setVisibility(View.VISIBLE);
        }
    }

    public void onEvent(ResponseEvent responseEvent) {

    }

    private void sendCancelSignal() {
        onAction(Constants.E);
        onAction(Constants.T);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signal);
        initBtAdapter();
        initActionBar();
        initTabNavigation();
        initBlockView();
    }

    private void initTabNavigation() {
        PagerAdapter mPagerAdapter = new PagerAdapter(this, getFragmentManager());
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(mPageListener);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    private void initBlockView() {
        mBlockView = (TextView) findViewById(R.id.blockView);
        mBlockView.setVisibility(View.VISIBLE);
        mBlockView.setOnTouchListener((v, event) -> true);
    }

    private void initBtAdapter() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initActionBar() {
        findViewById(R.id.backButton).setOnClickListener(view -> finish());
    }

    private void requestBtEnable() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    private void checkBtAdapterStatus() {
        if (!mBtAdapter.isEnabled()) {
            requestBtEnable();
        } else {
            startService(new Intent(this, BluetoothService.class));
        }
    }

    private void showBlockView() {
        mBlockView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        checkBtAdapterStatus();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        showBlockView();
        stopService(new Intent(this, BluetoothService.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        sendCancelSignal();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != RESULT_OK) {
            requestBtEnable();
        }
    }

    @Override
    public void onAction(String message) {
        if (D) Log.d(TAG, "onAction: " + message);
        EventBus.getDefault().post(new ControlEvent(message));
    }
}
