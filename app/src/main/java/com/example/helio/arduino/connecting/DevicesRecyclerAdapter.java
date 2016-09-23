package com.example.helio.arduino.connecting;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.helio.arduino.R;

import java.util.ArrayList;
import java.util.List;

public class DevicesRecyclerAdapter extends RecyclerView.Adapter<DevicesRecyclerAdapter.DeviceViewHolder> {

    private final Context mContext;
    private final List<String> mDataList;
    private final List<String> mDataAddresses;
    private final DeviceClickListener mListener;

    public DevicesRecyclerAdapter(Context context, DeviceClickListener listener) {
        this.mContext = context;
        this.mDataList = new ArrayList<>();
        this.mDataAddresses = new ArrayList<>();
        this.mListener = listener;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.device_list_item, null, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        holder.deviceName.setText(mDataList.get(position));
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    public String getDevice(int position) {
        return mDataAddresses.get(position);
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder{

        final TextView deviceName;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            deviceName = (TextView) itemView.findViewById(R.id.deviceName);
            deviceName.setOnClickListener(v -> handleClick(getAdapterPosition()));
        }
    }

    public void addDevice(String name, String address) {
        if (mDataList.size() > 0) {
            String noDevices = mContext.getString(R.string.none_found);
            if (mDataList.contains(noDevices)) {
                mDataList.remove(noDevices);
            }
            if (mDataList.contains(name)) {
                return;
            }
        }
        mDataList.add(name);
        mDataAddresses.add(address);
        int pos = mDataList.indexOf(name);
        notifyItemInserted(pos);
    }

    private void handleClick(int adapterPosition) {
        if (mListener != null) {
            mListener.onClick(null, adapterPosition);
        }
    }
}