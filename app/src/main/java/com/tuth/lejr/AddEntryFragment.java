package com.tuth.lejr;

import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
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
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddEntryFragment extends Fragment implements View.OnClickListener {
    static final String TAG = "AddEntry";

    static final int FROM_GALLERY = 1;
    static final int FROM_CAMERA = 2;

    private View view;
    private File cameraFile;

    private String receiptTitle;
    private double receiptAmount;
    private Uri receiptUri;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Created.");
        view = inflater.inflate(R.layout.add_entry_fragment, container, false);
        view.findViewById(R.id.from_camera).setOnClickListener(this);
        view.findViewById(R.id.from_gallery).setOnClickListener(this);
        view.findViewById(R.id.confirm_image).setOnClickListener(this);
        view.findViewById(R.id.confirm_image).setVisibility(View.INVISIBLE);
        return view;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.from_gallery) {
            Log.d(TAG, "Selecting from gallery");
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/jpeg", "image/png"});
            startActivityForResult(intent, FROM_GALLERY);
        } else if (view.getId() == R.id.from_camera) {
            Log.d(TAG, "Selecting from camera");
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
        } else if (view.getId() == R.id.confirm_image) {
            Log.d(TAG, "Creating options frame");
            getActivity().getSupportFragmentManager().beginTransaction()
                    .add(R.id.add_entry_frame, new CreateEntryFragment(receiptAmount, receiptUri, receiptTitle))
                    .commit();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Uri imageUri = null;
            if (requestCode == FROM_GALLERY) {
                Log.d(TAG, "Received gallery response");
                imageUri = data.getData();
            } else if (requestCode == FROM_CAMERA) {
                Log.d(TAG, "Received camera response");
                if (cameraFile != null) {
                    imageUri = Uri.fromFile(cameraFile);
                }
            }
            if (imageUri != null) {
                receiptUri = imageUri;
                grabText();
            }
        }
    }

    private void updateImage() {
        ImageView iv = view.findViewById(R.id.captured_image);
        if (receiptUri != null) {
            iv.setImageURI(receiptUri);
        } else {
            iv.setImageURI(null);
        }
    }

    private void updateAmount() {
        EditText et = view.findViewById(R.id.receipt_amount);
        if (receiptAmount > 0) {
            et.setText("" + receiptAmount);
            view.findViewById(R.id.confirm_image).setVisibility(View.VISIBLE);
        } else {
            et.setText("");
            view.findViewById(R.id.confirm_image).setVisibility(View.INVISIBLE);
        }
    }

    private void grabText() {
        FirebaseVisionImage fvImage;
        try {
            fvImage = FirebaseVisionImage.fromFilePath(getContext(), receiptUri);
        } catch (IOException err) {
            err.printStackTrace();
            return;
        }
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        Task<FirebaseVisionText> result = detector.processImage(fvImage)
                .addOnSuccessListener(new GetReceiptInfo());
    }

    private class GetReceiptInfo implements OnSuccessListener<FirebaseVisionText> {
        private Pattern pAmount = Pattern.compile("\\d+\\.\\d\\d");

        @Override
        public void onSuccess(FirebaseVisionText fvText) {
            updateImage();
            // Look for TOTAL
            Rect location = null;
            for (FirebaseVisionText.TextBlock block : fvText.getTextBlocks()) {
                String blockText = block.getText();
                if (blockText.contains("TOTAL")) {
                    Log.d(TAG, "Block match =\n" + blockText);
                    for (FirebaseVisionText.Line line : block.getLines()) {
                        String lineText = line.getText();
                        if (lineText.contains("TOTAL") && !lineText.contains("SUBTOTAL")) {
                            Log.d(TAG, "Line match =\n" + lineText);
                            location = line.getBoundingBox();
                        }
                    }
                }
            }
            if (location == null) {
                return;
            }
            // Calculate look parameters
            int lineHeight = location.height();
            int minY = location.top - 2 * lineHeight;
            int maxY = location.bottom + 2 * lineHeight;
            // Look for amount
            double amount = 0;
            for (FirebaseVisionText.TextBlock block : fvText.getTextBlocks()) {
                Rect box = block.getBoundingBox();
                if (box.bottom > minY && box.top < maxY) {
                    Log.d(TAG, "Found block match =\n" + block.getText());
                    for (FirebaseVisionText.Line line : block.getLines()) {
                        box = line.getBoundingBox();
                        if (box.bottom > minY && box.top < maxY) {
                            String lineText = line.getText();
                            Matcher matches = pAmount.matcher(lineText);
                            while (matches.find()) {
                                String match = lineText.substring(matches.start(), matches.end());
                                Log.d(TAG, "Possible amount = " + match);
                                double currentAmount = Double.parseDouble(match);
                                if (currentAmount > amount) {
                                    amount = currentAmount;
                                }
                            }
                        }
                    }
                }
            }
            // Use amount
            Log.d(TAG, "Final amount = " + amount);
            if (amount != 0) {
                receiptAmount = amount;
                updateAmount();
                // Set temporary description
                final int numChars = 16;
                receiptTitle = fvText.getText().substring(0, numChars) + "...";
            }
        }
    }
}
