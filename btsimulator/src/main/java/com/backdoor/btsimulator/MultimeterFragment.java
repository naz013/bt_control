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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.backdoor.shared.Constants;

public class MultimeterFragment extends Fragment {

    private static final String KEY_ARG = "key_arg";

    private MultimeterListener mMultimeterListener;

    private Context mContext;
    private String mKey;

    private TextView meterField;
    private EditText voltageField;
    private EditText currentField;
    private EditText ohmField;

    public MultimeterFragment() {

    }

    public static MultimeterFragment newInstance(Message message) {
        MultimeterFragment fragment = new MultimeterFragment();
        Bundle bundle = new Bundle();
        byte[] writeBuf = (byte[]) message.obj;
        String key = new String(writeBuf);
        bundle.putString(KEY_ARG, key);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getBundle();
    }

    private void getBundle() {
        if (getArguments() != null) {
            mKey = getArguments().getString(KEY_ARG);
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
        return inflater.inflate(R.layout.fragment_multimeter, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        meterField = (TextView) view.findViewById(R.id.meterField);
        meterField.setText(mKey);
        voltageField = (EditText) view.findViewById(R.id.voltageField);
        currentField = (EditText) view.findViewById(R.id.currentField);
        ohmField = (EditText) view.findViewById(R.id.ohmField);
        Button voltageButton = (Button) view.findViewById(R.id.voltageButton);
        Button currentButton = (Button) view.findViewById(R.id.currentButton);
        Button ohmButton = (Button) view.findViewById(R.id.ohmButton);
        ohmButton.setOnClickListener(mListener);
        currentButton.setOnClickListener(mListener);
        voltageButton.setOnClickListener(mListener);
    }

    private View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.voltageButton:
                    sendVoltage();
                    break;
                case R.id.currentButton:
                    sendCurrent();
                    break;
                case R.id.ohmButton:
                    sendResistance();
                    break;
            }
        }
    };

    private void sendResistance() {
        if (!mKey.matches(Constants.R)) {
            showToast("You need to send another data");
            return;
        }
        String resistanceString = ohmField.getText().toString().trim();
        if (resistanceString.matches("")) {
            showToast(mContext.getString(R.string.empty_resistance_field));
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(Constants.R, resistanceString);
        sendMessage(bundle);
    }

    private void sendCurrent() {
        if (!mKey.matches(Constants.I)) {
            showToast("You need to send another data");
            return;
        }
        String currentString = currentField.getText().toString().trim();
        if (currentString.matches("")) {
            showToast(mContext.getString(R.string.empty_current_field));
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(Constants.I, currentString);
        sendMessage(bundle);
    }

    private void sendVoltage() {
        if (!mKey.matches(Constants.V)) {
            showToast("You need to send another data");
            return;
        }
        String voltageString = voltageField.getText().toString().trim();
        if (voltageString.matches("")) {
            showToast(mContext.getString(R.string.empty_voltage_field));
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(Constants.V, voltageString);
        sendMessage(bundle);
    }

    private void sendMessage(Bundle bundle) {
        if (mMultimeterListener != null) {
            mMultimeterListener.obtainData(bundle);
        }
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
