package com.example.helio.arduino.multimeter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.helio.arduino.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesRecyclerAdapter extends RecyclerView.Adapter<FilesRecyclerAdapter.DeviceViewHolder> {

    private final Context mContext;
    private final List<FileItem> mDataList;

    public FilesRecyclerAdapter(Context context, List<FileItem> list) {
        this.mContext = context;
        this.mDataList = new ArrayList<>(list);
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.file_item_layout, null, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        holder.fileNameView.setText(mDataList.get(position).getFileName());
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder{

        final TextView fileNameView;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            fileNameView = (TextView) itemView.findViewById(R.id.nameView);
            fileNameView.setOnClickListener(v -> handleClick(getAdapterPosition()));
        }
    }

    private void handleClick(int adapterPosition) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(mDataList.get(adapterPosition).getFullPath())), "application/vnd.ms-excel");
        mContext.startActivity(intent);
    }
}
