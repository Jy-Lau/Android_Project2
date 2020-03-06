package com.coursework.mp3player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {
    private Context mContext;
    private List<MusicModel> baseModels;//generic
    private ItemClickListener itemClickListener;
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        LayoutInflater mInflater = LayoutInflater.from(mContext);
        view = mInflater.inflate(R.layout.music_cardview, parent, false);
        return new MyViewHolder(view);
    }

    public RecyclerViewAdapter(Context context,List<MusicModel> baseModels,ItemClickListener itemClickListener) {
        this.mContext = context;
        this.baseModels = baseModels;
        this.itemClickListener=itemClickListener;
    }

    //holder object has IemCLickListener object
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        MusicModel assessmentTest= baseModels.get(position);
        holder.textview_artist.setText("Artist: "+assessmentTest.getArtist());
        holder.textview_title.setText("Title: "+assessmentTest.getTitle());
        holder.setItemClickListener(itemClickListener);
    }
    @Override
    public int getItemCount() {
        return baseModels.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textview_artist;
        TextView textview_title;
        ItemClickListener mItemClickListener;

        private MyViewHolder(View itemView) {
            super(itemView);
            textview_artist = itemView.findViewById(R.id.textview_artist);
            textview_title = itemView.findViewById(R.id.textview_title);
            itemView.setOnClickListener(this);
        }

        private void setItemClickListener(ItemClickListener itemClickListener) {
            this.mItemClickListener = itemClickListener;
        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getAdapterPosition(), false);
        }
    }
}
