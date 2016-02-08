package com.karaokyo.android.app.player.fragment.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.karaokyo.android.app.player.R;

public class LegalPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_legal);
    }
}