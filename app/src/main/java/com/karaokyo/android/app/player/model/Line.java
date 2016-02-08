package com.karaokyo.android.app.player.model;

import java.util.ArrayList;

public class Line {
    private String text;
    private int start;
    private int end;
    private boolean untimed;
    private boolean rtl = false;
    private ArrayList<Transition> transitions;

    public Line(String text, int start, int end, boolean rtl){
        this.text = text;
        this.start = start;
        this.end = end;
        this.rtl = rtl;

        transitions = new ArrayList<Transition>();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public boolean isUntimed() {
        return untimed;
    }

    public void setUntimed(boolean untimed) {
        this.untimed = untimed;
    }

    public boolean isRtl() {
        return rtl;
    }

    public void setRtl(boolean rtl) {
        this.rtl = rtl;
    }

    public ArrayList<Transition> getTransitions() {
        return transitions;
    }

    public void addTransition(Transition transition) {
        transitions.add(transition);
    }
}
