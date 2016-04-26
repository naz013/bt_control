package com.example.helio.arduino.dso;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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

    private Context mContext;
    private List<String> mDataList;

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

        ImageListItemBinding binding;

        public ImageViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
            binding.setClick(new ClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
        }
    }

    private void openActivity(int adapterPosition) {

    }

    @BindingAdapter("app:loadImage")
    public static void loadImage(ImageView imageView, String v) {
        //Log.d("TAG", "" + v);
        File file = new File(v);
        Picasso.with(imageView.getContext())
                .load(file)
                .into(imageView);
    }

}
