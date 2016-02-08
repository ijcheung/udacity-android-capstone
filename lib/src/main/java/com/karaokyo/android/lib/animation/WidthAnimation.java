package com.karaokyo.android.lib.animation;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class WidthAnimation extends Animation {
    private int mWidth;
    private int mStartWidth;
    private View mView;

    public WidthAnimation(View view, int width)
    {
        mView = view;
        mWidth = width;
        mStartWidth = view.getWidth();
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t)
    {
        int newWidth = mStartWidth + (int) ((mWidth - mStartWidth) * interpolatedTime);

        mView.getLayoutParams().width = newWidth;
        mView.requestLayout();
    }

    @Override
    public boolean willChangeBounds()
    {
        return true;
    }
}
