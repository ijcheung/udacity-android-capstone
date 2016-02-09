package com.karaokyo.android.app.player.activity;

import android.os.Bundle;

import com.google.android.gms.analytics.HitBuilders;
import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.fragment.settings.SettingsFragment;

public class SettingsActivity extends SelfClosingActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        getTracker().setScreenName("Settings");
        getTracker().send(new HitBuilders.ScreenViewBuilder().build());
    }
}