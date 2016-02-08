package com.karaokyo.android.app.player.task;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.model.Line;
import com.karaokyo.android.app.player.model.Transition;
import com.karaokyo.android.app.player.service.LyricService;
import com.karaokyo.android.app.player.util.Constants;
import com.karaokyo.android.app.player.util.Utilities;

import org.jdom2.Element;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LyricLayoutTask extends AsyncTask<Element, Void, List<Line>> {
    private static final String TAG = "LyricLayoutTask";

    private WeakReference<LyricService> mParent;
    private int mScreenWidth;

    private int mTextSize;

    public LyricLayoutTask(LyricService parent, int screenWidth, int textSize){
        mParent = new WeakReference<LyricService>(parent);
        mScreenWidth = screenWidth;

        mTextSize = textSize;
    }

    @Override
    protected List<Line> doInBackground(Element... elements) {
        List<Line> list = new ArrayList<Line>();

        List<Element> lines = elements[0].getChild(Constants.KEY_LYRICS).getChildren(Constants.KEY_LINE);
        int width;
        try {
            for (Element line : lines) {
                if (isCancelled()) {
                    return new ArrayList<Line>();
                }
                String[] custom;

                int start = Utilities.timecodeToMilliseconds(line.getAttributeValue(Constants.KEY_START));
                int end = Utilities.timecodeToMilliseconds(line.getAttributeValue(Constants.KEY_END));
                boolean rtl = false;
                List<Element> highlights = line.getChildren(Constants.KEY_HIGHLIGHT);

                if(line.getAttributeValue(Constants.KEY_CLASS) != null){
                    custom = line.getAttributeValue(Constants.KEY_CLASS).split(" ");

                    for(String c : custom){
                        switch(c){
                            case "rtl":
                                rtl = true;
                        }
                    }
                }

                //Untimed line
                if (highlights.size() == 0) {
                    Line l = new Line(line.getText(), start, end, rtl);
                    l.setUntimed(true);
                    list.add(l);
                }
                //Timed line
                else {
                    Line l = new Line("", start, end, rtl);
                    for (int highlightIndex = 0; highlightIndex < highlights.size(); highlightIndex++) {
                        Element highlight = highlights.get(highlightIndex);
                        int highlightStart = Utilities.timecodeToMilliseconds(highlight.getAttributeValue(Constants.KEY_START));
                        int highlightEnd = Utilities.timecodeToMilliseconds(highlight.getAttributeValue(Constants.KEY_END));

                        //Check if highlight text causes an overflow
                        if ((width = Utilities.calculateWidth(l.getText() + highlight.getText(), mTextSize)) < mScreenWidth) {
                            l.setText(l.getText() + highlight.getText());
                            l.addTransition(new Transition(highlightStart, highlightEnd, width));
                            continue;
                        }
                        //Process the overflow
                        //Go word by word
                        Log.i(TAG, "'" + highlight.getText() + "'");

                        String[] words = highlight.getText().split(" ");

                        // split(" ") on empty string and any number of spaces
                        // returns a 0-length array in Java
                        if(words.length == 0){
                            words = highlight.getText().split("");
                        }

                        int wIndex = 0;
                        String text = words[0] + ((words.length == 1) ? "" : " ");

                        int breakPoint = 0;

                        //Calculate width of first word
                        int processedWidth = Utilities.calculateWidth(text, mTextSize);

                        //Calculate width of the highlight
                        int fullWidth = Utilities.calculateWidth(highlight.getText(), mTextSize);

                        //First word overflows?
                        if (processedWidth > mScreenWidth) {
                            //Blank line?
                            if (l.getText().equals("")) {
                                //We are out of options, add the word regardless
                                l.setText(text);
                                breakPoint = (highlightEnd - highlightStart) * processedWidth / fullWidth + highlightStart;
                                l.addTransition(new Transition(highlightStart, breakPoint, width));
                                wIndex++;
                            }
                        } else {
                            text = "";
                            String testText = "";
                            int w;
                            //Add words until an overflow occurs
                            for (; wIndex < words.length; wIndex++) {
                                testText += words[wIndex] + ((wIndex == words.length - 1) ? "" : " ");
                                if ((w = Utilities.calculateWidth(l.getText() + testText, mTextSize)) >= mScreenWidth) {
                                    break;
                                }
                                text = testText;
                                width = w;
                            }
                            l.setText(l.getText() + text);
                            processedWidth = Utilities.calculateWidth(text, mTextSize);
                            breakPoint = (highlightEnd - highlightStart) * processedWidth / fullWidth + highlightStart;
                            l.addTransition(new Transition(highlightStart, breakPoint, width));
                        }

                        //Remove the processed words from the overflowing highlight element
                        if (breakPoint != 0) {
                            text = TextUtils.join(" ", Arrays.copyOfRange(words, wIndex, words.length));
                            highlight.setText(text);
                            highlight.setAttribute("start", Utilities.millisecondsToTimecode(breakPoint));
                        }
                        //Finalize Line and Add to List
                        list.add(l);

                        l = new Line("", start, end, rtl);
                        highlightIndex--;
                    }
                    //Finalize line
                    if (!l.getText().equals("")) {
                        list.add(l);
                    }
                }
            }
        }
        catch(NumberFormatException e){
            //TODO: Couldn't parse lyrics
            Log.i(TAG, e.toString());
            return null;
        }

        return list;
    }

    @Override
    protected void onPostExecute(List<Line> lines) {
        if(mParent.get() != null) {
            if(lines == null) {
                Utilities.showError(mParent.get(), R.string.error_lyric_has_invalid_timecode);
            }
            else {
                mParent.get().onLyricLayoutTaskComplete(lines);
            }
        }
    }
}