package com.utd.radio.models;

import android.graphics.drawable.Drawable;

/**
 * Created by Rahat on 9/22/2014.
 */
public class ThumbnailCard {
    private Drawable thumbnail;
    private String title = "";
    private String subtitle = "";

    public ThumbnailCard(String title)
    {
        this.title = title;
    }

    public ThumbnailCard(String title, String subtitle, Drawable thumbnail)
    {
        this.title = title;
        this.subtitle = subtitle;
        this.thumbnail = thumbnail;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Drawable getThumbnail() {
        return thumbnail;
    }
}
