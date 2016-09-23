package com.example.helio.arduino.dso;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImagesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);
        initActionBar();
        initList();
    }

    private void initList() {
        RecyclerView mImageList = (RecyclerView) findViewById(R.id.imagesList);
        mImageList.setHasFixedSize(true);
        mImageList.setLayoutManager(new GridLayoutManager(this, 2));
        ImagesRecyclerAdapter mAdapter = new ImagesRecyclerAdapter(this, loadFiles());
        mImageList.setAdapter(mAdapter);
    }

    private List<String> loadFiles() {
        List<String> list = new ArrayList<>();
        File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Constants.SCREENS_FOLDER);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            for (File f : files) {
                if (f.isFile() && f.toString().endsWith(".png")) list.add(f.toString());
            }
        }
        return list;
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.screenshots);
        }
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
