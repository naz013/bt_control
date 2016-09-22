package com.example.helio.arduino.dso;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.example.helio.arduino.R;

class DsoPagerAdapter extends FragmentPagerAdapter {

    private Context mContext;
    private Fragment[] fragment = new Fragment[2];

    DsoPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.mContext = context;
    }

    Fragment getFragment(int position) {
        return fragment[position];
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 1) {
            fragment[position] = SnapshotFragment.newInstance();
        } else {
            fragment[position] = AutoRefreshFragment.newInstance();
        }
        return fragment[position];
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 1:
                return mContext.getString(R.string.snapshot);
            case 0:
                return mContext.getString(R.string.auto_refresh);
        }
        return null;
    }
}
