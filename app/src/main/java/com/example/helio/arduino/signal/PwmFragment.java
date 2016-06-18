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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.Constants;

public class PwmFragment extends Fragment {

    private FragmentListener mFragmentListener;

    private TextView mGenerateButton;
    private TextView mTerminateButton;
    private Spinner mFrequencySelector;
    private EditText mFrequencyField;
    private EditText mDutyField;
    private TextInputLayout mCycleLabel;
    private TextInputLayout mFreqLabel;

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

    private AdapterView.OnItemSelectedListener mItemSelectListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (position == 0) {
                setMaxLength(8);
            } else if (position == 1) {
                setMaxLength(5);
            } else if (position == 2) {
                setMaxLength(2);
            }
            checkFrequency();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

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
        mFreqLabel = (TextInputLayout) view.findViewById(R.id.freqInput);
        mFreqLabel.setHintEnabled(false);
        mFrequencyField = (EditText) view.findViewById(R.id.freqField);
        mFrequencyField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkData();
                checkFrequency();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mFrequencySelector = (Spinner) view.findViewById(R.id.freqSelector);
        mFrequencySelector.setOnItemSelectedListener(mItemSelectListener);

        mCycleLabel = (TextInputLayout) view.findViewById(R.id.cycleInput);
        mCycleLabel.setHintEnabled(false);
        mDutyField = (EditText) view.findViewById(R.id.cycleField);
        mDutyField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkData();
                checkDuty();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        checkDuty();
    }

    private void checkData() {
        if (mFrequencyField.getText().length() > 0 && mDutyField.getText().length() > 0) {
            mGenerateButton.setEnabled(true);
        } else {
            mGenerateButton.setEnabled(false);
        }
    }

    private void checkDuty() {
        mCycleLabel.refreshDrawableState();
        if (TextUtils.isEmpty(mDutyField.getText().toString().trim())){
            mCycleLabel.setErrorEnabled(true);
            mCycleLabel.setError(getString(R.string.must_be_not_empty));
            return;
        } else {
            mCycleLabel.setErrorEnabled(false);
            mCycleLabel.setError("");
        }
        int perc = Integer.parseInt(mDutyField.getText().toString().trim());
        if (perc > 100) {
            mCycleLabel.setErrorEnabled(true);
            mCycleLabel.setError(getString(R.string.must_be_less_than));
            mGenerateButton.setEnabled(false);
        } else {
            mCycleLabel.setErrorEnabled(false);
            mCycleLabel.setError("");
            mGenerateButton.setEnabled(true);
        }
    }

    private void checkFrequency() {
        if (TextUtils.isEmpty(mFrequencyField.getText().toString().trim())){
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
        int multi = mFrequencySelector.getSelectedItemPosition();
        long eval = 1;
        if (multi == 0) {
            eval = Constants.HZ;
        } else if (multi == 1) {
            eval = Constants.kHZ;
        } else if (multi == 2) {
            eval = Constants.MHZ;
        }
        return freq * eval;
    }

    private void initButtons(View view) {
        mGenerateButton = (TextView) view.findViewById(R.id.generateButton);
        mGenerateButton.setOnClickListener(mListener);
        mTerminateButton = (TextView) view.findViewById(R.id.terminateButton);
        mTerminateButton.setOnClickListener(mListener);
        mGenerateButton.setEnabled(false);
    }

    private void sendTerminateMessage() {
        String msg = Constants.E;
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
        String dutyString = mDutyField.getText().toString().trim();
        if (TextUtils.isEmpty(dutyString)) {
            showToast(getString(R.string.must_be_not_empty));
            mDutyField.setText("");
            return;
        }
        int percent = Integer.parseInt(dutyString);
        if (percent > 100 || percent < 0) {
            showToast(getString(R.string.must_be_less_than));
            mDutyField.setText("");
            return;
        }
        long frequency = Long.parseLong(freqString);
        int multi = mFrequencySelector.getSelectedItemPosition();
        long eval = 1;
        if (multi == 0) {
            eval = Constants.HZ;
        } else if (multi == 1) {
            eval = Constants.kHZ;
        } else if (multi == 2) {
            eval = Constants.MHZ;
        }
        frequency = frequency * eval;
        if (frequency > Constants.MAX_HZ) {
            showToast(getString(R.string.max_frequency4));
            mFrequencyField.setText("");
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
