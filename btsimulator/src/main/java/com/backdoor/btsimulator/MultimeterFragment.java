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
import com.backdoor.shared.JMessage;

public class MultimeterFragment extends Fragment {

    private static final String KEY_ARG = "key_arg";

    private MultimeterListener mMultimeterListener;

    private Context mContext;
    private String mKey;

    private EditText mVoltageField;
    private EditText mCurrentField;
    private EditText mResistanceField;

    private Button mVoltageButton;
    private Button mResistanceButton;
    private Button mCurrentButton;

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
        initButtons(view);
        initTextFields(view);
    }

    private void initButtons(View view) {
        mVoltageButton = (Button) view.findViewById(R.id.voltageButton);
        mCurrentButton = (Button) view.findViewById(R.id.currentButton);
        mResistanceButton = (Button) view.findViewById(R.id.ohmButton);
        mResistanceButton.setOnClickListener(mListener);
        mCurrentButton.setOnClickListener(mListener);
        mVoltageButton.setOnClickListener(mListener);
    }

    private void initTextFields(View view) {
        TextView meterField = (TextView) view.findViewById(R.id.meterField);
        JMessage jMessage = new JMessage(mKey);
        if (jMessage.hasFlag()) {
            mKey = jMessage.getFlag();
            meterField.setText(mKey);
            selectButton();
        } else {
            meterField.setText(R.string.no_flag);
        }
        mVoltageField = (EditText) view.findViewById(R.id.voltageField);
        mCurrentField = (EditText) view.findViewById(R.id.currentField);
        mResistanceField = (EditText) view.findViewById(R.id.ohmField);
    }

    private void selectButton() {
        if (mKey.matches(Constants.R)) {
            mResistanceButton.setSelected(true);
            mResistanceButton.setEnabled(true);
        } else if (mKey.matches(Constants.I)) {
            mCurrentButton.setSelected(true);
            mCurrentButton.setEnabled(true);
        } else if (mKey.matches(Constants.V)) {
            mVoltageButton.setSelected(true);
            mVoltageButton.setEnabled(true);
        }
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
        String resistanceString = mResistanceField.getText().toString().trim();
        if (resistanceString.matches("")) {
            showToast(mContext.getString(R.string.empty_resistance_field));
            return;
        }
        mResistanceField.setText("");
        String msg = new JMessage().putResistance(resistanceString).asString();
        sendMessage(msg.getBytes());
    }

    private void sendCurrent() {
        String currentString = mCurrentField.getText().toString().trim();
        if (currentString.matches("")) {
            showToast(mContext.getString(R.string.empty_current_field));
            return;
        }
        mCurrentField.setText("");
        String msg = new JMessage().putCurrent(currentString).asString();
        sendMessage(msg.getBytes());
    }

    private void sendVoltage() {
        String voltageString = mVoltageField.getText().toString().trim();
        if (voltageString.matches("")) {
            showToast(mContext.getString(R.string.empty_voltage_field));
            return;
        }
        mVoltageField.setText("");
        String msg = new JMessage().putVoltage(voltageString).asString();
        sendMessage(msg.getBytes());
    }

    private void sendMessage(byte[] bundle) {
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
