package com.karaokyo.android.lib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.karaokyo.android.lib.R;

public class StrokedTextView extends RelativeLayout {
    private TextView mTextView;
    private TextView mStrokeView;

    public StrokedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mStrokeView = new TextView(context, attrs);
        mTextView = new TextView(context, attrs);

        addView(mStrokeView);
        addView(mTextView);

        TextPaint paint = mStrokeView.getPaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);

        setClipChildren(false);

        TypedArray appearance = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.StrokedTextView,
                0, 0);

        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);
                if(attr == R.styleable.StrokedTextView_strokeWidth)
                    setStrokeWidth(appearance.getInt(attr, 0));
                else if(attr == R.styleable.StrokedTextView_strokeColor){
                    setStrokeColor(appearance.getColor(attr, 0));
                }
            }
            appearance.recycle();
        }
    }

    public void setText(CharSequence text){
        mTextView.setText(text);
        mStrokeView.setText(text);
    }

    public CharSequence getText(){
        return mTextView.getText();
    }

    public void setTextSize(int size){
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mStrokeView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    public void setTextColor(int color) {
        mTextView.setTextColor(color);
    }

    public void setStrokeColor(int color) {
        mStrokeView.setTextColor(color);
    }

    public void setStrokeWidth(float width) {
        TextPaint paint = mStrokeView.getPaint();
        paint.setStrokeWidth(width);
    }

    public void setGravity(int gravity){
        mTextView.setGravity(gravity);
        mStrokeView.setGravity(gravity);
    }

    public void setSingleLine(boolean singleLine){
        mTextView.setSingleLine(singleLine);
        mStrokeView.setSingleLine(singleLine);
    }

    /*private void initialize(Context context, AttributeSet attrs){
        /*mLayout = LayoutInflater.from(context).inflate(R.layout.view_strokedtext, null);
        mTextView = (TextView) mLayout.findViewById(R.id.text);
        mStrokeView = (TextView) mLayout.findViewById(R.id.stroke);

        mLayout = new RelativeLayout(context);
        mStrokeView = new TextView(context, attrs);
        mTextView = new TextView(context, attrs);

        mLayout.addView(mStrokeView);
        mLayout.addView(mTextView);

        TextPaint paint = mStrokeView.getPaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);

        setClipChildren(false);

        addView(mLayout);
    }*/
}