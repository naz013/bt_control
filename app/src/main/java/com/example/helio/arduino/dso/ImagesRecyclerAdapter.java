package com.example.helio.arduino.dso;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.helio.arduino.R;
import com.example.helio.arduino.databinding.ImageListItemBinding;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImagesRecyclerAdapter extends RecyclerView.Adapter<ImagesRecyclerAdapter.ImageViewHolder> {

    private final Context mContext;
    private final List<String> mDataList;

    public ImagesRecyclerAdapter(Context context, List<String> list) {
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
        return mDataList != null ? mDataList.size() : 0;
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder{

        final ImageListItemBinding binding;

        public ImageViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
            binding.setClick(new ClickListener() {
                @Override
                public void onClick(View view) {
                    performClick(view.getId(), getAdapterPosition());
                }
            });
        }
    }

    private void performClick(int id, int adapterPosition) {
        switch (id) {
            case R.id.screenView:
                openActivity(adapterPosition);
                break;
            case R.id.removeButton:
                showConfirmationDialog(adapterPosition);
                break;
        }
    }

    private void showConfirmationDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(true);
        builder.setMessage(mContext.getString(R.string.are_you_sure));
        builder.setPositiveButton(mContext.getString(R.string.remove), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                removeImage(position);
            }
        });
        builder.setNegativeButton(mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
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
