package com.example.helio.arduino.dso;

import android.content.Context;
import android.content.Intent;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.example.helio.arduino.R;
import com.example.helio.arduino.core.ShareUtil;
import com.example.helio.arduino.databinding.ImageListItemBinding;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImagesRecyclerAdapter extends RecyclerView.Adapter<ImagesRecyclerAdapter.ImageViewHolder> {

    private final Context mContext;
    private final List<String> mDataList;

    ImagesRecyclerAdapter(Context context, List<String> list) {
        this.mContext = context;
        this.mDataList = new ArrayList<>(list);
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        return new ImageViewHolder(DataBindingUtil.inflate(inflater, R.layout.image_list_item, parent, false).getRoot());
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        String item = mDataList.get(position);
        holder.binding.setImage(item);
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {

        final ImageListItemBinding binding;

        ImageViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
            binding.setClick(view -> performClick(view, getAdapterPosition()));
        }
    }

    private void performClick(View view, int adapterPosition) {
        switch (view.getId()) {
            case R.id.screenView:
                openActivity(adapterPosition);
                break;
            case R.id.removeButton:
                showConfirmationDialog(adapterPosition);
                break;
            case R.id.shareButton:
                showPopup(adapterPosition, view);
                break;
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
        ShareUtil.sendEmail(mContext, new File(mDataList.get(position)));
    }

    private void saveToDrive(int position) {
        ShareUtil.saveToDrive(mContext, new File(mDataList.get(position)));
    }

    private void showConfirmationDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(true);
        builder.setMessage(mContext.getString(R.string.are_you_sure));
        builder.setPositiveButton(mContext.getString(R.string.remove), (dialog, which) -> {
            dialog.dismiss();
            removeImage(position);
        });
        builder.setNegativeButton(mContext.getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void removeImage(int position) {
        if (position < mDataList.size()) {
            String path = mDataList.get(position);
            if (deleteFile(path)) {
                mDataList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(0, mDataList.size());
            }
        }
    }

    private boolean deleteFile(String path) {
        File file = new File(path);
        return file.exists() && file.delete();
    }

    private void openActivity(int adapterPosition) {
        mContext.startActivity(new Intent(mContext, ImagePreviewActivity.class)
                .putExtra(mContext.getString(R.string.image_path_intent), mDataList.get(adapterPosition)));
    }

    @BindingAdapter("app:loadImage")
    public static void loadImage(ImageView imageView, String v) {
        File file = new File(v);
        Picasso.with(imageView.getContext())
                .load(file)
                .into(imageView);
    }

}
