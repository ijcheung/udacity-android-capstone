package com.karaokyo.android.app.player.fragment;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.activity.MainActivity;
import com.karaokyo.android.app.player.helper.SongAdapter;
import com.karaokyo.android.app.player.model.Song;
import com.karaokyo.android.app.player.service.LyricService;
import com.karaokyo.android.app.player.util.Utilities;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlaylistFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class PlaylistFragment extends Fragment implements
        DragSortListView.DropListener {
    private static final String TAG = "PlaylistFragment";

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String ARG_CURRENT_POSITION = "current";
    private static final String ARG_DURATION = "duration";
    private static final String ARG_IS_PLAYING = "playing";

    private ArrayList<Song> songs;
    private LyricService lyricService;
    private DragSortListView playlist;
    private SongAdapter mSongAdapter;
    private boolean mBound;
    private boolean mTracking = false;

    private Handler mHandler;

    private ImageButton mBackButton;
    private ImageButton mPlayButton;
    private ImageButton mForwardButton;

    private TextView mCurrentTime;
    private SeekBar mSeekBar;
    private TextView mTotalTime;

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if(mBound && lyricService.isSongLoaded()) {
                int duration = lyricService.getDuration();
                int currentPosition = lyricService.getCurrentPosition();

                mCurrentTime.setText(Utilities.milliSecondsToTimer(currentPosition));
                mTotalTime.setText(Utilities.milliSecondsToTimer(duration));

                mSeekBar.setProgress(currentPosition);
                mSeekBar.setMax(duration);

                if(lyricService.isPlaying()){
                    mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
                }
                else{
                    mPlayButton.setImageResource(android.R.drawable.ic_media_play);
                }

                if(mSongAdapter.getActiveSong() != lyricService.getSongIndex()){
                    Log.i(TAG, "setActiveSong: " + lyricService.getSongIndex());
                    mSongAdapter.setActiveSong(lyricService.getSongIndex());
                    mSongAdapter.notifyDataSetChanged();
                }
            }
            else{
                mCurrentTime.setText(R.string.timer_zero);
                mTotalTime.setText(R.string.timer_zero);
                mSeekBar.setProgress(0);
                mSeekBar.setMax(0);
            }

            mHandler.postDelayed(this, 100);
        }
    };

    private ServiceConnection lyricConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LyricService.LyricBinder binder = (LyricService.LyricBinder)service;
            lyricService = binder.getService();
            songs = lyricService.getSongs();
            mSongAdapter = new SongAdapter(getActivity(), songs, true);
            playlist.setAdapter(mSongAdapter);
            mBound = true;
            Log.i(TAG, "Service Bound");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            lyricService = null;
            Log.i(TAG, "Service Disconnected");
        }
    };

    public static PlaylistFragment newInstance(int sectionNumber) {
        PlaylistFragment fragment = new PlaylistFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public static PlaylistFragment newInstance(int currentPosition, int duration, boolean isPlaying, int sectionNumber) {
        PlaylistFragment fragment = new PlaylistFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CURRENT_POSITION, currentPosition);
        args.putInt(ARG_DURATION, duration);
        args.putBoolean(ARG_IS_PLAYING, isPlaying);
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public PlaylistFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        playlist = (DragSortListView) getView().findViewById(R.id.playlist);

        mBackButton = (ImageButton) getView().findViewById(R.id.back);
        mPlayButton = (ImageButton) getView().findViewById(R.id.play);
        mForwardButton = (ImageButton) getView().findViewById(R.id.forward);

        mCurrentTime = (TextView) getView().findViewById(R.id.currentTime);
        mSeekBar = (SeekBar) getView().findViewById(R.id.seekBar);
        mTotalTime = (TextView) getView().findViewById(R.id.totalTime);

        songs = new ArrayList<Song>();

        if (getArguments() != null) {
            int duration = getArguments().getInt(ARG_DURATION);
            int currentPosition = getArguments().getInt(ARG_CURRENT_POSITION);

            mCurrentTime.setText(Utilities.milliSecondsToTimer(currentPosition));
            mTotalTime.setText(Utilities.milliSecondsToTimer(duration));

            mSeekBar.setProgress(currentPosition);
            mSeekBar.setMax(duration);

            if(getArguments().getBoolean(ARG_IS_PLAYING)){
                mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
            }
            else{
                mPlayButton.setImageResource(android.R.drawable.ic_media_play);
            }
        }
        else {
            mCurrentTime.setText(R.string.timer_zero);
            mTotalTime.setText(R.string.timer_zero);
            mSeekBar.setProgress(0);
            mSeekBar.setMax(0);
        }

        mSongAdapter = new SongAdapter(getActivity(), songs);
        playlist.setAdapter(mSongAdapter);
        playlist.setDropListener(this);
        playlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                if(mBound){
                    mHandler.post(new Runnable(){
                        @Override
                        public void run() {
                            lyricService.setSong(position);
                        }
                    });
                    mSongAdapter.setActiveSong(position);
                    mSongAdapter.notifyDataSetChanged();
                }
            }
        });
        playlist.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                Log.i(TAG, "onItemLongClick, Index: " + Integer.toString(position));
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(songs.get(position).getTitle())
                        .setItems(R.array.playlist_options, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i(TAG, "onItemLongClick, Option: " + Integer.toString(which));
                                if(mBound) {
                                    switch (which) {
                                        //Play
                                        case 0:
                                            lyricService.setSong(position);
                                            break;
                                        //Remove
                                        case 1:
                                            int current = lyricService.getSongIndex();

                                            if(current > position){
                                                mSongAdapter.setActiveSong(current - 1);
                                            }

                                            lyricService.removeSong(position);
                                            mSongAdapter.notifyDataSetChanged();
                                            break;
                                    }
                                }
                            }
                        });
                builder.create().show();
                return true;
            }
        });

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBound) {
                    lyricService.doBack();
                }
            }
        });
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBound) {
                    if (lyricService.isPlaying()) {
                        lyricService.doPause();
                    } else {
                        lyricService.doPlay();
                    }
                }
            }
        });
        mForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBound) {
                    lyricService.doForward();
                }
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(mTracking && mBound && lyricService.isSongLoaded()) {
                    mCurrentTime.setText(Utilities.milliSecondsToTimer(i));
                    lyricService.seekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mTracking = true;
                mHandler.removeCallbacks(mUpdateTimeTask);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mTracking = false;
                mHandler.postDelayed(mUpdateTimeTask, 100);
            }
        });

        mHandler = new Handler();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        ((MainActivity) context).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().bindService(new Intent(getActivity(), LyricService.class), lyricConnection, Context.BIND_AUTO_CREATE);
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unbindService(lyricConnection);
        lyricService = null;
        mBound = false;
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void drop(int from, int to) {
        Log.i(TAG, "drop, from:" + from + ", to:" + to);
        if(from != to) {
            songs.add(to, songs.remove(from));
            mSongAdapter.notifyDataSetChanged();

            if (mBound) {
                lyricService.savePlaylist();

                int current = lyricService.getSongIndex();

                if(from < current && to >= current){
                    lyricService.setSongIndex(current - 1);
                    mSongAdapter.setActiveSong(current - 1);
                }
                else if(from == current){
                    lyricService.setSongIndex(to);
                    mSongAdapter.setActiveSong(to);
                }
                else if(from > current && to <= current){
                    lyricService.setSongIndex(current + 1);
                    mSongAdapter.setActiveSong(current + 1);
                }
            }
        }
    }
}
