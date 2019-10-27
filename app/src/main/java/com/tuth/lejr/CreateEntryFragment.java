package com.tuth.lejr;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CreateEntryFragment extends Fragment implements View.OnClickListener, OnCompleteListener<Uri> {
    static final String TAG = "CreateEntry";

    private View view;

    private Date paymentDate;
    private String payerID;
    private double receiptAmount;
    private Uri receiptImage;
    private String receiptDesc;

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

            HashMap<String, Object> paymentData = new HashMap<>();
            paymentData.put(payerID, receiptAmount);

            data.put("shares", paymentData);

            FirebaseFirestore.getInstance()
                    .collection("groups")
                    .document(((MainActivity)getActivity()).groupID)
                    .collection("entries")
                    .document().set(data);

            Log.d(TAG, "Submitted data");


        }
    }
}
