package com.tuth.lejr;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Entry {
    public static HashMap<String, String> userMap;

    private String description, imagePath, payer;

    private Date date;

    private HashMap<String, Object> shares;

    private Double amount;

    public Entry() {

    }

    public Entry(String _description, String _imagePath, String _payer, String _entryID,
                 Date _date, HashMap<String, Object> _shares, Double _amount) {
        description = _description;
        imagePath = _imagePath;
        payer = _payer;

        date = _date;
        shares = _shares;

        amount = _amount;
    }

    public String getDescription() {
        return description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getPayer() {
        return payer;
    }

    public String getPayerName() {
        return userMap.get(payer);
    }

    public Date getDate() {
        return date;
    }

    public String getDateString() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String dateString = simpleDateFormat.format(getDate());
        return dateString;
    }

    public HashMap<String, Object> getShares() {
        return shares;
    }

    public Double getAmount() {
        return amount;
    }

    public String getTitle() {
        String amountString = String.join("", "$", String.valueOf(getAmount()));
        return String.join(" - ", getPayerName(), amountString);
    }

}
