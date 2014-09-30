package com.utd.radio.models;

public class Metadata {

    public Metadata() {
        artist = song = album = showName = "";
    }

    public String artist;
    public String song;
    public String album;
    public String showName;

    @Override
    public String toString() {
        return artist + " - " + song + " [" + album + "] {" + showName + "}";
    }
}