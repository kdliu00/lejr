package com.tuth.lejr;

import android.os.Bundle;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "MainActivity";

    public  String groupID;
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Query query;
    private RecyclerView recyclerView;
    private EntryAdapter entryAdapter;

    private Double balance = 0.0;
    private String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateBalance();

        groupID = getIntent().getStringExtra("groupID");
        updateQuery();

        getUserMap();

        setUpBalanceListener();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);

        findViewById(R.id.view_group_details).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.fab) {
            addEntry();
        } else if (i == R.id.view_group_details) {
            viewGroupDetails();
        }
    }

    private void viewGroupDetails() {
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_frame, new GroupDetailsFragment(groupID))
                .commit();
        findViewById(R.id.fab).setVisibility(View.INVISIBLE);
    }

    private void addEntry() {
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_frame, new AddEntryFragment())
                .commit();
        findViewById(R.id.fab).setVisibility(View.INVISIBLE);
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

    private void updateBalance() {
        TextView balanceText = findViewById(R.id.current_balance_text);
        String sign = balance < 0 ? "-" : "";
        String balanceString = String.join("", sign, "$", String.valueOf(balance));
        balanceText.setText(balanceString);
    }

    private void setUpBalanceListener() {
        final DocumentReference docRef = db.collection("groups").document(groupID);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: " + snapshot.getData());
                    HashMap<String, Number> memberBalances = (HashMap<String, Number>) snapshot.get("members");
                    balance = memberBalances.get(userID).doubleValue();
                    updateBalance();
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    private void setUpRecyclerView() {
        FirestoreRecyclerOptions<Entry> options = new FirestoreRecyclerOptions.Builder<Entry>()
                .setQuery(query, Entry.class)
                .build();

        entryAdapter = new EntryAdapter(options, new EntryAdapter.OnEntryItemClickListener() {
            @Override
            public void onEntryItemClick(Entry entryItem) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_frame, new EntryFragment(entryItem))
                        .commit();
                findViewById(R.id.fab).setVisibility(View.INVISIBLE);
            }
        });

        recyclerView = findViewById(R.id.entry_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(entryAdapter);

        entryAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        entryAdapter.stopListening();
    }

    @Override
    public void onBackPressed() {
        int stackCount = getSupportFragmentManager().getBackStackEntryCount();
//        if (stackCount > 0) {
//            getSupportFragmentManager().popBackStack();
//        } else {
//            super.onBackPressed();
//        }
        FloatingActionButton fab = findViewById(R.id.fab);
        if (fab.getVisibility() == View.INVISIBLE && stackCount == 0) {
            findViewById(R.id.fab).setVisibility(View.VISIBLE);
        }
    }
}
