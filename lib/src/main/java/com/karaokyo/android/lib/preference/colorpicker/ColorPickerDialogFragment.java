package com.karaokyo.android.lib.preference.colorpicker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.karaokyo.android.lib.R;

public class ColorPickerDialogFragment extends DialogFragment {
    private static final String ARG_COLOR = "color";
    private static final String ARG_SUPPORTS_ALPHA = "supports_alpha";
    private static final String ARG_PREFERENCE = "preference";

    private static final String KEY_NEW_COLOR = "newColor";
    private static final String KEY_NEW_HUE = "newHue";

    private boolean supportsAlpha;
    private ColorPickerPreference preference;
    private View view;
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

    public static ColorPickerDialogFragment newInstance(int color, boolean supportsAlpha, ColorPickerPreference preference) {
        ColorPickerDialogFragment fragment = new ColorPickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLOR, color);
        args.putBoolean(ARG_SUPPORTS_ALPHA, supportsAlpha);
        //args.putSerializable(ARG_PREFERENCE, preference);
        fragment.setArguments(args);
        return fragment;
    }

    public ColorPickerDialogFragment(){}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        isRtl = ViewCompat.getLayoutDirection(getView()) == ViewCompat.LAYOUT_DIRECTION_RTL;
        AlertDialog dialog = null;
        if (getArguments() == null) {
            throw new NullPointerException("ColorPickerFragment should be instantiated via newInstance()");
        }
        else {
            color = getArguments().getInt(ARG_COLOR);
            supportsAlpha = getArguments().getBoolean(ARG_SUPPORTS_ALPHA);
            preference = (ColorPickerPreference) getArguments().getSerializable(ARG_PREFERENCE);

            if (!supportsAlpha) { // remove alpha if not supported
                color = color | 0xff000000;
            }

            dialog = new AlertDialog.Builder(getActivity())
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            preference.setValue(getColor());
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .create();

            dialog.setCanceledOnTouchOutside(false);

            view = dialog.getLayoutInflater().inflate(R.layout.colorpicker_dialog, null);
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

            { // hide/show alpha
                viewAlphaOverlay.setVisibility(supportsAlpha?View.VISIBLE:View.GONE);
                viewAlphaCursor.setVisibility(supportsAlpha?View.VISIBLE:View.GONE);
                viewAlphaCheckered.setVisibility(supportsAlpha?View.VISIBLE:View.GONE);
            }

            final int newColor;
            final float newHue;
            if(savedInstanceState != null){
                newColor = savedInstanceState.getInt(KEY_NEW_COLOR);
                Color.colorToHSV(newColor, currentColorHsv);
                newHue = savedInstanceState.getFloat(KEY_NEW_HUE);
            }
            else{
                newColor = color;
                Color.colorToHSV(newColor, currentColorHsv);
                newHue = getHue();
            }

            alpha = Color.alpha(newColor);
            viewSatVal.setHue(newHue);
            setHue(newHue);
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
                    if (ColorPickerDialogFragment.this.supportsAlpha) moveAlphaCursor();
                    moveTarget();
                    if (ColorPickerDialogFragment.this.supportsAlpha) updateAlphaView();
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });

            dialog.setView(view);
        }

        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_NEW_COLOR, getColor());
        outState.putFloat(KEY_NEW_HUE, getHue());
        super.onSaveInstanceState(outState);
    }

    protected void moveCursor() {
        float y = viewHue.getMeasuredHeight() - (getHue() * viewHue.getMeasuredHeight() / 360.f);
        if (y == viewHue.getMeasuredHeight()) y = 0.f;
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewCursor.getLayoutParams();
        layoutParams.leftMargin = (int) (viewHue.getLeft() - Math.floor(viewCursor.getMeasuredWidth() / 2) - viewContainer.getPaddingLeft());
        layoutParams.topMargin = (int) (viewHue.getTop() + y - Math.floor(viewCursor.getMeasuredHeight() / 2) - viewContainer.getPaddingTop());
        viewCursor.setLayoutParams(layoutParams);
    }

    protected void moveTarget() {
        float x = getSat() * viewSatVal.getMeasuredWidth();
        float y = (1.f - getVal()) * viewSatVal.getMeasuredHeight();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewTarget.getLayoutParams();
        if(isRtl){
            layoutParams.rightMargin = (int) (viewSatVal.getRight() + x - Math.floor(viewTarget.getMeasuredWidth() / 2) - viewContainer.getPaddingRight());
        }
        else {
            layoutParams.leftMargin = (int) (viewSatVal.getLeft() + x - Math.floor(viewTarget.getMeasuredWidth() / 2) - viewContainer.getPaddingLeft());
        }
        layoutParams.topMargin = (int) (viewSatVal.getTop() + y - Math.floor(viewTarget.getMeasuredHeight() / 2) - viewContainer.getPaddingTop());
        viewTarget.setLayoutParams(layoutParams);
    }

    protected void moveAlphaCursor() {
        final int measuredHeight = this.viewAlphaCheckered.getMeasuredHeight();
        float y = measuredHeight - ((this.getAlpha() * measuredHeight) / 255.f);
        final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.viewAlphaCursor.getLayoutParams();
        if(isRtl){
            layoutParams.rightMargin = (int) (this.viewAlphaCheckered.getRight() - Math.floor(this.viewAlphaCursor.getMeasuredWidth() / 2) - this.viewContainer.getPaddingRight());
        }
        else {
            layoutParams.leftMargin = (int) (this.viewAlphaCheckered.getLeft() - Math.floor(this.viewAlphaCursor.getMeasuredWidth() / 2) - this.viewContainer.getPaddingLeft());
        }
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