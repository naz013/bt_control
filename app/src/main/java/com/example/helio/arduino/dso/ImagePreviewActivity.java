package com.example.helio.arduino.dso;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImagePreviewActivity extends AppCompatActivity {

    private ActionBar actionBar;
    private String mPhotoPath;
    private List<String> mList;
    private ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            File file = new File(mList.get(position));
            actionBar.setTitle(file.getName());
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        mPhotoPath = getIntent().getStringExtra(getString(R.string.image_path_intent));
        initActionBar();
        initImage();
    }

    private void initActionBar() {
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.screenshots);
        }
    }

    private void initImage() {
        ImagesPagerAdapter adapter = new ImagesPagerAdapter(getFragmentManager(), loadFiles());
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pagerView);
        mViewPager.setOnPageChangeListener(mPageChangeListener);
        mViewPager.setAdapter(adapter);
        int pos = getPhotoPosition();
        if (pos >= 0) {
            mViewPager.setCurrentItem(pos);
            File file = new File(mList.get(pos));
            actionBar.setTitle(file.getName());
        }
    }

    private int getPhotoPosition() {
        if (mList.size() > 0) {
            return mList.indexOf(mPhotoPath);
        } else {
            return 0;
        }
    }

    private List<String> loadFiles() {
        mList = new ArrayList<>();
        File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Constants.SCREENS_FOLDER);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            for (File f : files) {
                if (f.isFile() && f.toString().endsWith(".png")) mList.add(f.toString());
            }
        }
        return mList;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
