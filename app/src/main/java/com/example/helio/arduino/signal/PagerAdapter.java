package com.example.helio.arduino.signal;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.example.helio.arduino.R;

public class PagerAdapter extends FragmentPagerAdapter {

    private Context mContext;

    public PagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return SignalFragment.newInstance();
        } else {
            return PwmFragment.newInstance();
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
                return mContext.getString(R.string.signal_generator);
            case 1:
                return mContext.getString(R.string.pwm);
        }
        return null;
    }
}
