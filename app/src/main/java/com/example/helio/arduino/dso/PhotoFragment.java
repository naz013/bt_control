package com.example.helio.arduino.dso;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.helio.arduino.R;
import com.squareup.picasso.Picasso;

import java.io.File;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_path";
    private String mPath;

    private Context mContext;

    public PhotoFragment() {
    }

    public static PhotoFragment newInstance(String filePath) {
        PhotoFragment fragment = new PhotoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SECTION_NUMBER, filePath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mContext == null) {
            this.mContext = context;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mContext == null) {
            this.mContext = activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mPath = args.getString(ARG_SECTION_NUMBER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image, container, false);
        initImage(view);
        return view;
    }

    private void initImage(View view) {
        ImageView mImageView = (ImageView) view.findViewById(R.id.iv_photo);
        File image = new File(mPath);
        if (image.exists()) {
            Picasso.with(mImageView.getContext())
                    .load(image)
                    .into(mImageView);

            PhotoViewAttacher mAttacher = new PhotoViewAttacher(mImageView);
            mAttacher.setScaleType(ImageView.ScaleType.CENTER);
        }
    }
}
