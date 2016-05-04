package com.backdoor.btsimulator;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.backdoor.shared.JMessage;

import java.util.Random;

public class DSOFragment extends Fragment {

    private MultimeterListener mMultimeterListener;
    private Context mContext;
    private EditText yField;

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            float randomY = new Random().nextFloat() * (180f - 1f) + 1f;
            sendYValue("" + randomY);
            mHandler.postDelayed(mRunnable, 1000);
        }
    };

    public DSOFragment() {

    }

    public static DSOFragment newInstance() {
        return new DSOFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mContext == null) {
            this.mContext = context;
        }
        if (mMultimeterListener == null) {
            try {
                mMultimeterListener = (MultimeterListener) context;
            } catch (ClassCastException e) {
                throw new ClassCastException("Activity must implement MultimeterListener.");
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mContext == null) {
            this.mContext = activity;
        }
        if (mMultimeterListener == null) {
            try {
                mMultimeterListener = (MultimeterListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException("Activity must implement MultimeterListener.");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dso, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        yField = (EditText) view.findViewById(R.id.yField);
        Button yButton = (Button) view.findViewById(R.id.yButton);
        yButton.setOnClickListener(mListener);
        ((CheckBox) view.findViewById(R.id.randomCheck)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    yField.setEnabled(false);
                    mHandler.postDelayed(mRunnable, 1000);
                } else {
                    yField.setEnabled(true);
                    mHandler.removeCallbacks(mRunnable);
                }
            }
        });
    }

    private View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String yString = yField.getText().toString().trim();
            if (yString.matches("")) {
                showToast(mContext.getString(R.string.empty_y_field));
                return;
            }
            yField.setText("");
            sendYValue(yString);
        }
    };

    private void sendYValue(String value) {
        sendMessage(value);
    }

    private void sendMessage(String message) {
        if (mMultimeterListener != null) {
            String msg = new JMessage().putYValue(message).asString();
            mMultimeterListener.obtainData(msg.getBytes());
        }
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRunnable);
    }
}
