package com.example.helio.arduino.dso;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.example.helio.arduino.core.DsoEvent;
import com.example.helio.arduino.core.ResponseEvent;
import com.example.helio.arduino.signal.FragmentListener;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class DsoActivity extends AppCompatActivity implements FragmentListener {

    private static final int REQUEST_ENABLE_BT = 3;
    private static final String TAG = "DsoActivity";
    private static final boolean D = BuildConfig.DEBUG;
    private static final int SNAPSHOT = 0;
    private static final int AUTO_REFRESH = 1;
    private static final int REAL_TIME = 2;
    private static final int NONE = -1;

    private TextView mBlockView;

    private BluetoothAdapter mBtAdapter = null;

    private static Activity activity;
    private int mEnabledAction;
    private long mTime;

    private DsoPagerAdapter mPagerAdapter;

    private int mSelectedPage;
    private Handler mHandler = new Handler();
    private Runnable mAutoRunnable = new Runnable() {
        @Override
        public void run() {
            loadTestData();
            mHandler.removeCallbacks(mAutoRunnable);
            mHandler.postDelayed(mAutoRunnable, 15);
        }
    };

    public void onEvent(DsoEvent dsoEvent) {
        try {
            readDso(dsoEvent.getArray());
        } catch (NumberFormatException e) {

        }
    }

    public void onEvent(ResponseEvent event) {
        try {
            readDso((String) event.getMsg().obj);
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
            onAction(Constants.S);
            mSelectedPage = position;
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
        mPagerAdapter = new DsoPagerAdapter(this, getFragmentManager());
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
        mBlockView.setVisibility(View.VISIBLE);
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

    private void readDso(short[] array) {
        List<Float> mYVals = new ArrayList<>();
        List<Float> mXVals = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            float x = ((float) i / ((float) array.length / ChartView.MAX_X)) * (1f / 1000f);
            short yVal = array[i];
            float y = (float) yVal / 1000f;
            mYVals.add(y);
            mXVals.add(x);
        }

    }

    private void readDso(String data) {
        List<Float> mYVals = new ArrayList<>();
        List<Float> mXVals = new ArrayList<>();
        if (data.startsWith(Constants.rY)) {
            String yArray = data.replace(Constants.rY, "");
            String[] parts = yArray.split(Constants.COMMA);
            mYVals.clear();
            for (int i = 0; i < parts.length; i++) {
                String yVal = parts[i];
                float x = ((float) i / ((float) parts.length / ChartView.MAX_X)) * (1f / 1000f);
                if (TextUtils.isEmpty(yVal.trim())) continue;
                float y = Float.parseFloat(yVal.trim());
                mYVals.add(y);
                mXVals.add(x);
            }
        }
        sendDataToFragment(mXVals, mYVals);
    }

    private void sendDataToFragment(List<Float> mXVals, List<Float> mYVals) {
        Fragment fragment = mPagerAdapter.getFragment(mSelectedPage);
        Log.d(TAG, "sendDataToFragment: " + fragment);
        switch (mEnabledAction) {
            case SNAPSHOT:
                if (fragment instanceof SnapshotFragment) {
                    ((SnapshotFragment) fragment).setData(mXVals, mYVals);
                }
                break;
            case AUTO_REFRESH:
                if (fragment instanceof AutoRefreshFragment) {
                    ((AutoRefreshFragment) fragment).setData(mXVals, mYVals);
                }
                break;
        }
    }

    private float step = 0.05f;
    private float corrector = 0.001f;

    private void loadTestData() {
        step += corrector;
        if (step > 0.23 && step <= 0.25) corrector = -0.001f;
        else if (step >= 0.05 && step < 0.06) corrector = 0.001f;
        float y = 0f;
        int testCount = 1000;
        List<Float> mYVals = new ArrayList<>();
        List<Float> mXVals = new ArrayList<>();
        for (int i = 0; i < testCount; i++) {
            float x = ((float) i / ((float) testCount / ChartView.MAX_X)) * (1f / 1000f);
            y += step;
            if (Math.round(y) == 16f) step = -step;
            else if (Math.round(y) == -16.0f) step = Math.abs(step);
            mYVals.add(y);
            mXVals.add(x);
        }
        sendDataToFragment(mXVals, mYVals);
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
        mTime = System.currentTimeMillis();
        EventBus.getDefault().post(new ControlEvent(message));
        showToast(getString(R.string.request_sent));
        if (message.matches(Constants.C)) {
            mEnabledAction = SNAPSHOT;
        } else if (message.matches(Constants.A)) {
            mEnabledAction = AUTO_REFRESH;
        } else if (message.matches(Constants.L)) {
            mEnabledAction = REAL_TIME;
        } else if (message.matches(Constants.S)) {
            mEnabledAction = NONE;
            mHandler.removeCallbacks(mAutoRunnable);
        }
    }
}
