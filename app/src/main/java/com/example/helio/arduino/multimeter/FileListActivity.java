package com.example.helio.arduino.multimeter;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
        initActionBar();
        initFileList();
    }

    private void initFileList() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.fileList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        FilesRecyclerAdapter adapter = new FilesRecyclerAdapter(this, getFiles());
        recyclerView.setAdapter(adapter);
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(R.string.exported_data);
        }
    }

    private List<FileItem> getFiles() {
        List<FileItem> list = new ArrayList<>();
        File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/" + Constants.SCREENS_FOLDER + "/" + Constants.SHEETS_FOLDER);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.toString().endsWith(".xls")) {
                        list.add(new FileItem(file.getName(), file.toString()));
                    }
                }
            }
        }
        return list;
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
