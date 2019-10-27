package com.tuth.lejr;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;
import java.util.HashMap;

public class CreateEntryFragment extends Fragment implements View.OnClickListener {

    private View view;
    private AddEntryFragment parent;

    public static HashMap<String, String> userMap;

    private String description, imagePath, payer;

    private HashMap<String, Object> shares;

    private double receiptAmount;
    private Uri receiptImage;

    public CreateEntryFragment(double amount, Uri image) {
        receiptAmount = amount;
        receiptImage = image;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.create_entry_fragment, container, false);
        // Initialize
        ((TextView)view.findViewById(R.id.ce_amount)).setText("$" + receiptAmount);
        view.findViewById(R.id.ce_submit).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {

    }
}
