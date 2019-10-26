package com.tuth.lejr;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GroupFragment extends Fragment implements View.OnClickListener {

    private String email;
    private String name;
    private String groupID;
    private SignInActivity signInActivity;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private static FirebaseFirestore db;

    private EditText mEdit;

    private String TAG = "GroupFragment";

    public GroupFragment(SignInActivity _signInActivity) {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        signInActivity = _signInActivity;

        email = mUser.getEmail();
        name = mUser.getDisplayName();

        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.group_view, container, false);

        view.findViewById(R.id.find_group).setOnClickListener(this);
        view.findViewById(R.id.create_group).setOnClickListener(this);
        mEdit = view.findViewById(R.id.group_id_field);

        return view;
    }

    private void findGroup() {
        groupID = mEdit.getText().toString();

        DocumentReference docRef = db.collection("groups").document(groupID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "Group exists!");
                        updateGroup(document.get("members"));
                    } else {
                        Log.d(TAG, "No such group");
                        groupDoesNotExist(groupID);
                    }
                } else {
                    Log.d(TAG, "Error finding group", task.getException());
                }
            }
        });
    }

    private void updateGroup(Object _members) {
        HashMap<String, Object> members = (HashMap) _members;
        members.put(mUser.getUid(), 0f);

        DocumentReference docRef = db.collection("groups").document(groupID);
        docRef
            .update("members", members)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Group successfully updated!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error updating group", e);
                }
            });

        createUser(name, email, groupID);
    }

    private void groupDoesNotExist(String _groupID) {
        Toast toast = Toast.makeText(getContext(), _groupID, Toast.LENGTH_LONG);
        toast.show();
    }

    private void createGroup() {
        Map<String, Object> group = new HashMap<>();
        Map<String, Object> members = new HashMap<>();
        members.put(mUser.getUid(), 0f);
        group.put("members", members);

        db.collection("groups")
                .add(group)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Group added with ID: " + documentReference.getId());
                        groupID = documentReference.getId();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding group", e);
                    }
                });

        createUser(name, email, groupID);
    }

    private void createUser(String _name, String _email, String _groupID) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", _name);
        user.put("email", _email);
        user.put("group", _groupID);

        db.collection("users").document(mUser.getUid())
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User successfully created!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error creating user document", e);
                    }
                });

        signInActivity.returnFromGroupFragment(_groupID);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.find_group) {
            findGroup();
        } else if (i == R.id.create_group) {
            createGroup();
        }
    }
}
