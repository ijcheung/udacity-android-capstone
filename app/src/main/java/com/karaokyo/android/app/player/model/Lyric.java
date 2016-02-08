package com.karaokyo.android.app.player.model;

import java.io.Serializable;

public class Lyric implements Serializable{
    private static final long serialVersionUID = -3784285485513366239L;

    private int id;
    private String title;
    private String artist;
    private String uploader;
    private double rating;
    private String locale;
    private String description;

    public Lyric(int id, String title, String artist, String uploader, double rating, String locale, String description) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.uploader = uploader;
        this.rating = rating;
        this.locale = locale;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
