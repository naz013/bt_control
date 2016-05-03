package com.backdoor.btsimulator;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.backdoor.shared.JMessage;

public class DSOFragment extends Fragment {

    private MultimeterListener mMultimeterListener;
    private Context mContext;
    private EditText yField;

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
    }

    private View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendYValue();
        }
    };

    private void sendYValue() {
        String yString = yField.getText().toString().trim();
        if (yString.matches("")) {
            showToast(mContext.getString(R.string.empty_y_field));
            return;
        }
        sendMessage(yString);
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
}
