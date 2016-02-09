package com.karaokyo.android.app.player.activity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.analytics.HitBuilders;
import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.fragment.LibraryFragment;
import com.karaokyo.android.app.player.model.Song;
import com.karaokyo.android.app.player.util.Constants;

public class AudioPickerActivity extends SelfClosingActivity implements
    LibraryFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_picker);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, LibraryFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        getTracker().setScreenName("Audio Picker");
        getTracker().send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onLibraryFragmentInteraction(Song song) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.KEY_SONG_ID, song.getSongId());
        resultIntent.putExtra(Constants.KEY_LYRIC_FILE_PATH, this.getIntent().getStringExtra(Constants.KEY_LYRIC_FILE_PATH));
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
