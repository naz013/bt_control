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
import android.widget.PopupMenu;
import android.widget.TextView;

import com.example.helio.arduino.BuildConfig;
import com.example.helio.arduino.R;
import com.example.helio.arduino.core.ShareUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class FilesRecyclerAdapter extends RecyclerView.Adapter<FilesRecyclerAdapter.DeviceViewHolder> {

    private final Context mContext;
    private final List<FileItem> mDataList;

    FilesRecyclerAdapter(Context context, List<FileItem> list) {
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

    class DeviceViewHolder extends RecyclerView.ViewHolder{

        final TextView fileNameView;

        DeviceViewHolder(View itemView) {
            super(itemView);
            fileNameView = (TextView) itemView.findViewById(R.id.nameView);
            fileNameView.setOnClickListener(v -> handleClick(getAdapterPosition()));
            ImageButton button = (ImageButton) itemView.findViewById(R.id.deleteButton);
            ImageButton shareButton = (ImageButton) itemView.findViewById(R.id.shareButton);
            button.setOnClickListener(view -> showDeleteDialog(getAdapterPosition()));
            shareButton.setOnClickListener(view -> showPopup(getAdapterPosition(), view));
        }
    }

    private void showPopup(int position, View view) {
        PopupMenu popup = new PopupMenu(mContext, view);
        popup.getMenuInflater().inflate(R.menu.share_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.mail_action:
                    sendEmail(position);
                    break;
                case R.id.drive_action:
                    saveToDrive(position);
                    break;
            }
            return true;
        });
        popup.show();
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
