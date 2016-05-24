package com.example.helio.arduino.signal;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.Constants;

public class PwmFragment extends Fragment {

    private FragmentListener mFragmentListener;

    private Button mGenerate;
    private Button mTerminate;
    private Spinner mFrequencySelector;
    private EditText mFrequencyField;
    private EditText mDutyField;

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.generateButton:
                    sendSignal();
                    break;
                case R.id.terminateButton:
                    sendTerminateMessage();
                    break;
            }
        }
    };

    public PwmFragment() {
        // Required empty public constructor
    }

    public static PwmFragment newInstance() {
        PwmFragment fragment = new PwmFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pwm, container, false);
        initViews(view);
        initButtons(view);
        return view;
    }

    private void initViews(View view) {
        mFrequencyField = (EditText) view.findViewById(R.id.freqField);
        mFrequencyField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkData(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mFrequencySelector = (Spinner) view.findViewById(R.id.freqSelector);
        mDutyField = (EditText) view.findViewById(R.id.cycleField);
        mDutyField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkData(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void checkData(CharSequence s) {
        if (s.length() == 0) {
            mGenerate.setEnabled(false);
        } else {
            mGenerate.setEnabled(true);
        }
    }

    private void initButtons(View view) {
        mGenerate = (Button) view.findViewById(R.id.generateButton);
        mGenerate.setOnClickListener(mListener);
        mTerminate = (Button) view.findViewById(R.id.terminateButton);
        mTerminate.setOnClickListener(mListener);
        mGenerate.setEnabled(false);
    }

    private void sendTerminateMessage() {
        String msg = Constants.E;
        if (mFragmentListener != null) {
            mFragmentListener.onAction(msg);
        }
    }

    private void sendSignal() {
        String freqString = mFrequencyField.getText().toString().trim();
        if (freqString.matches("")) {
            showToast(getString(R.string.empty_frequency));
            mFrequencyField.setText("0");
            return;
        }
        String dutyString = mDutyField.getText().toString().trim();
        if (dutyString.matches("")) {
            showToast(getString(R.string.must_be_not_empty));
            mDutyField.setText("0");
            return;
        }
        int percent = Integer.parseInt(dutyString);
        if (percent > 100 || percent < 0) {
            showToast(getString(R.string.must_be_less_than));
            mDutyField.setText("0");
            return;
        }
        long frequency = Long.parseLong(freqString);
        int multi = mFrequencySelector.getSelectedItemPosition();
        if (multi == 0) {
            multi = multi * Constants.HZ;
        } else if (multi == 1) {
            multi = multi * Constants.kHZ;
        } else if (multi == 2) {
            multi = multi * Constants.MHZ;
        }
        frequency = frequency * multi;
        if (frequency > Constants.MAX_HZ) {
            showToast(getString(R.string.max_frequency4));
            mFrequencyField.setText("0");
            return;
        }

        String msg = Constants.P + ";d:" + percent + ";f:" + frequency;
        if (mFragmentListener != null) {
            mFragmentListener.onAction(msg);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentListener) {
            mFragmentListener = (FragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentListener = null;
    }
}
