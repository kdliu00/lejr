package com.tuth.lejr;

import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.provider.MediaStore;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;

public class AddEntryFragment extends Fragment implements View.OnClickListener {
    static final String TAG = "AddEntry";

    static final int FROM_GALLERY = 1;
    static final int FROM_CAMERA = 2;

    private Activity myActivity;
    private View myView;
    private File cameraFile;

    public AddEntryFragment(Activity a) {
        myActivity = a;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Created.");
        myView = inflater.inflate(R.layout.add_entry_fragment, container, false);
        myView.findViewById(R.id.from_camera).setOnClickListener(this);
        myView.findViewById(R.id.from_gallery).setOnClickListener(this);
        return myView;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.from_gallery) {
            Log.d(TAG, "Selecting from gallery.");
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/jpeg", "image/png"});
            startActivityForResult(intent, FROM_GALLERY);
        } else if (view.getId() == R.id.from_camera) {
            Log.d(TAG, "Selecting from camera.");
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                try {
                    File image = File.createTempFile("camera_input", ".jpg",
                            getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES));
                    if (image != null) {
                        cameraFile = image;
                        Uri imageUri = FileProvider.getUriForFile(getContext(),
                                "com.tuth.lejr.fileprovider", image);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                        startActivityForResult(intent, FROM_CAMERA);
                    }
                } catch (IOException err) {
                    err.printStackTrace();
                    return;
                }
            } else {
                Log.d(TAG, "Failed.");
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Uri imageUri = null;
            if (requestCode == FROM_GALLERY) {
                Log.d(TAG, "Received gallery response.");
                imageUri = data.getData();
            } else if (requestCode == FROM_CAMERA) {
                Log.d(TAG, "Received camera response.");
                if (cameraFile != null) {
                    imageUri = Uri.fromFile(cameraFile);
                }
            }
            if (imageUri != null) {
                ImageView iv = myView.findViewById(R.id.captured_image);
                iv.setImageURI(imageUri);
                grabText(imageUri);
            }
        }
    }

    private void grabText(Uri imageUri) {
        FirebaseVisionImage fvImage;
        try {
            fvImage = FirebaseVisionImage.fromFilePath(getContext(), imageUri);
        } catch (IOException err) {
            err.printStackTrace();
            return;
        }
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        Task<FirebaseVisionText> result = detector.processImage(fvImage)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText fvText) {
                        String resultText = fvText.getText();
                        Log.d(TAG, resultText);
                    }
                });
    }
}
