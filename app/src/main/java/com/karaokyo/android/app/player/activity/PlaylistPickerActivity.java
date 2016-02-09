package com.karaokyo.android.app.player.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.fragment.PlaylistsFragment;
import com.karaokyo.android.app.player.model.Playlist;
import com.karaokyo.android.app.player.model.Song;
import com.karaokyo.android.app.player.service.LyricService;
import com.karaokyo.android.app.player.util.Constants;
import com.karaokyo.android.app.player.util.Utilities;

import java.io.IOException;

public class PlaylistPickerActivity extends SelfClosingActivity implements
    PlaylistsFragment.OnFragmentInteractionListener {

    private static final String TAG = "PlaylistPickerActvity";

    private LyricService mLyricService;
    private boolean mBound = false;

    private ServiceConnection mLyricConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LyricService.LyricBinder binder = (LyricService.LyricBinder)service;
            mLyricService = binder.getService();
            mBound = true;
            Log.i(TAG, "Service Bound");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLyricService = null;
            mBound = false;
            Log.i(TAG, "Service Disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_picker);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, PlaylistsFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, LyricService.class), mLyricConnection, Context.BIND_AUTO_CREATE);

        getTracker().setScreenName("Playlist Picker");
        getTracker().send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mLyricConnection);
    }

    @Override
    public void onPlaylistsFragmentInteraction(Playlist playlist, boolean isLongClick) {
        Song song = (Song) getIntent().getSerializableExtra(Constants.KEY_SONG);

        if(mBound){
            try {
                mLyricService.loadPlaylist(playlist.getTitle());
                mLyricService.addSong(song);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        else{
            Utilities.showError(this, R.string.error_unable_to_add_song);
        }

        finish();
    }
}
