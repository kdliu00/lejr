package com.tuth.lejr;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CreateEntryFragment extends Fragment implements View.OnClickListener, OnCompleteListener<Uri> {
    static final String TAG = "CreateEntry";

    private View view;
    private String groupID;

    private Date paymentDate;
    private String payerID;
    private double receiptAmount;
    private Uri receiptImage;
    private String receiptDesc;

    public CreateEntryFragment(String gid, double amount, Uri image, String desc) {
        groupID = gid;
        payerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        receiptAmount = amount;
        receiptImage = image;
        receiptDesc = desc;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.create_entry_fragment, container, false);
        // Initialize
        ((TextView)view.findViewById(R.id.ce_payer_name)).setText(Entry.userMap.get(payerID));
        ((TextView)view.findViewById(R.id.ce_amount)).setText("$" + receiptAmount);
        ((TextView)view.findViewById(R.id.ce_desc)).setText(receiptDesc);
        view.findViewById(R.id.ce_submit).setOnClickListener(this);

        // TODO: Fill out user names

        return view;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.ce_submit) {
            paymentDate = new Date();
            // Upload image
            final StorageReference imageRef = FirebaseStorage.getInstance().getReference()
                    .child("images/" + paymentDate.getTime() + receiptImage.getLastPathSegment());
            imageRef.putFile(receiptImage)
                    .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return imageRef.getDownloadUrl();
                        }
                    }).addOnCompleteListener(this);

        }
    }

    @Override
    public void onComplete(@NonNull Task<Uri> task) {
        if (task.isSuccessful()) {
            Uri downloadUri = task.getResult();
            // Complete submission
            Map<String, Object> data = new HashMap<>();
            data.put("amount", receiptAmount);
            data.put("date", paymentDate);
            data.put("description", receiptDesc);
            data.put("imagePath", downloadUri.toString());
            data.put("payer", payerID);

            final HashMap<String, Double> paymentData = new HashMap<>();
            int numShares = Entry.userMap.size();
            for (String uid : Entry.userMap.keySet()) {
                paymentData.put(uid, receiptAmount / numShares);
            }

            data.put("shares", paymentData);

            FirebaseFirestore.getInstance()
                    .collection("groups")
                    .document(groupID)
                    .collection("entries")
                    .document().set(data);

            Log.d(TAG, "Submitted data");

            // Update balances?
            final DocumentReference docRef = FirebaseFirestore.getInstance()
                    .collection("groups")
                    .document(groupID);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        HashMap<String, Number> balanceMap = (HashMap<String, Number>) document.get("members");
                        if (document.exists() && balanceMap != null) {
                            Log.d(TAG, "Updating balances");
                            for (String uid : paymentData.keySet()) {
                                balanceMap.put(uid, balanceMap.getOrDefault(uid, 0.0).doubleValue() - paymentData.get(uid));
                            }
                            balanceMap.put(payerID, balanceMap.getOrDefault(payerID, 0.0).doubleValue() + receiptAmount);
                            // Resubmit balances
                            docRef.update("members", balanceMap);
                        } else {
                            Log.d(TAG, "Failed");
                        }
                    } else {
                        Log.d(TAG, "Failed", task.getException());
                    }
                }
            });
        }
    }

    private void setUpRecyclerView() {
        FirebaseFirestore.getInstance()
                .collection("groups")
                .document(groupID)
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    HashMap<String, Object> balanceMap = (HashMap<String, Object>) document.get("members");
                    if (document.exists() && balanceMap != null) {
                        Log.d(TAG, "Updating balances");

                    } else {
                        Log.d(TAG, "Failed");
                    }
                } else {
                    Log.d(TAG, "Failed", task.getException());
                }
            }
        });
//        Query query = colRef.orderBy("date", Query.Direction.DESCENDING);
//
//        FirestoreRecyclerOptions<Entry> options = new FirestoreRecyclerOptions.Builder<Entry>()
//                .setQuery(query, Entry.class)
//                .build();
//
//        entryAdapter = new EntryAdapter(options, new EntryAdapter.OnEntryItemClickListener() {
//            @Override
//            public void onEntryItemClick(Entry entryItem) {
//                getSupportFragmentManager().beginTransaction()
//                        .add(R.id.add_entry_frame, new EntryFragment(entryItem))
//                        .commit();
//                findViewById(R.id.fab).setVisibility(View.INVISIBLE);
//            }
//        });
//
//        RecyclerView rv = view.findViewById(R.id.entry_recycler);
//        rv.setLayoutManager(new LinearLayoutManager(getContext()));
//        rv.setAdapter(entryAdapter);
//
//        entryAdapter.startListening();
    }
}
