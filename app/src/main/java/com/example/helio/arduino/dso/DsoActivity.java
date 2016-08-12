package com.example.helio.arduino.dso;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helio.arduino.BuildConfig;
import com.example.helio.arduino.R;
import com.example.helio.arduino.SettingsActivity;
import com.example.helio.arduino.core.BluetoothService;
import com.example.helio.arduino.core.ConnectionEvent;
import com.example.helio.arduino.core.Constants;
import com.example.helio.arduino.core.ControlEvent;
import com.example.helio.arduino.core.ResponseEvent;
import com.example.helio.arduino.signal.FragmentListener;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class DsoActivity extends AppCompatActivity implements FragmentListener {

    private static final int REQUEST_ENABLE_BT = 3;
    private static final String TAG = "DsoActivity";
    private static final boolean D = BuildConfig.DEBUG;

    private TextView mBlockView;

    private BluetoothAdapter mBtAdapter = null;

    private static Activity activity;
    private boolean mXReceived;

    public void onEvent(ResponseEvent responseEvent) {
        try {
            readDso(responseEvent.getMsg());
        } catch (NumberFormatException e) {

        }
    }

    public void onEvent(ConnectionEvent responseEvent) {
        if (responseEvent.isConnected()) {
            mBlockView.setVisibility(View.GONE);
        } else {
            mBlockView.setVisibility(View.VISIBLE);
        }
    }

    private ViewPager.OnPageChangeListener mPageListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_dso);
        initBtAdapter();
        initActionBar();
        initTabNavigation();
        initBlockView();
    }

    private void initTabNavigation() {
        DsoPagerAdapter mPagerAdapter = new DsoPagerAdapter(this, getFragmentManager());
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(mPageListener);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    public static Activity getActivity() {
        return activity;
    }

    private void initBlockView() {
        mBlockView = (TextView) findViewById(R.id.blockView);
//        mBlockView.setVisibility(View.VISIBLE);
        mBlockView.setOnTouchListener((v, event) -> true);
    }

    private void initBtAdapter() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.dso);
        }
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

    private void sendCancelMessage() {
        String msg = Constants.S;
        EventBus.getDefault().post(new ControlEvent(msg));
        showToast(getString(R.string.request_sent));
    }

    private void readDso(Message msg) throws NumberFormatException {
        String data = (String) msg.obj;
        String[] arrays = data.split(";");
        if (arrays[0].startsWith("����x:")) {
            String xArray = arrays[0].replace("����x:", "");
            String[] parts = xArray.split(Constants.COMMA);
            List<Float> mXVals = new ArrayList<>();
            for (String xVal : parts) {
                if (TextUtils.isEmpty(xVal.trim())) continue;
                float x = Float.parseFloat(xVal.trim());
                mXVals.add(x / 1000f);
            }
            mXReceived = true;
        } else if (arrays[0].startsWith(Constants.rX)) {
            String xArray = arrays[0].replace(Constants.rX, "");
            String[] parts = xArray.split(Constants.COMMA);
            List<Float> mXVals = new ArrayList<>();
            for (String xVal : parts) {
                if (TextUtils.isEmpty(xVal.trim())) continue;
                float x = Float.parseFloat(xVal.trim());
                mXVals.add(x / 1000f);
            }
            mXReceived = true;
        }
        if (arrays[1].startsWith(Constants.rY)) {
            String yArray = arrays[1].replace(Constants.rY, "");
            String[] parts = yArray.split(Constants.COMMA);
            List<Float> mYVals = new ArrayList<>();
            for (String yVal : parts) {
                if (TextUtils.isEmpty(yVal.trim())) continue;
                float y = Float.parseFloat(yVal.trim());
                mYVals.add(y);
            }
            if (mXReceived) {
                mXReceived = false;
                // TODO: 12.08.2016 transfer data
            }
        }
    }

    private void showBlockView() {
        mBlockView.setVisibility(View.VISIBLE);
    }

    private void closeScreen() {
        sendCancelMessage();
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                closeScreen();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != RESULT_OK) {
            requestBtEnable();
        } else {
            startService(new Intent(this, BluetoothService.class));
        }
    }

    @Override
    public void onBackPressed() {
        closeScreen();
    }

    @Override
    public void onAction(String message) {
        if (D) Log.d(TAG, "onAction: " + message);
        EventBus.getDefault().post(new ControlEvent(message));
        showToast(getString(R.string.request_sent));
    }
}
