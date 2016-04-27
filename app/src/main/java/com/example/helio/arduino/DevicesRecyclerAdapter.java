package com.example.helio.arduino;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DevicesRecyclerAdapter extends RecyclerView.Adapter<DevicesRecyclerAdapter.DeviceViewHolder> {

    private Context mContext;
    private List<String> mDataList;
    private DeviceClickListener mListener;

    public DevicesRecyclerAdapter(Context context, DeviceClickListener listener) {
        this.mContext = context;
        this.mDataList = new ArrayList<>();
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
        return mDataList != null ? mDataList.size() : 0;
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder{

        TextView deviceName;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            deviceName = (TextView) itemView.findViewById(R.id.deviceName);
            deviceName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleClick(getAdapterPosition());
                }
            });
        }
    }

    public void addDevice(String name) {
        mDataList.add(name);
        int pos = mDataList.indexOf(name);
        notifyItemInserted(pos);
    }

    private void handleClick(int adapterPosition) {
        if (mListener != null) {
            mListener.onClick(null, adapterPosition);
        }
    }
}
