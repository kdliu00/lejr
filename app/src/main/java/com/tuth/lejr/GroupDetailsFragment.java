package com.tuth.lejr;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class GroupDetailsFragment extends Fragment {

    private String groupID;
    private String TAG = "GroupDetailsFragment";

    public GroupDetailsFragment(String _groupID) {
        groupID = _groupID;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.group_details_view, container, false);

        Bitmap bitmap = QRCodeHelper
                .newInstance(this.getContext())
                .setContent(groupID)
                .setErrorCorrectionLevel(ErrorCorrectionLevel.Q)
                .setMargin(2)
                .getQRCOde();

        ImageView qr = view.findViewById(R.id.qr_code);
        qr.setImageBitmap(bitmap);

        return view;
    }

}
