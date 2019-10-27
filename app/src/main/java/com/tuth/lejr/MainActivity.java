package com.tuth.lejr;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String groupID;
    private static FirebaseFirestore db;
    private Query query;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.entry_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);

        groupID = getIntent().getStringExtra("groupID");

        // TODO: handle null groupID

        db = FirebaseFirestore.getInstance();
        updateQuery();
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

}
