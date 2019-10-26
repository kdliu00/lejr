package com.tuth.lejr;

public class Entry {

    public String title, body;

    public Entry(){

    }

    public Entry(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

}
