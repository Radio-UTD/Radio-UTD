package com.utd.radio.models;

import android.graphics.drawable.Drawable;

/**
 * Created by Rahat on 9/22/2014.
 */
public class ThumbnailCard {
    private Drawable thumbnail;
    private String title;
    private String subtitle;
    private String subsubtitle;

    public ThumbnailCard(String title, String subtitle, String subsubtitle, Drawable thumbnail)
    {
        this.title = title;
        this.subtitle = subtitle;
        this.subsubtitle = subsubtitle;
        this.thumbnail = thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getSubsubtitle() { return subsubtitle; }

    public Drawable getThumbnail() {
        return thumbnail;
    }
}
