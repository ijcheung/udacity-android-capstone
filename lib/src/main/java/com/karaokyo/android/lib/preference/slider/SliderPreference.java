package com.karaokyo.android.lib.preference.slider;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.karaokyo.android.lib.R;

public class SliderPreference extends DialogPreference {
    private final static String TAG = "SliderPreference";

    private int initialValue;
    private int value;
    private int min;
    private int max;
    private int buttonPress;

    /**
     * @param context
     * @param attrs
     */
    public SliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SliderPreference);
        min = ta.getInt(R.styleable.SliderPreference_min, 0);
        max = ta.getInt(R.styleable.SliderPreference_max, 100);
        ta.recycle();
        setDialogLayoutResource(R.layout.slider_preference);
    }

    public void setValue(int value) {
        if(callChangeListener(value)) {
            Log.i(TAG, "setValue: " + value);
            Log.i(TAG, this.value + " " + this.initialValue);
            this.value = value;
            persistInt(value);
            notifyChanged();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        buttonPress = which;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        Log.i(TAG, "onSetInitialValue: " + restoreValue + "," + defaultValue);
        if (restoreValue) { // Restore state
            value = getPersistedInt(value);
        } else { // Set state
            int value = (Integer) defaultValue;
            this.value = value;
            persistInt(value);
        }
        initialValue = value;
    }

    @Override
    protected View onCreateDialogView() {
        Log.i(TAG, "onCreateDialogView");
        View view = super.onCreateDialogView();

        if(getPersistedInt(value) != value){
            Log.i(TAG, "changeOutsideOfPreferencesActivityDetected");
            value = getPersistedInt(value);
            initialValue = value;
        }

        buttonPress = 0;

        SeekBar seekbar = (SeekBar) view.findViewById(R.id.slider_preference_seekbar);
        seekbar.setMax(max - min);
        seekbar.setProgress(value - min);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setValue(progress + min);
                }
            }
        });
        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        Log.i(TAG, "onDialogClosed: " + positiveResult);
        Log.i(TAG, "buttonPress: " + buttonPress);
        switch(buttonPress){
            case DialogInterface.BUTTON_POSITIVE:
                initialValue = value;
                break;
            default:
                setValue(initialValue);
        }
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Log.i(TAG, "onSaveInstanceState");
        Log.i(TAG, this.value + " " + this.initialValue);

        final SavedState myState = new SavedState(super.onSaveInstanceState());
        myState.value = value;
        myState.initialValue = initialValue;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Log.i(TAG, "onRestoreInstanceState");

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        this.value = myState.value;
        this.initialValue = myState.initialValue;
        Log.i(TAG, this.value + " " + this.initialValue);
        notifyChanged();
    }

    private static class SavedState extends BaseSavedState {
        int value;
        int initialValue;

        public SavedState(Parcel source) {
            super(source);
            value = source.readInt();
            initialValue = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(value);
            dest.writeInt(initialValue);
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
    }
}