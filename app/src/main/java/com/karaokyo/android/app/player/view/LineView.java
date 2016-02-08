package com.karaokyo.android.app.player.view;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.lib.animation.WidthAnimation;
import com.karaokyo.android.lib.widget.StrokedTextView;

public class LineView extends FrameLayout {
    private static final LinearInterpolator LINEAR = new LinearInterpolator();

    private StrokedTextView mText;
    private TextView mOver;

    private int mStart;
    private int mEnd;

    public LineView(Context context, String s, int start, int end, int textSize, int textColor, int strokeColor, int highlightColor, int strokeWidth) {
        super(context);

        this.mStart = start;
        this.mEnd = end;

        View root = LayoutInflater.from(context).inflate(R.layout.line, null);
        mText = (StrokedTextView) root.findViewById(R.id.text);
        mOver = (TextView) root.findViewById(R.id.over);

        setTextSize(textSize);
        mText.setTextColor(textColor);
        mText.setStrokeColor(strokeColor);
        mText.setStrokeWidth(strokeWidth);
        mText.setText(s);

        mOver.setTextColor(highlightColor);

        setVisibility(GONE);
        setDrawingCacheEnabled(false);
        setLayerType(LAYER_TYPE_NONE, null);

        addView(root);
    }

    @Override
    public void setVisibility(int visibility) {
        switch(visibility){
            case View.GONE:
                clearAnimation();
                setPosition(0);
                break;
        }
        super.setVisibility(visibility);
    }

    public void setRtl(boolean rtl){
        if(rtl){
            ((RelativeLayout.LayoutParams) mOver.getLayoutParams()).addRule(RelativeLayout.ALIGN_RIGHT, mText.getId());
            mText.setText("‏" + mText.getText());
            mOver.setText(mText.getText());
        }
        else {
            ((RelativeLayout.LayoutParams) mOver.getLayoutParams()).addRule(RelativeLayout.ALIGN_LEFT, mText.getId());
            mText.setText("‎" + mText.getText());
            mOver.setText(mText.getText());
        }
    }

    public int getPosition(){
        return mOver.getMeasuredWidth();
    }

    public void setPosition(int position){
        clearAnimation();
        mOver.getLayoutParams().width = position;
        mOver.requestLayout();
    }

    public void animatePositionTo(int position, long duration){
        if(getAnimation() == null){
            WidthAnimation expand = new WidthAnimation(mOver, position);
            expand.setDuration(duration);
            expand.setInterpolator(LINEAR);
            startAnimation(expand);
        }
    }

    public void setTextSize(int size){
        mText.setTextSize(size);
        mOver.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    public void setTextColor(int color){
        mText.setTextColor(color);
    }

    public void setStrokeColor(int color){
        mText.setStrokeColor(color);
    }

    public void setHighlightColor(int color){
        mOver.setTextColor(color);
    }

    public void setText(String s){
        mText.setText(s);
        mOver.setText(s);
    }

    public String getText(){
        return mText.getText().toString();
    }

    /**
     * Used for untimed lines
     * @param untimed
     */
    public void setUntimed(boolean untimed){
        mText.setSingleLine(!untimed);
        mText.setGravity(untimed ? Gravity.CENTER_HORIZONTAL : Gravity.NO_GRAVITY);
        mOver.setVisibility(untimed ? INVISIBLE : VISIBLE);
    }

    public long getStart() {
        return mStart;
    }

    public void setStart(int start) {
        this.mStart = start;
    }

    public long getEnd() {
        return mEnd;
    }

    public void setEnd(int end) {
        this.mEnd = end;
    }
}