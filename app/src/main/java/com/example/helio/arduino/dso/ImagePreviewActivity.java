package com.example.helio.arduino.dso;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.backdoor.shared.Constants;
import com.example.helio.arduino.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImagePreviewActivity extends AppCompatActivity {

    private String mPhotoPath;
    private List<String> mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        mPhotoPath = getIntent().getStringExtra(getString(R.string.image_path_intent));
        initImage();
    }

    private void initImage() {
        ImagesPagerAdapter adapter = new ImagesPagerAdapter(this, getFragmentManager(), loadFiles());
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pagerView);
        mViewPager.setAdapter(adapter);

        int pos = getPhotoPosition();
        if (pos >= 0) {
            mViewPager.setCurrentItem(pos);
        }
    }

    private int getPhotoPosition() {
        if (mList.size() > 0) {
            return mList.indexOf(mPhotoPath);
        } else return 0;
    }

    private List<String> loadFiles() {
        mList = new ArrayList<>();
        File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Constants.SCREENS_FOLDER);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            for (File f : files)
                mList.add(f.toString());
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
