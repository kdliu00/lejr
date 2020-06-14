package com.tuth.lejr;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class EntryAdapter extends FirestoreRecyclerAdapter<Entry, EntryAdapter.EntryHolder> {

    public interface OnEntryItemClickListener {
        void onEntryItemClick(Entry entryItem);
    }

    private final OnEntryItemClickListener clickListener;

    public EntryAdapter(@NonNull FirestoreRecyclerOptions<Entry> options, OnEntryItemClickListener _clickListener) {
        super(options);
        clickListener = _clickListener;
    }

    @Override
    protected void onBindViewHolder(@NonNull EntryHolder holder, int position, @NonNull Entry model) {
        holder.textViewTitle.setText(model.getTitle());
        holder.textViewDescription.setText(model.getDescription());

        holder.textViewDate.setText(model.getDateString());

        holder.bind(model, clickListener);
    }

    @NonNull
    @Override
    public EntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_item, parent, false);
        return new EntryHolder(v);
    }

    class EntryHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;
        TextView textViewDescription;
        TextView textViewDate;

        public EntryHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewDescription = itemView.findViewById(R.id.text_view_description);
            textViewDate = itemView.findViewById(R.id.text_view_date);
        }

        public void bind(final Entry entry, final OnEntryItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onEntryItemClick(entry);
                }
            });
        }

    }

}
