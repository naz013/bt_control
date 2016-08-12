package com.example.helio.arduino.dso;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.example.helio.arduino.R;
import com.example.helio.arduino.signal.PwmFragment;
import com.example.helio.arduino.signal.SignalFragment;

public class DsoPagerAdapter extends FragmentPagerAdapter {

    private Context mContext;

    public DsoPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return SnapshotFragment.newInstance();
        } else {
            return SnapshotFragment.newInstance();
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mContext.getString(R.string.snapshot);
            case 1:
                return mContext.getString(R.string.auto_refresh);
        }
        return null;
    }
}
