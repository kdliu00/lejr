package com.tuth.lejr;

public class Member {

    private String name;
    private String userID;
    private boolean isSelected = true;

    public Member(String _name, String _userID) {
        name = _name;
        userID = _userID;
    }

    public String getName() {
        return name;
    }

    public String getUserID() {
        return userID;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isSelected() {
        return isSelected;
    }

}
