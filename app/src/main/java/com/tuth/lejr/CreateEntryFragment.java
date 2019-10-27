package com.tuth.lejr;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateEntryFragment extends Fragment implements View.OnClickListener, OnCompleteListener<Uri> {
    static final String TAG = "CreateEntry";

    private View view;

    private Date paymentDate;
    private String payerID;
    private double receiptAmount;
    private Uri receiptImage;
    private String receiptDesc;

    private List<Member> mModelList;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    public HashMap<String, Boolean> isSelected;


    public interface selectListener {
        public void onOkay(ArrayList<Integer> arrayList);
        public void onCancel();
    }

    public CreateEntryFragment(double amount, Uri image, String desc) {
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

        isSelected = new HashMap<>();
        ArrayList<Member> members = new ArrayList<>();
        for (String userID : Entry.userMap.keySet()) {
            members.add(new Member(Entry.userMap.get(userID), userID));
            isSelected.put(userID, true);
        }

        mRecyclerView = view.findViewById(R.id.member_list);
        mAdapter = new MemberAdapter(members, this);
        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setAdapter(mAdapter);

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

            for (String uid : Entry.userMap.keySet()) {
                if (!isSelected.get(uid)) {
                    isSelected.remove(uid);
                }
            }

            double numShares = isSelected.size();
            for (String uid : isSelected.keySet()) {
                paymentData.put(uid, receiptAmount / numShares);
            }

            data.put("shares", paymentData);

            FirebaseFirestore.getInstance()
                    .collection("groups")
                    .document(((MainActivity)getActivity()).groupID)
                    .collection("entries")
                    .document().set(data);

            Log.d(TAG, "Submitted data");

            // Update balances?
            final DocumentReference docRef = FirebaseFirestore.getInstance()
                    .collection("groups")
                    .document(((MainActivity)getActivity()).groupID);
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
}
