package com.tuth.lejr;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends FragmentActivity implements View.OnClickListener {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    private GoogleSignInClient mGoogleSignInClient;

    private String groupID;
    private GroupFragment groupFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        findViewById(R.id.google_sign_in).setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mAuth = FirebaseAuth.getInstance();

        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            signInVisibility(View.INVISIBLE);
            assignGroup(currentUser.getUid());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
            }
        }

        if (requestCode == 379) {
            if (resultCode == RESULT_OK) {
                groupID = data.getStringExtra("SCAN_RESULT");
                groupFragment.setGroupID(groupID);
                groupFragment.lookForGroup();
            }
            if(resultCode == RESULT_CANCELED){
                Log.d(TAG, "qr scan canceled");
            }
        }
    }

    public void returnFromGroupFragment(String _groupID) {
        getSupportFragmentManager().popBackStack();

        groupID = _groupID;

        goToMainActivity();
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            assignGroup(user.getUid());
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void signInVisibility(int visibility) {
        findViewById(R.id.google_sign_in).setVisibility(visibility);
    }

    private void assignGroup(String userID) {
        DocumentReference userDocRef = db.collection("users").document(userID);
        userDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "User exists, going to MainActivity");
                        groupID = document.getString("group");
                        goToMainActivity();
                    } else {
                        Log.d(TAG, "User does not exist, going to GroupFragment");
                        goToGroupFragment();
                    }
                } else {
                    Log.d(TAG, "assign group failed", task.getException());
                    signInVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void goToGroupFragment() {
        groupFragment = new GroupFragment(this);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_frame, groupFragment)
                .addToBackStack("group_fragment")
                .commit();
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("groupID", groupID);
        startActivity(intent);
        finish();
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

//    private void signOut() {
//        // Firebase sign out
//        mAuth.signOut();
//
//        // Google sign out
//        mGoogleSignInClient.signOut().addOnCompleteListener(this,
//                new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        Log.d(TAG, "signOut:success");
//                    }
//                });
//    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.google_sign_in) {
            signIn();
        }
    }
}
