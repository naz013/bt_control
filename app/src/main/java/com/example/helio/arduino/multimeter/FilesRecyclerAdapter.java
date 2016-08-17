package com.example.helio.arduino.multimeter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
            ImageButton button = (ImageButton) itemView.findViewById(R.id.deleteButton);
            button.setOnClickListener(view -> showDeleteDialog(getAdapterPosition()));
        }
    }

    private void showDeleteDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(true);
        builder.setMessage(mContext.getString(R.string.are_you_sure));
        builder.setPositiveButton(mContext.getString(R.string.remove), (dialog, which) -> {
            dialog.dismiss();
            removeItem(position);
        });
        builder.setNegativeButton(mContext.getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void removeItem(int position) {
        mDataList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(0, mDataList.size());
    }

    private void handleClick(int position) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(mDataList.get(position).getFullPath())), "application/vnd.ms-excel");
        mContext.startActivity(intent);
    }
}
