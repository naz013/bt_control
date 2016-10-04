package com.example.helio.arduino.multimeter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.helio.arduino.BuildConfig;
import com.example.helio.arduino.R;
import com.example.helio.arduino.core.ShareUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

class FilesRecyclerAdapter extends RecyclerView.Adapter<FilesRecyclerAdapter.DeviceViewHolder> {

    private final Context mContext;
    private final List<FileItem> mDataList;
    private int item = 0;

    FilesRecyclerAdapter(Context context, List<FileItem> list) {
        this.mContext = context;
        this.mDataList = new ArrayList<>(list);
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.file_item_layout, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        holder.fileNameView.setText(mDataList.get(position).getFileName());
        File file = new File(mDataList.get(position).getFullPath());
        if (file.exists()) {
            holder.fileDateView.setText(getConvertedDate(file.lastModified()));
        }
    }

    private String getConvertedDate(long mills) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mills);
        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return format.format(calendar.getTime());
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder{

        final TextView fileNameView;
        final TextView fileDateView;

        DeviceViewHolder(View itemView) {
            super(itemView);
            fileNameView = (TextView) itemView.findViewById(R.id.nameView);
            fileDateView = (TextView) itemView.findViewById(R.id.dateView);
            itemView.findViewById(R.id.card).setOnClickListener(v -> handleClick(getAdapterPosition()));
            ImageButton button = (ImageButton) itemView.findViewById(R.id.deleteButton);
            ImageButton shareButton = (ImageButton) itemView.findViewById(R.id.shareButton);
            button.setOnClickListener(view -> showDeleteDialog(getAdapterPosition()));
            shareButton.setOnClickListener(view -> showShareDialog(getAdapterPosition()));
        }
    }

    private void showShareDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(true);
        builder.setTitle(R.string.send_data_file);
        item = 0;
        builder.setSingleChoiceItems(R.array.share_options, item, (dialogInterface, i) -> item = i);
        builder.setPositiveButton(R.string.send, (dialog, which) -> {
            if (item == 0) {
                saveToDrive(position);
            } else {
                sendEmail(position);
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(mContext.getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sendEmail(int position) {
        ShareUtil.sendEmail(mContext, new File(mDataList.get(position).getFullPath()));
    }

    private void saveToDrive(int position) {
        ShareUtil.saveToDrive(mContext, new File(mDataList.get(position).getFullPath()));
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
        new File(mDataList.get(position).getFullPath()).delete();
        mDataList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(0, mDataList.size());
    }

    private void handleClick(int position) {
        File file = new File(mDataList.get(position).getFullPath());
        Uri uri = Uri.fromFile(file);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", file);
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.ms-excel");
        mContext.startActivity(intent);
    }
}
