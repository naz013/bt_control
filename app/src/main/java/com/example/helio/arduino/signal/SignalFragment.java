package com.example.helio.arduino.signal;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.Constants;

public class SignalFragment extends Fragment {

    private FragmentListener mFragmentListener;

    private Button mGenerateButton;
    private Button mTerminateButton;
    private RadioGroup waveGroup;
    private RadioGroup freqGroup;
    private EditText mFrequencyField;
    private TextInputLayout mFreqLabel;

    private boolean isGenerating;

    private final View.OnClickListener mListener = v -> {
        switch (v.getId()) {
            case R.id.generateButton:
                sendSignal();
                break;
            case R.id.terminateButton:
                sendTerminateMessage();
                break;
        }
    };

    private RadioGroup.OnCheckedChangeListener mCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            if (i == R.id.hzCheck) {
                setMaxLength(8);
            } else if (i == R.id.khzCheck) {
                setMaxLength(5);
            } else if (i == R.id.mhzCheck) {
                setMaxLength(2);
            }
            if (isGenerating) return;
            checkFrequency();
        }
    };

    private void setMaxLength(int length) {
        String string = mFrequencyField.getText().toString().trim();
        if (string.length() > length) {
            mFrequencyField.setText(string.substring(0, length - 1));
        }
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(length);
        mFrequencyField.setFilters(FilterArray);
        mFrequencyField.setSelection(mFrequencyField.getText().length());
    }

    public SignalFragment() {
    }

    public static SignalFragment newInstance() {
        return new SignalFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signal, container, false);
        initViews(view);
        initButtons(view);
        return view;
    }

    private void initViews(View view) {
        waveGroup = (RadioGroup) view.findViewById(R.id.waveGroup);
        freqGroup = (RadioGroup) view.findViewById(R.id.freqGroup);
        freqGroup.setOnCheckedChangeListener(mCheckedChangeListener);

        mFreqLabel = (TextInputLayout) view.findViewById(R.id.freqInput);
        mFreqLabel.setHintEnabled(false);
        mFrequencyField = (EditText) view.findViewById(R.id.freqField);
        mFrequencyField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isGenerating) return;
                if (s.length() == 0) {
                    mGenerateButton.setEnabled(false);
                } else {
                    mGenerateButton.setEnabled(true);
                }
                checkFrequency();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void checkFrequency() {
        if (TextUtils.isEmpty(mFrequencyField.getText().toString().trim())) {
            mFreqLabel.setErrorEnabled(true);
            mFreqLabel.setError(getString(R.string.must_be_not_empty));
            return;
        } else {
            mFreqLabel.setErrorEnabled(false);
            mFreqLabel.setError("");
        }

        if (getFrequency() > Constants.MAX_HZ) {
            mFreqLabel.setErrorEnabled(true);
            mFreqLabel.setError(getString(R.string.max_frequency4));
            mGenerateButton.setEnabled(false);
        } else {
            mFreqLabel.setErrorEnabled(false);
            mFreqLabel.setError("");
            mGenerateButton.setEnabled(true);
        }
    }

    private long getFrequency() {
        long freq = Integer.parseInt(mFrequencyField.getText().toString().trim());
        int id = freqGroup.getCheckedRadioButtonId();
        long eval = 1;
        if (id == R.id.hzCheck) {
            eval = Constants.HZ;
        } else if (id == R.id.khzCheck) {
            eval = Constants.kHZ;
        } else if (id == R.id.mhzCheck) {
            eval = Constants.MHZ;
        }
        return freq * eval;
    }

    private void initButtons(View view) {
        mGenerateButton = (Button) view.findViewById(R.id.generateButton);
        mGenerateButton.setOnClickListener(mListener);
        mTerminateButton = (Button) view.findViewById(R.id.terminateButton);
        mTerminateButton.setOnClickListener(mListener);
        mGenerateButton.setEnabled(false);
        mTerminateButton.setEnabled(false);
    }

    private void sendTerminateMessage() {
        isGenerating = false;
        String msg = Constants.T;
        mGenerateButton.setEnabled(true);
        mTerminateButton.setEnabled(false);
        if (mFragmentListener != null) {
            mFragmentListener.onAction(msg);
        }
    }

    private void sendSignal() {
        String freqString = mFrequencyField.getText().toString().trim();
        if (TextUtils.isEmpty(freqString)) {
            showToast(getString(R.string.empty_frequency));
            mFrequencyField.setText("");
            return;
        }
        long frequency = Long.parseLong(freqString);
        int wave = 1;
        if (waveGroup.getCheckedRadioButtonId() == R.id.sineCheck) {
            wave = 2;
        }
        int id = freqGroup.getCheckedRadioButtonId();
        long eval = 1;
        if (id == R.id.hzCheck) {
            eval = Constants.HZ;
        } else if (id == R.id.khzCheck) {
            eval = Constants.kHZ;
        } else if (id == R.id.mhzCheck) {
            eval = Constants.MHZ;
        }
        frequency = frequency * eval;
        if (frequency > Constants.MAX_HZ) {
            showToast(getString(R.string.max_frequency4));
            mFrequencyField.setText("");
            return;
        }
        String msg = Constants.G + ";w:" + wave + ";f:" + frequency;
        Log.d("TAG", "sendSignal: " + msg);
        mGenerateButton.setEnabled(false);
        mTerminateButton.setEnabled(true);
        isGenerating = true;
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof FragmentListener && mFragmentListener == null) {
            mFragmentListener = (FragmentListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentListener = null;
    }
}
