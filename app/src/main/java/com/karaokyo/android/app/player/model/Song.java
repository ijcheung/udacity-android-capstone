package com.karaokyo.android.app.player.model;

import java.io.Serializable;

public class Song implements Serializable {
    static final long serialVersionUID = -2136949814626227307L;

    private String title;
    private String artist;
    private String lyric;
    long songId;

    public Song(String title, String artist, long songId, String lyric){
        this.title = title;
        this.artist = artist;
        this.lyric = lyric;
        this.songId = songId;
    }

    public Song(String title, String artist, long songId){
        this.songId = songId;
        this.title = title;
        this.artist = artist;
    }

    public String getTitle(){
        return title;
    }

    public String getArtist(){
        return artist;
    }

    public long getSongId(){
        return songId;
    }

    public void setSongId(long songId) {
        this.songId = songId;
    }

    public String getLyricFile(){
        return lyric;
    }

    public boolean equals(Song s){
        if(title.equals(s.title) && artist.equals(s.artist) && songId == s.songId && lyric == s.lyric){
            return true;
        }

        return false;
    }
}
