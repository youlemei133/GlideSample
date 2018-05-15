package com.hudawei.glidesample;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by hudawei on 2018/5/3.
 */

public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.ViewHolder> {
    private String url = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1524215488479&di=8248a23793b15fd253482068bf2e9160&imgtype=0&src=http%3A%2F%2Fimgsrc.baidu.com%2Fimage%2Fc0%253Dshijue1%252C0%252C0%252C294%252C40%2Fsign%3D742c802fc3ea15ce55e3e84ade695086%2F37d3d539b6003af34d9032ca3f2ac65c1038b676.jpg";
    int count = 50;
    private Context mContext;

    public void addItem() {
        count += 10;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mContext == null)
            mContext = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tv_label.setText("申论积累" + position);
        holder.tv_author.setText("文/小洁老师整理编辑" + position);
        holder.tv_title.setText("常识判断之刑法法律知识" + position);
        GlideApp.with(mContext)
                .load(url)
                .centerCrop()
                .into(holder.iv_body);
        GlideApp.with(mContext)
                .load(R.mipmap.ic_launcher_round)
                .into(holder.iv_head);
    }

    @Override
    public int getItemCount() {
        return count;
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_label;
        TextView tv_title;
        TextView tv_author;
        TextView tv_time;
        ImageView iv_head;
        ImageView iv_body;

        public ViewHolder(View itemView) {
            super(itemView);
            tv_label = itemView.findViewById(R.id.tv_label);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_author = itemView.findViewById(R.id.tv_author);
            tv_time = itemView.findViewById(R.id.tv_time);
            iv_head = itemView.findViewById(R.id.iv_head);
            iv_body = itemView.findViewById(R.id.iv_body);
        }
    }
}
