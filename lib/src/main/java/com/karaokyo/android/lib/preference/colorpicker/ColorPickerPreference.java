package com.karaokyo.android.lib.preference.colorpicker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.preference.DialogPreference;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.karaokyo.android.lib.R;

public class ColorPickerPreference extends DialogPreference{
    private static final String TAG = "ColorPickerPreference";

    private View displayColor;

    private boolean supportsAlpha;

    private int newColor;
    private Float newHue;

    private View viewHue;
    private ColorPickerSquare viewSatVal;
    private ImageView viewCursor;
    private ImageView viewAlphaCursor;
    private View viewOldColor;
    private View viewNewColor;
    private View viewAlphaOverlay;
    private ImageView viewTarget;
    private ImageView viewAlphaCheckered;
    private ViewGroup viewContainer;
    private final float[] currentColorHsv = new float[3];
    private int color;
    private int alpha;
    private boolean isRtl;

    private int buttonPress;

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ColorPickerPreference);
        supportsAlpha = ta.getBoolean(R.styleable.ColorPickerPreference_supportsAlpha, false);
        ta.recycle();

        setWidgetLayoutResource(R.layout.colorpicker_preference);
        setDialogLayoutResource(R.layout.colorpicker_dialog);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        // Set our custom views inside the layout
        displayColor = view.findViewById(R.id.colorpicker_pref_widget_box);
        setDisplayColor(color);
    }

	/*@Override
    protected void onClick() {
        new ColorPickerDialogFragment().newInstance(value, supportsAlpha, this).show(mFragmentManager, "tag");
	}*/

    @Override
    public void onClick(DialogInterface dialog, int which) {
        buttonPress = which;
    }

    @Override
    protected View onCreateDialogView() {
        final View view = super.onCreateDialogView();

        Log.i(TAG, "onCreateDialogView");

        isRtl = ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
        viewHue = view.findViewById(R.id.colorpicker_viewHue);
        viewSatVal = (ColorPickerSquare) view.findViewById(R.id.colorpicker_viewSatBri);
        viewCursor = (ImageView) view.findViewById(R.id.colorpicker_cursor);
        viewOldColor = view.findViewById(R.id.colorpicker_oldColor);
        viewNewColor = view.findViewById(R.id.colorpicker_newColor);
        viewTarget = (ImageView) view.findViewById(R.id.colorpicker_target);
        viewContainer = (ViewGroup) view.findViewById(R.id.colorpicker_viewContainer);
        viewAlphaOverlay = view.findViewById(R.id.colorpicker_overlay);
        viewAlphaCursor = (ImageView) view.findViewById(R.id.colorpicker_alphaCursor);
        viewAlphaCheckered = (ImageView) view.findViewById(R.id.colorpicker_alphaCheckered);

        ImageView arrow = (ImageView) view.findViewById(R.id.arrow);
        DrawableCompat.setAutoMirrored(arrow.getDrawable(), true);

        // hide/show alpha
        viewAlphaOverlay.setVisibility(supportsAlpha? View.VISIBLE: View.GONE);
        viewAlphaCursor.setVisibility(supportsAlpha? View.VISIBLE: View.GONE);
        viewAlphaCheckered.setVisibility(supportsAlpha? View.VISIBLE: View.GONE);

        if (!supportsAlpha) { // remove alpha if not supported
            color = color | 0xff000000;
        }

        Color.colorToHSV(newColor, currentColorHsv);
        alpha = Color.alpha(newColor);

        if(newHue == null){
            viewSatVal.setHue(getHue());
        }
        else{
            viewSatVal.setHue(newHue);
            setHue(newHue);
        }
        viewOldColor.setBackgroundColor(color);
        viewNewColor.setBackgroundColor(newColor);

        viewHue.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_UP) {

                    float y = event.getY();
                    if (y < 0.f) y = 0.f;
                    if (y > viewHue.getMeasuredHeight()) {
                        y = viewHue.getMeasuredHeight() - 0.001f; // to avoid jumping the cursor from bottom to top.
                    }
                    float hue = 360.f - 360.f / viewHue.getMeasuredHeight() * y;
                    if (hue == 360.f) hue = 0.f;
                    setHue(hue);

                    // update view
                    viewSatVal.setHue(getHue());
                    moveCursor();
                    viewNewColor.setBackgroundColor(getColor());
                    updateAlphaView();
                    return true;
                }
                return false;
            }
        });

        if (supportsAlpha) viewAlphaCheckered.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() == MotionEvent.ACTION_MOVE)
                        || (event.getAction() == MotionEvent.ACTION_DOWN)
                        || (event.getAction() == MotionEvent.ACTION_UP)) {

                    float y = event.getY();
                    if (y < 0.f) {
                        y = 0.f;
                    }
                    if (y > viewAlphaCheckered.getMeasuredHeight()) {
                        y = viewAlphaCheckered.getMeasuredHeight() - 0.001f; // to avoid jumping the cursor from bottom to top.
                    }
                    final int a = Math.round(255.f - ((255.f / viewAlphaCheckered.getMeasuredHeight()) * y));
                    setAlpha(a);

                    // update view
                    moveAlphaCursor();
                    int col = getColor();
                    int c = a << 24 | col & 0x00ffffff;
                    viewNewColor.setBackgroundColor(c);
                    return true;
                }
                return false;
            }
        });
        viewSatVal.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE
                        || event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_UP) {

                    float x = event.getX(); // touch event are in dp units.
                    float y = event.getY();

                    if (x < 0.f) x = 0.f;
                    if (x > viewSatVal.getMeasuredWidth()) x = viewSatVal.getMeasuredWidth();
                    if (y < 0.f) y = 0.f;
                    if (y > viewSatVal.getMeasuredHeight()) y = viewSatVal.getMeasuredHeight();

                    setSat(1.f / viewSatVal.getMeasuredWidth() * x);
                    setVal(1.f - (1.f / viewSatVal.getMeasuredHeight() * y));

                    // update view
                    moveTarget();
                    viewNewColor.setBackgroundColor(getColor());

                    return true;
                }
                return false;
            }
        });

        // move cursor & target on first draw
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                moveCursor();
                if (supportsAlpha) moveAlphaCursor();
                moveTarget();
                if (supportsAlpha) updateAlphaView();
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(buttonPress == DialogInterface.BUTTON_POSITIVE){
            setValue(getColor());
            newColor = getColor();
        }
        else{
            newColor = color;
        }
        Color.colorToHSV(newColor, currentColorHsv);
        alpha = Color.alpha(newColor);
        viewSatVal.setHue(getHue());
    }

    /*@Override
    protected Parcelable onSaveInstanceState() {
        Log.i(TAG, "onSaveInstanceState");
        Log.i(TAG, Utilities.intToColorCode(getColor()) + " " + getHue());

        final SavedState myState = new SavedState(super.onSaveInstanceState());
        myState.newColor = getColor();
        myState.newHue = getHue();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Log.i(TAG, "onRestoreInstanceState");

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        this.newColor = myState.newColor;
        this.newHue = myState.newHue;

        Log.i(TAG, Utilities.intToColorCode(newColor) + " " + newHue);
        notifyChanged();
    }

    private static class SavedState extends BaseSavedState {
        int newColor;
        float newHue;

        public SavedState(Parcel source) {
            super(source);
            newColor = source.readInt();
            newHue = source.readFloat();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(newColor);
            dest.writeFloat(newHue);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }*/

    public void setValue(int value) {
        if(callChangeListener(value)) {
            color = value;
            persistInt(value);
            notifyChanged();
        }
    }

    public void setDisplayColor(int value){
        if (displayColor != null) {
            displayColor.setBackgroundColor(value);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // This preference type's value type is Integer, so we read the default value from the attributes as an Integer.
        return a.getInteger(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        Log.i(TAG, "onSetInitialValue");
        if (restoreValue) { // Restore state
            color = getPersistedInt(color);
        } else { // Set state
            int value = (Integer) defaultValue;
            color = value;
            persistInt(value);
        }
        newColor = color;
        /*Color.colorToHSV(color, currentColorHsv);
        newColor = color;
        alpha = Color.alpha(color);
        Log.i(TAG, Utilities.intToColorCode(newColor));*/
    }

    protected void moveCursor() {
        float y = viewHue.getMeasuredHeight() - (getHue() * viewHue.getMeasuredHeight() / 360.f);
        if (y == viewHue.getMeasuredHeight()) y = 0.f;
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewCursor.getLayoutParams();
        //layoutParams.leftMargin = (int) (viewHue.getLeft() - Math.floor(viewCursor.getMeasuredWidth() / 2) - viewContainer.getPaddingLeft());
        layoutParams.topMargin = (int) (viewHue.getTop() + y - Math.floor(viewCursor.getMeasuredHeight() / 2) - viewContainer.getPaddingTop());
        viewCursor.setLayoutParams(layoutParams);
    }

    protected void moveTarget() {
        float x = getSat() * viewSatVal.getMeasuredWidth();
        float y = (1.f - getVal()) * viewSatVal.getMeasuredHeight();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewTarget.getLayoutParams();
        //layoutParams.leftMargin = (int) (viewSatVal.getLeft() + x - Math.floor(viewTarget.getMeasuredWidth() / 2) - viewContainer.getPaddingLeft());
        layoutParams.leftMargin = (int) (x - Math.floor(viewTarget.getMeasuredWidth() / 2));
        layoutParams.topMargin = (int) (viewSatVal.getTop() + y - Math.floor(viewTarget.getMeasuredHeight() / 2) - viewContainer.getPaddingTop());
        viewTarget.setLayoutParams(layoutParams);
    }

    protected void moveAlphaCursor() {
        final int measuredHeight = this.viewAlphaCheckered.getMeasuredHeight();
        float y = measuredHeight - ((this.getAlpha() * measuredHeight) / 255.f);
        final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.viewAlphaCursor.getLayoutParams();
        //layoutParams.leftMargin = (int) (this.viewAlphaCheckered.getLeft() - Math.floor(this.viewAlphaCursor.getMeasuredWidth() / 2) - this.viewContainer.getPaddingLeft());
        layoutParams.topMargin = (int) ((this.viewAlphaCheckered.getTop() + y) - Math.floor(this.viewAlphaCursor.getMeasuredHeight() / 2) - this.viewContainer.getPaddingTop());

        this.viewAlphaCursor.setLayoutParams(layoutParams);
    }

    private int getColor() {
        final int argb = Color.HSVToColor(currentColorHsv);
        return alpha << 24 | (argb & 0x00ffffff);
    }

    private float getHue() {
        return currentColorHsv[0];
    }

    private float getAlpha() {
        return this.alpha;
    }

    private float getSat() {
        return currentColorHsv[1];
    }

    private float getVal() {
        return currentColorHsv[2];
    }

    private void setHue(float hue) {
        currentColorHsv[0] = hue;
    }

    private void setSat(float sat) {
        currentColorHsv[1] = sat;
    }

    private void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    private void setVal(float val) {
        currentColorHsv[2] = val;
    }

    private void updateAlphaView() {
        final GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {
                Color.HSVToColor(currentColorHsv), 0x0
        });
        viewAlphaOverlay.setBackground(gd);
    }
}