package com.tuth.lejr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EntryFragment extends Fragment {

    private Entry entry;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private ImageView imageView;
    private TextView title;
    private TextView description;
    private TextView date;

    private FirebaseStorage storage = FirebaseStorage.getInstance();

    public EntryFragment(Entry _entry) {
        entry = _entry;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.entry_view, container, false);

        imageView = view.findViewById(R.id.entry_image_view);
        title = view.findViewById(R.id.entry_title_view);
        description = view.findViewById(R.id.entry_description_view);

        StorageReference gsReference = storage.getReferenceFromUrl(entry.getImagePath());

        Glide.with(this)
                .load(gsReference)
                .into(imageView);

        title.setText(entry.getTitle());
        description.setText(entry.getDescription());
        date.setText(entry.getDateString());

        recyclerView = view.findViewById(R.id.shares_recycler_view);
        layoutManager = new LinearLayoutManager(this.getContext());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new ShareAdapter(entry);

        return view;
    }

}
