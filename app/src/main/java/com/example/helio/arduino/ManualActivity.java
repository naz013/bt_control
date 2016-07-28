package com.example.helio.arduino;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;

public class ManualActivity extends AppCompatActivity {

    private ProgressDialog mProgress;

    private OnErrorListener mErrorListener = t -> {
        Toast.makeText(ManualActivity.this, R.string.failed_to_open_file, Toast.LENGTH_SHORT).show();
        finish();
    };
    private OnLoadCompleteListener mLoadListener = nbPages -> {
        hideProgress();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);
        initActionBar();
        loadPdf();
    }

    private void showProgress() {
        mProgress = ProgressDialog.show(this, null, getString(R.string.please_wait), false, false);
    }

    private void hideProgress() {
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
    }

    private void loadPdf() {
        PDFView pdfView = (PDFView) findViewById(R.id.pdfView);
        showProgress();
        pdfView.fromAsset("file.pdf")
                .pages(0, 1, 2, 3, 4, 5) // all pages are displayed by default
                .enableSwipe(true)
                .enableDoubletap(true)
                .swipeVertical(false)
                .defaultPage(0)
                .showMinimap(true)
                .onLoad(mLoadListener)
                .onError(mErrorListener)
                .enableAnnotationRendering(false)
                .password(null)
                .showPageWithAnimation(true)
                .load();
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setTitle(R.string.user_manual);
        }
    }
}
