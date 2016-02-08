package com.karaokyo.android.app.player.fragment.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.lib.preference.colorpicker.ColorPickerPreference;

public class DisplayPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_display);

        /*findPreference(getString(R.string.pref_key_text_size)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary(o.toString());
                return true;
            }
        });
        findPreference(getString(R.string.pref_key_text_color)).setOnPreferenceChangeListener(sOnColorChangeListener);
        findPreference(getString(R.string.pref_key_text_highlight_color)).setOnPreferenceChangeListener(sOnColorChangeListener);
        findPreference(getString(R.string.pref_key_text_stroke_color)).setOnPreferenceChangeListener(sOnColorChangeListener);*/
    }

    private static Preference.OnPreferenceChangeListener sOnColorChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            Log.i("COLOR", value.toString());
            ColorPickerPreference colorPreference = (ColorPickerPreference) preference;
            colorPreference.setDisplayColor(Integer.parseInt(value.toString()));
            return true;
        }
    };
}