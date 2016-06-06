package com.example.helio.arduino.signal;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.Constants;

public class PwmFragment extends Fragment {

    private FragmentListener mFragmentListener;

    private TextView mGenerate;
    private TextView mTerminate;
    private Spinner mFrequencySelector;
    private EditText mFrequencyField;
    private EditText mDutyField;
    private TextInputLayout cycleInput;
    private TextInputLayout freqInput;

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
        freqInput = (TextInputLayout) view.findViewById(R.id.freqInput);
        freqInput.setHintEnabled(false);
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

        cycleInput = (TextInputLayout) view.findViewById(R.id.cycleInput);
        cycleInput.setHintEnabled(false);
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
            mGenerate.setEnabled(true);
        } else {
            mGenerate.setEnabled(false);
        }
    }

    private void checkDuty() {
        cycleInput.refreshDrawableState();
        if (mDutyField.getText().toString().trim().matches("")){
            cycleInput.setErrorEnabled(true);
            cycleInput.setError(getString(R.string.must_be_not_empty));
            return;
        } else {
            cycleInput.setErrorEnabled(false);
        }
        int perc = Integer.parseInt(mDutyField.getText().toString().trim());
        if (perc > 100) {
            cycleInput.setErrorEnabled(true);
            cycleInput.setError(getString(R.string.must_be_less_than));
            mGenerate.setEnabled(false);
        } else {
            cycleInput.setErrorEnabled(false);
        }
    }

    private void checkFrequency() {
        if (mFrequencyField.getText().toString().trim().matches("")){
            freqInput.setErrorEnabled(true);
            freqInput.setError(getString(R.string.must_be_not_empty));
            return;
        } else {
            freqInput.setErrorEnabled(false);
        }

        if (getFrequency() > Constants.MAX_HZ) {
            freqInput.setErrorEnabled(true);
            freqInput.setError(getString(R.string.max_frequency4));
            mGenerate.setEnabled(false);
        } else {
            freqInput.setErrorEnabled(false);
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
        mGenerate = (TextView) view.findViewById(R.id.generateButton);
        mGenerate.setOnClickListener(mListener);
        mTerminate = (TextView) view.findViewById(R.id.terminateButton);
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
            mFrequencyField.setText("");
            return;
        }
        String dutyString = mDutyField.getText().toString().trim();
        if (dutyString.matches("")) {
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
    public void onDetach() {
        super.onDetach();
        mFragmentListener = null;
    }
}
