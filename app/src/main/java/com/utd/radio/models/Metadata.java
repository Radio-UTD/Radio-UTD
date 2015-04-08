package com.utd.radio.models;

public class Metadata {

    public Metadata() {
        artist = song = album = showName = showDJ =  "";
    }

    public String artist;
    public String song;
    public String album;
    public String showName;
    public String showDJ;
    public String avatar;

    @Override
    public String toString() {
        return artist + " - " + song + " [" + album + "] {" + showName + " with " + showDJ + "} (" + avatar + ")";
    }
}