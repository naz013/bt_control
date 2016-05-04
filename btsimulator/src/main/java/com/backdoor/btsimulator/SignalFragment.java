package com.backdoor.btsimulator;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.backdoor.shared.JMessage;
import com.backdoor.shared.SignalObject;

public class SignalFragment extends Fragment {

    private static final String ARG = "arg";

    private MultimeterListener mMultimeterListener;
    private Context mContext;
    private String data;

    public SignalFragment() {

    }

    public static SignalFragment newInstance(Message msg) {
        SignalFragment fragment = new SignalFragment();
        byte[] readBuff = (byte[]) msg.obj;
        String data = new String(readBuff, 0, msg.arg1);
        Bundle bundle = new Bundle();
        bundle.putString(ARG, data);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            data = getArguments().getString(ARG);
        }
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
        return inflater.inflate(R.layout.fragment_signal, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        TextView meterField = (TextView) view.findViewById(R.id.meterField);
        if (data != null) {
            JMessage jMessage = new JMessage(data);
            if (jMessage.hasSignal()) {
                SignalObject object = jMessage.getSignal();
                meterField.setText("S " + object.getWaveType() + ", freq " +
                        object.getFrequency() + ", freqM " + object.getFrequencyModifier() +
                        ", magn " + object.getMagnitude());
            }
        }
    }
}
