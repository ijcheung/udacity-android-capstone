package com.karaokyo.android.app.player.model;

public class Playlist {
    private long id;
    private String title;

    public Playlist(long id, String title){
        this.id = id;
        this.title = title;
    }

    public long getId(){
        return id;
    }

    public void setId(long id){
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}