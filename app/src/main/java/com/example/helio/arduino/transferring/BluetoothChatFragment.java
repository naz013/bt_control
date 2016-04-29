package com.example.helio.arduino.transferring;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.helio.arduino.R;

public class BluetoothChatFragment extends Fragment {

    private static final int REQUEST_ENABLE_BT = 3;
    private static final String ARG_SERVER = "server_key";

    private RecyclerView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private String mConnectedDeviceName = null;
    private ConversationRecyclerAdapter mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter = null;
    private OriginalChatService mChatService = null;

    private Context mContext;
    private boolean mServer = false;

    public BluetoothChatFragment() {

    }

    public static BluetoothChatFragment newInstance(boolean isServer) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ARG_SERVER, isServer);
        BluetoothChatFragment fragment = new BluetoothChatFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBluetoothAdapter();
        checkBluetoothAvailability();
        getBundle();
    }

    private void initBluetoothAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void getBundle() {
        if (getArguments() != null) {
            mServer = getArguments().getBoolean(ARG_SERVER, true);
        }
    }

    private void checkBluetoothAvailability() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(mContext, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        checkAdapterStatus();
    }

    private void checkAdapterStatus() {
        if (!mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
        } else if (mChatService == null) {
            setupChat();
        }
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

    private void requestBluetoothEnable() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeBluetoothService();
    }

    private void resumeBluetoothService() {
        if (mChatService != null) {
            startBluetoothService();
        }
    }

    private void startBluetoothService() {
        if (mChatService.getState() == OriginalChatService.STATE_NONE) {
            mChatService.start();
            if (!mServer) {
                while (true) {
                    if (mChatService.getState() == OriginalChatService.STATE_LISTEN) {
                        connectDevice(true);
                        break;
                    }
                }
            } else ensureDiscoverable();
        }
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            discoverRequest();
        }
    }

    private void discoverRequest() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (RecyclerView) view.findViewById(R.id.in);
        mConversationView.setHasFixedSize(true);
        mConversationView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);
    }

    private void setupChat() {
        mConversationArrayAdapter = new ConversationRecyclerAdapter(getActivity());
        mConversationView.setAdapter(mConversationArrayAdapter);
        mOutEditText.setOnEditorActionListener(mWriteListener);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startSendingMessage(getView());
            }
        });

        mChatService = new OriginalChatService(getActivity(), mHandler);
        mOutStringBuffer = new StringBuffer("");
    }

    private void startSendingMessage(View view) {
        if (null != view) {
            TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
            String message = textView.getText().toString();
            sendMessage(message);
        }
    }

    private void sendMessage(String message) {
        if (mChatService.getState() != OriginalChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.writeMessage(send);
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    private final TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_WRITE:
                    addMyMessage(msg);
                    break;
                case Constants.MESSAGE_READ:
                    addTheirMessage(msg);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    getDeviceName(msg);
                    break;
                case Constants.MESSAGE_TOAST:
                    showMessage(msg);
                    break;
            }
        }
    };

    private void showMessage(Message msg) {
        String message = msg.getData().getString(Constants.TOAST);
        if (message == null) return;
        if (message.startsWith("Unable") || message.startsWith("Device")) {
            if (mChatService.getState() == OriginalChatService.STATE_NONE) {
                mChatService.start();
            }
            if (mChatService.getState() == OriginalChatService.STATE_LISTEN) {
                connectDevice(true);
            }
        }
        showToast(msg.getData().getString(Constants.TOAST));
    }

    private void getDeviceName(Message msg) {
        mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
        showToast(mContext.getString(R.string.connected_to) + " " + mConnectedDeviceName);
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void addTheirMessage(Message msg) {
        byte[] readBuf = (byte[]) msg.obj;
        String readMessage = new String(readBuf, 0, msg.arg1);
        mConversationArrayAdapter.addMessage(mConnectedDeviceName + ":  " + readMessage);
    }

    private void addMyMessage(Message message) {
        byte[] writeBuf = (byte[]) message.obj;
        String writeMessage = new String(writeBuf);
        mConversationArrayAdapter.addMessage("Me:  " + writeMessage);
    }

    private void connectDevice(boolean secure) {
        SharedPreferences preferences =
                mContext.getSharedPreferences(com.example.helio.arduino.Constants.PREFS, Activity.MODE_PRIVATE);
        String mAddress = preferences.getString(com.example.helio.arduino.Constants.DEVICE_ADDRESS, null);
        if (mAddress != null) {
            Log.d("TAG", mAddress);
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mAddress);
            mConnectedDeviceName = device.getName();
            mChatService.connect(device, secure);
        }
    }
}
