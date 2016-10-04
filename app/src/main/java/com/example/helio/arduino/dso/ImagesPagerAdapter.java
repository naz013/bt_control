package com.example.helio.arduino.dso;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import java.util.List;

class ImagesPagerAdapter extends FragmentPagerAdapter {

    private final List<String> mPathList;

    ImagesPagerAdapter(FragmentManager fm, List<String> pathList) {
        super(fm);
        this.mPathList = pathList;
    }

    @Override
    public Fragment getItem(int position) {
        return PhotoFragment.newInstance(mPathList.get(position));
    }

    @Override
    public int getCount() {
        return mPathList.size();
    }
}
