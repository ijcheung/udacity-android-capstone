package com.karaokyo.android.app.player.model;

public class Transition {
    private long start;
    private long end;
    private int width;

    public Transition(long start, long end, int width){
        this.start = start;
        this.end = end;
        this.width = width;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
