package com.example.helio.arduino.transferring;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.helio.arduino.R;

import java.util.ArrayList;
import java.util.List;

public class ConversationRecyclerAdapter extends RecyclerView.Adapter<ConversationRecyclerAdapter.DeviceViewHolder> {

    private Context mContext;
    private List<String> mDataList;

    public ConversationRecyclerAdapter(Context context) {
        this.mContext = context;
        this.mDataList = new ArrayList<>();
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

    public void addMessage(String message) {
        mDataList.add(message);
        int pos = mDataList.indexOf(message);
        notifyItemInserted(pos);
    }

    @Override
    public int getItemCount() {
        return mDataList != null ? mDataList.size() : 0;
    }

    public void clear() {
        mDataList.clear();
        notifyDataSetChanged();
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder{

        TextView deviceName;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            deviceName = (TextView) itemView.findViewById(R.id.deviceName);
        }
    }
}
