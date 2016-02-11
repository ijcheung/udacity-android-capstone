package com.karaokyo.android.app.player.service;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.activity.MainActivity;
import com.karaokyo.android.app.player.helper.AudioFocusHelper;
import com.karaokyo.android.app.player.helper.MediaButtonHelper;
import com.karaokyo.android.app.player.helper.MusicFocusable;
import com.karaokyo.android.app.player.helper.MusicIntentReceiver;
import com.karaokyo.android.app.player.helper.RemoteControlClientCompat;
import com.karaokyo.android.app.player.helper.RemoteControlHelper;
import com.karaokyo.android.app.player.model.Line;
import com.karaokyo.android.app.player.model.Song;
import com.karaokyo.android.app.player.model.Transition;
import com.karaokyo.android.app.player.task.LyricLayoutTask;
import com.karaokyo.android.app.player.util.Constants;
import com.karaokyo.android.app.player.util.Utilities;
import com.karaokyo.android.app.player.view.LineView;
import com.karaokyo.android.app.player.widget.ControlWidget;
import com.karaokyo.android.lib.widget.StrokedTextView;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LyricService extends Service implements
        MusicFocusable,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "LyricService";
    private static final int NOTIFY_ID = 1;

    private WindowManager mWindowManager;
    private BroadcastReceiver mReceiver;
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mEditor;
    private NotificationCompat.Builder mBuilder;
    private RemoteViews mRemoteViews;
    private Bitmap mDummyAlbumArt;

    private String mTitle;
    private String mArtist;

    private List<LineView> mLines;
    private int mStartingIndex = 0;
    private boolean mJump;

    private View mOverlay;
    private StrokedTextView mTitleView;
    private StrokedTextView mArtistView;
    private LinearLayout mLyrics;
    private LyricLayoutTask mLyricLayoutTask;

    private int mTextSize;
    private int mTextColor;
    private int mStrokeColor;
    private int mHighlightColor;
    private int mStrokeWidth;

    private AlphaAnimation titleAnimation;
    private AlphaAnimation artistAnimation;

    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;

    private MediaPlayer mMediaPlayer;

    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;

    // indicates the state our service:
    enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
        // paused in this state if we don't have audio focus. But we stay in this state
        // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };

    private State mState = State.Stopped;

    // if in Retrieving mode, this flag indicates whether we should start playing immediately
    // when we are ready or not.
    private boolean mStartPlayingAfterRetrieve = true;
    private boolean mSongLoaded = false;

    //Misc State Variables
    private boolean mHasLyrics = false;
    private boolean mSeeking = false;
    private int mOrientation;
    private Element mRoot;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }
    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    // our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClientCompat mRemoteControlClientCompat;

    // The component name of MusicIntentReceiver, for use with media button and remote control
    // APIs
    ComponentName mMediaButtonReceiverComponent;

    AudioManager mAudioManager;

    private final IBinder lyricBinder = new LyricBinder();
    private ArrayList<Song> songs;
    private int index;

    private Handler mHandler;

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if(mHasLyrics){
                int current = getCurrentPosition();

                main:
                for(int i = mStartingIndex; i < mLines.size(); i++){
                    final LineView line = mLines.get(i);

                    switch(line.getVisibility()){
                        case View.GONE:
                            if(current < line.getStart() && !mJump){
                                break main;
                            }
                            if(current >= line.getStart() && current <= line.getEnd()){
                                line.setVisibility(View.VISIBLE);
                            }
                            break;
                        case View.VISIBLE:
                            if(current < line.getStart()){
                                line.clearAnimation();
                                line.setVisibility(View.GONE);
                            }
                            else if(current > line.getEnd()){
                                line.clearAnimation();
                                line.setVisibility(View.GONE);
                                if(i == mStartingIndex){
                                    mStartingIndex++;
                                }
                            }
                            break;
                    }

                    if(line.getVisibility() == View.VISIBLE){
                        List<Transition> transitions = (List<Transition>) line.getTag();
                        if(transitions != null){
                            Transition transition;
                            int j = 0;

                            for(; j < transitions.size(); j++){
                                transition = transitions.get(j);
                                if(transition.getStart() >= current){
                                    if(j == 0){
                                        line.setPosition(0);
                                    }
                                    else{
                                        line.setPosition(transitions.get(j - 1).getWidth());
                                    }
                                    break;
                                }
                                else if(transition.getEnd() <= current){
                                    if(j == transitions.size() - 1){
                                        line.setPosition(line.getWidth());
                                    }
                                }
                                else{
                                    int width = 0;
                                    if(j > 0){
                                        width = transitions.get(j - 1).getWidth();
                                    }
                                    width = (int) (width + ((transition.getWidth() - width) * (current - transition.getStart())/(transition.getEnd() - transition.getStart())));

                                    if(isPlaying()) {
                                        if(line.getAnimation() == null){
                                            if(width < line.getPosition()){
                                                line.animatePositionTo(width, 0);
                                            }
                                            line.animatePositionTo(transition.getWidth(), transition.getEnd() - current);
                                        }
                                    }
                                    else {
                                        line.setPosition(width);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
                mJump = false;
                /*for(int i = 0; i < mLyrics.getChildCount(); i++){
                    final LineView line = (LineView) mLyrics.getChildAt(i);

                    switch(line.getVisibility()){
                        case View.GONE:
                            if(current >= line.getStart() && current <= line.getEnd()){
                                line.setVisibility(View.VISIBLE);
                            }
                            break;
                        case View.VISIBLE:
                            if(current < line.getStart() || current > line.getEnd()){
                                line.clearAnimation();
                                line.setVisibility(View.GONE);
                            }
                            break;
                    }

                    if(line.getVisibility() == View.VISIBLE){
                        List<Transition> transitions = (List<Transition>) line.getTag();
                        if(transitions != null){
                            Transition transition;
                            int j = 0;

                            for(; j < transitions.size(); j++){
                                transition = transitions.get(j);
                                if(transition.getStart() >= current){
                                    if(j == 0){
                                        line.setPosition(0);
                                    }
                                    else{
                                        line.setPosition(transitions.get(j - 1).getWidth());
                                    }
                                    break;
                                }
                                else if(transition.getEnd() <= current){
                                    if(j == transitions.size() - 1){
                                        line.setPosition(line.getWidth());
                                    }
                                }
                                else{
                                    int width = 0;
                                    if(j > 0){
                                        width = transitions.get(j - 1).getWidth();
                                    }
                                    width = (int) (width + ((transition.getWidth() - width) * (current - transition.getStart())/(transition.getEnd() - transition.getStart())));

                                    if(isPlaying()) {
                                        if(line.getAnimation() == null){
                                            if(width < line.getPosition()){
                                                line.animatePositionTo(width, 0);
                                            }
                                            line.animatePositionTo(transition.getWidth(), transition.getEnd() - current);
                                        }
                                    }
                                    else {
                                        line.setPosition(width);
                                    }

                                    break;
                                }
                            }
                        }
                    }
                }*/
            }

            mHandler.postDelayed(this, 200);
        }
    };

    public class LyricBinder extends Binder {
        public LyricService getService() {
            return LyricService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return lyricBinder;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "Creating service");
        super.onCreate();

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mEditor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mMediaButtonReceiverComponent = new ComponentName(this, MusicIntentReceiver.class);
        mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.album_art);

        mSharedPref.registerOnSharedPreferenceChangeListener(this);
        createMediaPlayer();
        songs = new ArrayList<Song>();
        index = 0;

        mLines = new ArrayList<LineView>();

        mTextSize = mSharedPref.getInt(getString(R.string.pref_key_text_size), Constants.DEFAULT_TEXT_SIZE);
        mTextColor = mSharedPref.getInt(getString(R.string.pref_key_text_color), Constants.DEFAULT_TEXT_COLOR);
        mStrokeColor = mSharedPref.getInt(getString(R.string.pref_key_text_stroke_color), Constants.DEFAULT_STROKE_COLOR);
        mHighlightColor = mSharedPref.getInt(getString(R.string.pref_key_text_highlight_color), Constants.DEFAULT_HIGHLIGHT_COLOR);
        mStrokeWidth = calculateStrokeWidth();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        mOverlay = LayoutInflater.from(this).inflate(R.layout.overlay, null);
        mTitleView = (StrokedTextView) mOverlay.findViewById(R.id.title);
        mArtistView = (StrokedTextView) mOverlay.findViewById(R.id.artist);
        mLyrics = (LinearLayout) mOverlay.findViewById(R.id.lyrics);

        mTitleView.setTextSize((int) (mTextSize * 1.3));
        mTitleView.setTextColor(mTextColor);
        mTitleView.setStrokeColor(mStrokeColor);
        mTitleView.setStrokeWidth((int) (mStrokeWidth * 1.3));
        mArtistView.setTextSize((int) (mTextSize * 1.2));
        mArtistView.setTextColor(mTextColor);
        mArtistView.setStrokeColor(mStrokeColor);
        mArtistView.setStrokeWidth((int) (mStrokeWidth * 1.3));

        mWindowManager.addView(mOverlay, params);

        setupAnimations();

        //Notification Controls
        mRemoteViews = new RemoteViews(getPackageName(),
                R.layout.notification_control_bar);

        //Foreground Notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Action Intents
        //PendingIntent zoomOutPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION_ZOOM_OUT), 0);
        //PendingIntent zoomInPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION_ZOOM_IN), 0);
        PendingIntent backPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION_BACK), 0);
        PendingIntent togglePlaybackPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION_TOGGLE_PLAYBACK), 0);
        PendingIntent forwardPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION_FORWARD), 0);
        PendingIntent closePendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.ACTION_CLOSE), 0);

        //mRemoteViews.setOnClickPendingIntent(R.id.zoomOut, zoomOutPendingIntent);
        //mRemoteViews.setOnClickPendingIntent(R.id.zoomIn, zoomInPendingIntent);
        mRemoteViews.setOnClickPendingIntent(R.id.back, backPendingIntent);
        mRemoteViews.setOnClickPendingIntent(R.id.play, togglePlaybackPendingIntent);
        mRemoteViews.setOnClickPendingIntent(R.id.forward, forwardPendingIntent);
        mRemoteViews.setOnClickPendingIntent(R.id.close, closePendingIntent);

        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setContent(mRemoteViews);

        mRemoteViews.setTextViewText(R.id.title, "");
        mRemoteViews.setTextViewText(R.id.artist, "");
        mRemoteViews.setImageViewResource(R.id.play, R.drawable.ic_play);
        startForeground(NOTIFY_ID, mBuilder.build());

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int size;

                switch(intent.getAction()){
                    case Constants.ACTION_ZOOM_OUT:
                        size = mSharedPref.getInt(getString(R.string.pref_key_text_size), Constants.DEFAULT_TEXT_SIZE);
                        if(size > 50) {
                            mEditor.putInt(getString(R.string.pref_key_text_size), size - 1);
                            mEditor.commit();
                        }
                        break;
                    case Constants.ACTION_ZOOM_IN:
                        size = mSharedPref.getInt(getString(R.string.pref_key_text_size), Constants.DEFAULT_TEXT_SIZE);
                        if(size < 100) {
                            mEditor.putInt(getString(R.string.pref_key_text_size), size + 1);
                            mEditor.commit();
                        }
                        break;
                    case Constants.ACTION_BACK:
                        doBack();
                        break;
                    case Constants.ACTION_TOGGLE_PLAYBACK:
                        doPlayback();
                        break;
                    case Constants.ACTION_PAUSE:
                        doPause();
                        break;
                    case Constants.ACTION_FORWARD:
                        doForward();
                        break;
                    case Constants.ACTION_CLOSE:
                        mHandler.removeCallbacks(mUpdateTimeTask);
                        stopSelf();
                        break;
                    case Constants.ACTION_WIDGET_UPDATE_REQUEST:
                        updateControlWidget(intent);
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_ZOOM_IN);
        filter.addAction(Constants.ACTION_ZOOM_OUT);
        filter.addAction(Constants.ACTION_BACK);
        filter.addAction(Constants.ACTION_TOGGLE_PLAYBACK);
        filter.addAction(Constants.ACTION_PAUSE);
        filter.addAction(Constants.ACTION_FORWARD);
        filter.addAction(Constants.ACTION_CLOSE);
        filter.addAction(Constants.ACTION_WIDGET_UPDATE_REQUEST);

        registerReceiver(mReceiver, filter);

        mHandler = new Handler();

        //Load Playlist
        String currentPlaylist = mSharedPref.getString(getString(R.string.pref_key_current_playlist), null);
        if(currentPlaylist != null){
            try {
                loadPlaylist(currentPlaylist);
            } catch (IOException e) {
                //Ignore
            } catch (ClassNotFoundException e) {
                //Ignore
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        if(intent != null) {
            action = intent.getAction();
        }

        Log.i(TAG, "onStartCommand: " + action);
        if(action == null);
        else if (action.equals(Constants.ACTION_TOGGLE_PLAYBACK)) doPlayback();
        else if (action.equals(Constants.ACTION_FORWARD)) doForward();
        else if (action.equals(Constants.ACTION_BACK)) doBack();

        return START_NOT_STICKY;
    }

    /**
     * Signals that audio focus was gained.
     */
    @Override
    public void onGainedAudioFocus() {
        Log.i(TAG, "onGainedAudioFocus");
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    /**
     * Signals that audio focus was lost.
     *
     * @param canDuck If true, audio can continue in "ducked" mode (low volume). Otherwise, all
     *                audio must stop.
     */
    @Override
    public void onLostAudioFocus(boolean canDuck) {
        Log.i(TAG, "onLostAudioFocus");
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mMediaPlayer != null && mMediaPlayer.isPlaying())
            configAndStartMediaPlayer();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.i(TAG, "onCompletion");
        if(mMediaPlayer.getCurrentPosition() > 0){
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        Log.i(TAG, "onError");
        Utilities.showError(this, R.string.error_playback);
        playNext();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.i(TAG, "onPrepared");
        mSongLoaded = true;
        Song song = songs.get(index);
        mTitle = song.getTitle();
        mArtist = song.getArtist();
        updateNotification(mTitle, mArtist);
        if(mStartPlayingAfterRetrieve) {
            configAndStartMediaPlayer();
        }
        else{
            mState = State.Paused;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "onSharedPreferenceChanged: " + key);
        if(key.equals(getString(R.string.pref_key_keep_screen_on))){
            if(mMediaPlayer != null) {
                mMediaPlayer.setWakeMode(getApplicationContext(), sharedPreferences.getBoolean(getString(R.string.pref_key_keep_screen_on), false)?PowerManager.SCREEN_BRIGHT_WAKE_LOCK:PowerManager.PARTIAL_WAKE_LOCK);
            }
        }
        else if(key.equals(getString(R.string.pref_key_text_size))) {
            //mOverlay.loadUrl("javascript:setTextSize(" + sharedPreferences.getInt(getString(R.string.pref_key_text_size), Constants.DEFAULT_TEXT_SIZE) + ")");
            mTextSize = sharedPreferences.getInt(getString(R.string.pref_key_text_size), Constants.DEFAULT_TEXT_SIZE);
            mStrokeWidth = calculateStrokeWidth();
            mTitleView.setTextSize((int) (mTextSize * 1.3));
            mArtistView.setTextSize((int) (mTextSize * 1.2));

            if(mHasLyrics) {
                clearOverlay();
                parseLyrics(false);
            }
        }
        else if(key.equals(getString(R.string.pref_key_text_color))) {
            mTextColor = mSharedPref.getInt(getString(R.string.pref_key_text_color), Constants.DEFAULT_TEXT_COLOR);
            mTitleView.setTextColor(mTextColor);
            mArtistView.setTextColor(mTextColor);
            for(int i = 0; i < mLyrics.getChildCount(); i++) {
                LineView line = (LineView) mLyrics.getChildAt(i);
                line.setTextColor(mTextColor);
            }
        }
        else if(key.equals(getString(R.string.pref_key_text_stroke_color))) {
            //mOverlay.loadUrl("javascript:setStrokeColor(\"" + Utilities.intToColorCode(sharedPreferences.getInt(key, Constants.DEFAULT_STROKE_COLOR)) + "\")");
            mStrokeColor = mSharedPref.getInt(getString(R.string.pref_key_text_stroke_color), Constants.DEFAULT_STROKE_COLOR);
            mTitleView.setStrokeColor(mStrokeColor);
            mArtistView.setStrokeColor(mStrokeColor);
            for(int i = 0; i < mLyrics.getChildCount(); i++) {
                LineView line = (LineView) mLyrics.getChildAt(i);
                line.setStrokeColor(mStrokeColor);
            }
        }
        else if(key.equals(getString(R.string.pref_key_text_highlight_color))) {
            //mOverlay.loadUrl("javascript:setHighlightColor(\"" + Utilities.intToColorCode(sharedPreferences.getInt(key, Constants.DEFAULT_HIGHLIGHT_COLOR)) + "\")");
            mHighlightColor = mSharedPref.getInt(getString(R.string.pref_key_text_highlight_color), Constants.DEFAULT_STROKE_COLOR);
            for(int i = 0; i < mLyrics.getChildCount(); i++) {
                LineView line = (LineView) mLyrics.getChildAt(i);
                line.setHighlightColor(mHighlightColor);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        if(mOverlay != null) mWindowManager.removeView(mOverlay);
        if(mMediaPlayer != null) mMediaPlayer.release();
        mHandler.removeCallbacks(mUpdateTimeTask);
        giveUpAudioFocus();
        unregisterReceiver(mReceiver);
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.i(TAG, "onConfigurationChanged");

        if (mHasLyrics && newConfig.orientation != mOrientation) {
            mOrientation = newConfig.orientation;
            clearOverlay();
            parseLyrics(false);
        }
    }

    public void onLyricLayoutTaskComplete(List<Line> lines){
        mLyricLayoutTask = null;

        for(Line line : lines){
            LineView lineView = new LineView(this, line.getText(), line.getStart(), line.getEnd(), mTextSize, mTextColor, mStrokeColor, mHighlightColor, mStrokeWidth);

            if(line.isUntimed()){
                lineView.setUntimed(true);
            }
            else{
                lineView.setTag(line.getTransitions());
                lineView.setRtl(line.isRtl());
            }

            mLyrics.addView(lineView);
            this.mLines.add(lineView);
        }

        Collections.sort(this.mLines, new Comparator<LineView>() {
            @Override
            public int compare(LineView one, LineView two) {
                if(one.getStart() - two.getStart() > 0){
                    return 1;
                }
                return -1;
            }
        });

        mStartingIndex = 0;
        mHasLyrics = true;
        startTimeUpdate();
    }

    public int getCurrentPosition(){
        if(!mSongLoaded){
            return 0;
        }
        return mMediaPlayer.getCurrentPosition();
    }

    public int getDuration(){
        if(!mSongLoaded){
            return 0;
        }
        return mMediaPlayer.getDuration();
    }

    public boolean isPlaying(){
        if(mMediaPlayer != null && isSongLoaded() && mMediaPlayer.isPlaying()){
            return true;
        }
        return false;
    }

    public void setIsSeeking(boolean seeking){
        mSeeking = seeking;
    }

    public boolean isSeeking(){
        return mSeeking;
    }

    public void seekTo(int pos){
        mMediaPlayer.seekTo(pos);
        mStartingIndex = 0;
        mJump = true;
    }

    public void doPlayback() {
        Log.i(TAG, "doPlayback");
        if (mState == State.Paused || mState == State.Stopped) {
            doPlay();
        } else {
            doPause();
        }
    }

    public void doPlay() {
        Log.i(TAG, "doPlay");
        mStartPlayingAfterRetrieve = true;
        if (mState == State.Retrieving) {
            return;
        }

        if(songs.size() > 0) {
            tryToGetAudioFocus();

            // actually play the song
            if (mState == State.Stopped) {
                // If we're stopped, just go ahead to the next song and start playing
                playSong();
            } else if (mState == State.Paused) {
                // If we're paused, just continue playback
                mState = State.Playing;
                configAndStartMediaPlayer();
                updateNotification();
            }

            // Tell any remote controls that our playback state is 'playing'.
            if (mRemoteControlClientCompat != null) {
                mRemoteControlClientCompat
                        .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            }
        }
        else {
            Utilities.showError(this, R.string.error_no_songs_added);
        }
    }

    public void doPause() {
        Log.i(TAG, "doPause");
        if(mHasLyrics){
            //mOverlay.loadUrl("javascript:doPause()");
            //Clear all animations
            clearAllAnimations();
        }
        if (mState == State.Retrieving) {
            // If we are still retrieving media, clear the flag that indicates we should start
            // playing when we're ready
            mStartPlayingAfterRetrieve = false;
            return;
        }

        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            mMediaPlayer.pause();
            updateNotification();
        }

        // Tell any remote controls that our playback state is 'paused'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }

    public void doBack() {
        Log.i(TAG, "doBack");
        if (mState == State.Playing || mState == State.Paused) {
            if (mMediaPlayer.getCurrentPosition() < 2500) {
                playPrev();
            }
            else {
                mMediaPlayer.seekTo(0);
            }
        }
        else {
            doPlay();
        }
    }

    public void doForward() {
        Log.i(TAG, "doFoward");
        if (mState == State.Playing || mState == State.Paused) {
            playNext();
        }
        else {
            doPlay();
        }
    }

    public void doStop() {
        Log.i(TAG, "doStop");
        doStop(false);
    }

    public void doStop(boolean force) {
        if (mState == State.Playing || mState == State.Paused || force) {
            mState = State.Stopped;

            giveUpAudioFocus();

            // Tell any remote controls that our playback state is 'paused'.
            if (mRemoteControlClientCompat != null) {
                mRemoteControlClientCompat
                        .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }
        }
    }

    public int getSongIndex(){
        return index;
    }

    public void addSong(Song song){
        songs.add(song);
        savePlaylist();
    }

    public void removeSong(int index) {
        Log.i(TAG, "Remove Song:" + index);
        if(index < songs.size()){
            songs.remove(index);
            if(index == this.index) {
                Log.i(TAG, "Removed Playing Song");
                if(songs.size() == 0) {
                    if(mMediaPlayer != null){
                        mMediaPlayer.reset();
                        mSongLoaded = false;
                    }
                }
                else {
                    mStartPlayingAfterRetrieve = isPlaying();
                    mMediaPlayer.reset();
                    initializeSong();
                }
                if(mHasLyrics){
                    clearOverlay();
                }
            }
            else if(index < this.index) {
                this.index--;
            }
        }
        if(songs.size() == 0){
            clearNotification();
        }
        savePlaylist();
    }

    public boolean isSongLoaded(){
        return mSongLoaded;
    }

    public void setSongIndex(int index){
        this.index = index;
    }

    public void setSong(int index){
        if(index < songs.size()){
            this.index = index;
            playSong();
        }
    }

    public void setSongs(ArrayList<Song> songs){
        clearPlaylist();
        this.songs = songs;
    }

    public void clearPlaylist(){
        songs = new ArrayList<Song>();
        index = 0;
        if(mSongLoaded) {
            mMediaPlayer.stop();
            mSongLoaded = false;
        }
    }

    public ArrayList<Song> getSongs(){
        return songs;
    }

    public void loadPlaylist(String filename) throws IOException, ClassNotFoundException {
        ObjectInputStream input = new ObjectInputStream(openFileInput(filename));
        ArrayList<Song> songs = (ArrayList<Song>) input.readObject();
        input.close();
        setSongs(songs);

        mEditor.putString(getString(R.string.pref_key_current_playlist), filename);
        mEditor.commit();
    }

    public void savePlaylist(){
        String currentPlaylist = mSharedPref.getString(getString(R.string.pref_key_current_playlist), null);
        if(currentPlaylist != null){
            try {
                ObjectOutputStream out = new ObjectOutputStream(openFileOutput(currentPlaylist, 0));
                out.writeObject(songs);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            //Fatal Error
        }
    }

    private void initializeSong(){
        if(mStartPlayingAfterRetrieve){
            tryToGetAudioFocus();
        }
        mSongLoaded = false;
        if(songs.size() > 0) {
            if (index < 0 || index >= songs.size()) {
                index = 0;
            }
            try {
                Song song = songs.get(index);

                //Clear mOverlay
                clearOverlay();

                //Load mLyrics (if available)
                if(!TextUtils.isEmpty(song.getLyricFile())){
                    File lyricFile = new File(song.getLyricFile());
                    if(lyricFile.exists()){
                        JSONObject json = null;
                        try {
                            //Check and update songId
                            long songId = -1;
                            SAXBuilder builder = new SAXBuilder();
                            Document doc = builder.build(lyricFile);
                            mRoot = doc.getRootElement();
                            Element audio  = mRoot.getChild(Constants.KEY_AUDIO);

                            if(audio != null){
                                try{
                                    songId = Long.parseLong(audio.getText());
                                    song.setSongId(songId);
                                } catch(NumberFormatException e){
                                    Log.e(TAG, e.getMessage());
                                }
                            }

                            if(songId == -1){
                                Utilities.showError(this, R.string.error_no_attached_audio);
                                return;
                            }

                            parseLyrics(true);
                            mOrientation = getResources().getConfiguration().orientation;
                        } catch (JDOMException e) {
                            Log.i(TAG, e.toString());
                            Utilities.showError(this, R.string.error_lyric_not_xml);
                        }
                    }
                    else {
                        Utilities.showError(this, R.string.error_lyric_not_found);
                    }
                }

                //Load Audio
                try {
                    Uri trackUri = ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getSongId());
                    mMediaPlayer.setDataSource(getApplicationContext(), trackUri);
                } catch (IllegalStateException e){
                    Utilities.showError(this, R.string.error_missing_audio);
                    return;
                } catch (IOException e){
                    Utilities.showError(this, R.string.error_missing_audio);
                    return;
                }

                // Use the media button APIs (if available) to register ourselves for media button
                // events

                MediaButtonHelper.registerMediaButtonEventReceiverCompat(
                        mAudioManager, mMediaButtonReceiverComponent);

                // Use the remote control APIs (if available) to set the playback state

                if (mRemoteControlClientCompat == null) {
                    Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                    intent.setComponent(mMediaButtonReceiverComponent);
                    mRemoteControlClientCompat = new RemoteControlClientCompat(
                            PendingIntent.getBroadcast(this /*context*/,
                                    0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
                    RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                            mRemoteControlClientCompat);
                }

                mRemoteControlClientCompat.setPlaybackState(
                        RemoteControlClient.PLAYSTATE_PLAYING);

                mRemoteControlClientCompat.setTransportControlFlags(
                        RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                                RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                                RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                                RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                                RemoteControlClient.FLAG_KEY_MEDIA_STOP);

                // Update the remote controls
                mRemoteControlClientCompat.editMetadata(true)
                        .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, song.getArtist())
                        .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, song.getTitle())
                        .putBitmap(
                                RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                                mDummyAlbumArt)
                        .apply();

                mMediaPlayer.prepareAsync();
            } catch (IllegalStateException e) {
                Utilities.showError(this, R.string.error_missing_audio);
            } catch (IOException e) {
                Log.e(TAG, "Playback Error: " + e.getMessage(), e);
            }
        }
    }

    private void playSong(){
        Log.i(TAG, "playSong");
        mMediaPlayer.reset();
        mStartPlayingAfterRetrieve = true;
        initializeSong();
    }

    private void playPrev(){
        Log.i(TAG, "playPrev");
        index--;
        if(index < 0) {
            index = songs.size() - 1;
        }
        playSong();
    }

    private void playNext(){
        Log.i(TAG, "playNext");
        index++;
        if(index >= songs.size()) {
            index = 0;
        }
        playSong();
    }

    private void updateNotification(String title, String artist) {
        mRemoteViews.setTextViewText(R.id.title, title);
        mRemoteViews.setTextViewText(R.id.artist, artist);
        mRemoteViews.setImageViewResource(R.id.play, R.drawable.ic_pause);
        mBuilder.setTicker(artist + " - " + title);
        startForeground(NOTIFY_ID, mBuilder.build());
        updateWidget();
    }

    private void updateNotification() {
        if(mState == State.Playing) {
            mRemoteViews.setImageViewResource(R.id.play, R.drawable.ic_pause);
        }
        else {
            mRemoteViews.setImageViewResource(R.id.play, R.drawable.ic_play);
        }
        startForeground(NOTIFY_ID, mBuilder.build());
        updateWidget();
    }

    private void clearNotification() {
        mTitle = "";
        mArtist = "";
        mRemoteViews.setTextViewText(R.id.title, mTitle);
        mRemoteViews.setTextViewText(R.id.artist, mArtist);
        mRemoteViews.setImageViewResource(R.id.play, R.drawable.ic_play);
        startForeground(NOTIFY_ID, mBuilder.build());
        updateWidget();
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(getApplicationContext(), ControlWidget.class));
        for(int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, generateControlWidgetView(mTitle, mArtist, appWidgetId));
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    private void configAndStartMediaPlayer() {
        Log.i(TAG, "configAndStartMediaPlayer");
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            if (mMediaPlayer.isPlaying()){
                doPause();
            }
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            mMediaPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
        else
            mMediaPlayer.setVolume(1.0f, 1.0f); // we can be loud

        if (!mMediaPlayer.isPlaying()){
            mState = State.Playing;
            mMediaPlayer.start();
            updateNotification();
        }
    }

    private void createMediaPlayer() {
        mMediaPlayer = new MediaPlayer();

        // Make sure the media player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        mMediaPlayer.setWakeMode(getApplicationContext(), mSharedPref.getBoolean(getString(R.string.pref_key_keep_screen_on), false)?PowerManager.SCREEN_BRIGHT_WAKE_LOCK:PowerManager.PARTIAL_WAKE_LOCK);

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    private void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    private void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    private void setupAnimations(){
        titleAnimation = new AlphaAnimation(0f, 1f);
        titleAnimation.setDuration(2000);
        artistAnimation = new AlphaAnimation(0f, 0f);
        artistAnimation.setDuration(1000);

        final AlphaAnimation titleStay = new AlphaAnimation(1f, 1f);
        titleStay.setDuration(1500);
        final AlphaAnimation titleOut = new AlphaAnimation(1f, 0f);
        titleOut.setDuration(2000);
        final AlphaAnimation artistIn = new AlphaAnimation(0f, 1f);
        artistIn.setDuration(2000);
        final AlphaAnimation artistOut = new AlphaAnimation(1f, 0f);
        artistOut.setDuration(2000);

        titleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mTitleView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mTitleView.startAnimation(titleStay);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        titleStay.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mTitleView.startAnimation(titleOut);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        titleOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mTitleView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        artistAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mArtistView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mArtistView.startAnimation(artistIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        artistIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mArtistView.startAnimation(artistOut);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        artistOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mArtistView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void startTimeUpdate(){
        stopTimeUpdate();
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    private void stopTimeUpdate(){
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    private void clearOverlay(){
        if(mLyricLayoutTask != null){
            mLyricLayoutTask.cancel(true);
            mLyricLayoutTask = null;
        }
        stopTimeUpdate();
        clearAllAnimations();
        mTitleView.setText("");
        mArtistView.setText("");
        mLyrics.removeAllViews();
        mLines.clear();
        mHasLyrics = false;
    }

    private void parseLyrics(boolean showIntro) {
        Element root = mRoot.clone();
        if(showIntro) {
            mTitle = root.getChild("title").getText();
            mArtist = root.getChild("artist").getText();
            mTitleView.setText(mTitle);
            mArtistView.setText(mArtist);
            mTitleView.startAnimation(titleAnimation);
            mArtistView.startAnimation(artistAnimation);
        }

        int screenWidth = mWindowManager.getDefaultDisplay().getWidth() - 12;

        if(mLyricLayoutTask != null){
            mLyricLayoutTask.cancel(true);
        }
        mLyricLayoutTask = new LyricLayoutTask(this, screenWidth, mTextSize);
        mLyricLayoutTask.execute(root);
    }

    private void clearAllAnimations(){
        for(int i = 0; i < mLyrics.getChildCount(); i++) {
            mLyrics.getChildAt(i).clearAnimation();
        }
    }

    private int calculateStrokeWidth(){
        return (mTextSize - 50)/16 + 5;
    }

    private void updateControlWidget(Intent intent) {
        int appWidgetId = intent.getExtras().getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID);

        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(getApplicationContext());

        appWidgetManager.updateAppWidget(appWidgetId, generateControlWidgetView(mTitle, mArtist, appWidgetId));
    }

    private RemoteViews generateControlWidgetView(CharSequence title, CharSequence artist, int appWidgetId) {
        RemoteViews views = new RemoteViews(getApplicationContext()
                .getPackageName(), R.layout.control_widget);

        views.setTextViewText(R.id.title, title);
        views.setTextViewText(R.id.artist, artist);
        views.setImageViewResource(R.id.play, isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        views.setContentDescription(R.id.play, getString(isPlaying() ? R.string.pause : R.string.play));
        views.setOnClickPendingIntent(R.id.back,
                ControlWidget.generateLyricServicePendingIntent(getApplicationContext(), appWidgetId, Constants.ACTION_BACK));
        views.setOnClickPendingIntent(R.id.play,
                ControlWidget.generateLyricServicePendingIntent(getApplicationContext(), appWidgetId, Constants.ACTION_TOGGLE_PLAYBACK));
        views.setOnClickPendingIntent(R.id.forward,
                ControlWidget.generateLyricServicePendingIntent(getApplicationContext(), appWidgetId, Constants.ACTION_FORWARD));

        return views;
    }
}