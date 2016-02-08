package com.karaokyo.android.app.player.model;

import java.util.ArrayList;
import java.util.List;

public class Lyrics {
    //{"total":57,"total_pages":6,"first_page":true,"last_page":false,"previous_page":null,"next_page":2,"out_of_bounds":false,"offset":0}
    private List<Lyric> lyrics;
    private boolean firstPage;
    private boolean lastPage;
    private int nextPage;

    public Lyrics(boolean firstPage, boolean lastPage, int nextPage){
        lyrics = new ArrayList<Lyric>();

        this.firstPage = firstPage;
        this.lastPage = lastPage;
        this.nextPage = nextPage;
    }

    public List<Lyric> getLyrics() {
        return lyrics;
    }

    public void addLyric(Lyric lyric) {
        lyrics.add(lyric);
    }

    public boolean isFirstPage() {
        return firstPage;
    }

    public void setFirstPage(boolean firstPage) {
        this.firstPage = firstPage;
    }

    public boolean isLastPage() {
        return lastPage;
    }

    public void setLastPage(boolean lastPage) {
        this.lastPage = lastPage;
    }

    public int getNextPage() {
        return nextPage;
    }

    public void setNextPage(int nextPage) {
        this.nextPage = nextPage;
    }
}
