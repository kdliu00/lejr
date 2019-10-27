package com.tuth.lejr;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Map;

public class ShareAdapter extends RecyclerView.Adapter<ShareAdapter.ShareHolder> {

    private Map.Entry[] mDataset;
    private Entry entry;

    public ShareAdapter(Entry _entry) {
        mDataset = (Map.Entry[]) _entry.getShares().entrySet().toArray();
        entry = _entry;
    }

    @NonNull
    @Override
    public ShareHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.share_item, parent, false);
        return new ShareHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ShareHolder holder, int position) {
        holder.shareViewAmount.setText(String.valueOf(mDataset[position].getValue()));
        holder.shareViewTitle.setText(Entry.userMap.get(mDataset[position].getKey()));
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }

    class ShareHolder extends RecyclerView.ViewHolder {
        TextView shareViewTitle;
        TextView shareViewAmount;

        public ShareHolder(View itemView) {
            super(itemView);
            shareViewTitle = itemView.findViewById(R.id.share_view_title);
            shareViewAmount = itemView.findViewById(R.id.share_view_amount);
        }

    }

}
