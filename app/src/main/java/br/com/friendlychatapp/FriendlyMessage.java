package br.com.friendlychatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class FriendlyMessage {
    private String text;
    private String name;
    private String photoUrl;

    public FriendlyMessage(){}

    public FriendlyMessage(String text, String name, @Nullable String photoUrl){
        this.text = text;
        this.name = name;
        this.photoUrl = photoUrl;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    @NonNull
    @Override
    public String toString() {
        return "Name: "+getName()+"\tText: "+getText();
    }
}
