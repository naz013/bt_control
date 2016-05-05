package com.example.helio.arduino.dso;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.backdoor.shared.Constants;
import com.example.helio.arduino.R;

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
                list.add(f.toString());
            }
        }
        return list;
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        if (toolbar != null) {
            toolbar.setTitle(R.string.screenshots);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
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
