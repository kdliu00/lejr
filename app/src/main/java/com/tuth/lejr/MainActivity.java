package com.tuth.lejr;

import android.os.Bundle;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "MainActivity";

    private String groupID;
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Query query;
    private RecyclerView recyclerView;
    private EntryAdapter entryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        groupID = getIntent().getStringExtra("groupID");
        updateQuery();

        getUserMap();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.fab) {
            addEntry();
        }
    }

    private void addEntry() {
        getSupportFragmentManager().beginTransaction()
            .add(R.id.add_entry_frame, new AddEntryFragment(this))
            .commit();
    }

    private void updateQuery() {
        CollectionReference colRef = db.collection("groups").document(groupID).collection("entries");
        query = colRef.orderBy("date", Query.Direction.DESCENDING);
    }

    private void getUserMap() {
        DocumentReference userDocRef = db.collection("groups").document(groupID);
        userDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    HashMap<String, String> retrievedUserMap = (HashMap<String, String>) document.get("userMap");
                    if (document.exists() && retrievedUserMap != null) {
                        Log.d(TAG, "group found, user map not null");
                        Entry.userMap = retrievedUserMap;
                        setUpRecyclerView();
                    } else {
                        Log.d(TAG, "group does not exist");
                    }
                } else {
                    Log.d(TAG, "getUserMap failed", task.getException());
                }
            }
        });
    }

    private void setUpRecyclerView() {
        FirestoreRecyclerOptions<Entry> options = new FirestoreRecyclerOptions.Builder<Entry>()
                .setQuery(query, Entry.class)
                .build();

        entryAdapter = new EntryAdapter(options);

        recyclerView = findViewById(R.id.entry_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(entryAdapter);

        entryAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        entryAdapter.stopListening();
    }
}
