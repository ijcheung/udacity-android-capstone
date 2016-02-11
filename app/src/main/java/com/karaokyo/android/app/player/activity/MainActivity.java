package com.karaokyo.android.app.player.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.fragment.LibraryFragment;
import com.karaokyo.android.app.player.fragment.LyricFragment;
import com.karaokyo.android.app.player.fragment.NavigationDrawerFragment;
import com.karaokyo.android.app.player.fragment.PlaylistFragment;
import com.karaokyo.android.app.player.fragment.PlaylistsFragment;
import com.karaokyo.android.app.player.model.Playlist;
import com.karaokyo.android.app.player.model.Song;
import com.karaokyo.android.app.player.provider.LyricContract;
import com.karaokyo.android.app.player.provider.PlaylistContract;
import com.karaokyo.android.app.player.provider.PlaylistDatabaseHelper;
import com.karaokyo.android.app.player.service.LyricService;
import com.karaokyo.android.app.player.task.LyricScanTaskFragment;
import com.karaokyo.android.app.player.util.Constants;
import com.karaokyo.android.app.player.util.Utilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class MainActivity extends SelfClosingActivity implements
        NavigationDrawerFragment.OnNavigationDrawerItemSelectedListener,
        LibraryFragment.OnFragmentInteractionListener,
        LyricFragment.OnFragmentInteractionListener,
        PlaylistsFragment.OnFragmentInteractionListener,
        LyricScanTaskFragment.OnLyricScanCompleteListener {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_PICK_AUDIO = 1;

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private int mTitle;

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mEditor;

    private boolean mTwoPane;
    private boolean mBound;
    private Intent playIntent;
    private LyricService lyricService;
    private ContentResolver mResolver;

    private ServiceConnection lyricConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LyricService.LyricBinder binder = (LyricService.LyricBinder)service;
            lyricService = binder.getService();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPref.edit();
        mResolver = getContentResolver();

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTwoPane = mNavigationDrawerFragment == null;

        if(mTwoPane) {
            mNavigationDrawerFragment = (NavigationDrawerFragment)
                    getSupportFragmentManager().findFragmentById(R.id.navigation_pane);
        }
        else {
            // Set up the drawer.
            mNavigationDrawerFragment.setUp(
                    R.id.navigation_drawer,
                    (DrawerLayout) findViewById(R.id.drawer_layout));
        }

        playIntent = new Intent(this, LyricService.class);
    }

    /**
     * Called when an item in the navigation drawer is selected.
     *
     * @param position
     */
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        switch(position){
            case 0:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, LibraryFragment.newInstance(position))
                        .commit();
                break;
            case 1:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, LyricFragment.newInstance(position))
                        .commit();
                break;
            case 2:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, PlaylistsFragment.newInstance(position))
                        .commit();
                break;
            case 3:
                Fragment fragment;
                if(mBound){
                    fragment = PlaylistFragment.newInstance(lyricService.getCurrentPosition(), lyricService.getDuration(), lyricService.isPlaying(), position);
                }
                else{
                    fragment = PlaylistFragment.newInstance(position);
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();
                break;
            default:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, BlankFragment.newInstance(position + 1))
                        .commit();
        }
    }
    public void onSectionAttached(int position) {
        switch (position) {
            case 0:
                mTitle = R.string.library;
                getTracker().setScreenName("Library");
                getTracker().send(new HitBuilders.ScreenViewBuilder().build());
                break;
            case 1:
                mTitle = R.string.lyrics;
                getTracker().setScreenName("Lyric");
                getTracker().send(new HitBuilders.ScreenViewBuilder().build());
                break;
            case 2:
                mTitle = R.string.playlists;
                getTracker().setScreenName("Playlists");
                getTracker().send(new HitBuilders.ScreenViewBuilder().build());
                break;
            case 3:
                mTitle = R.string.now_playing;
                getTracker().setScreenName("Now Playing");
                getTracker().send(new HitBuilders.ScreenViewBuilder().build());
                break;
            default:
                mTitle = R.string.app_name;
        }
        if(mTwoPane){
            mTitle = R.string.app_name;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mNavigationDrawerFragment == null || !mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id){
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();

        if(!mBound) {
            startService(playIntent);
            bindService(playIntent, lyricConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();

        if (mBound) {
            unbindService(lyricConnection);
            mBound = false;
            lyricService = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case REQUEST_CODE_PICK_AUDIO:
                if(resultCode == RESULT_OK){
                    //Retrieve Results
                    String lyricFilePath = data.getStringExtra(Constants.KEY_LYRIC_FILE_PATH);
                    long songId = data.getLongExtra(Constants.KEY_SONG_ID, -1);

                    //Update XML
                    updateLyricXML(lyricFilePath, songId);
                }
                break;
        }
    }

    @Override
    public void onLibraryFragmentInteraction(final Song song) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(song.getTitle())
                .setItems(R.array.library_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, Integer.toString(which));
                        switch(which){
                            //Add to Playlist
                            case 0:
                                addToPlaylist(song);
                                break;
                            //Delete
                            case 1:
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(song.getTitle())
                                        .setMessage(R.string.delete_song_confirm)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                                                mResolver.delete(musicUri, android.provider.MediaStore.Audio.Media._ID + "= ?", new String[]{Long.toString(song.getSongId())});
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                // Do nothing
                                            }
                                        })
                                        .show();

                                break;
                        }
                    }
                });
        builder.create().show();
    }

    @Override
    public void onLyricFragmentInteraction(final Song song) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(song.getTitle())
                .setItems(R.array.lyric_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, Integer.toString(which));
                        switch(which){
                            //Add to Playlist
                            case 0:
                                addToPlaylist(song);
                                break;
                            //Attach Audio
                            case 1:
                                Intent pickAudioIntent = new Intent(MainActivity.this, AudioPickerActivity.class);
                                pickAudioIntent.putExtra(Constants.KEY_LYRIC_FILE_PATH, song.getLyricFile());
                                startActivityForResult(pickAudioIntent, REQUEST_CODE_PICK_AUDIO);
                                break;
                            //Delete
                            case 2:
                                final File lyricFile = new File(song.getLyricFile());
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(song.getTitle())
                                        .setMessage(R.string.delete_lyric_confirm)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                if (lyricFile.exists()) {
                                                    lyricFile.delete();
                                                }

                                                mResolver.delete(LyricContract.CONTENT_URI, LyricContract.FILEPATH + " = ?", new String[]{song.getLyricFile()});
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                // Do nothing
                                            }
                                        })
                                        .show();
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    @Override
    public void onPlaylistsFragmentInteraction(final Playlist playlist, boolean isLongClick) {
        if(isLongClick){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(playlist.getTitle())
                    .setItems(R.array.playlists_options, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(TAG, Integer.toString(which));
                            final File file = new File(getFilesDir(), playlist.getTitle());
                            switch(which){
                                //Rename
                                case 0:
                                    final EditText input = new EditText(MainActivity.this);
                                    input.setText(playlist.getTitle());
                                    input.setSingleLine(true);

                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle(R.string.rename_playlist)
                                            .setView(input)
                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    String filename = input.getText().toString();

                                                    if (!TextUtils.isEmpty(filename)) {
                                                        File newFile = new File(getFilesDir(), filename);
                                                        if (newFile.exists()) {
                                                            Utilities.showError(MainActivity.this, R.string.error_playlist_name_exists);
                                                        } else {
                                                            if(file.exists()){
                                                                file.renameTo(newFile);

                                                                if(playlist.getTitle().equals(retrieveCurrentPlaylistName())){
                                                                    mEditor.putString(getString(R.string.pref_key_current_playlist), filename);
                                                                    mEditor.commit();
                                                                }

                                                                playlist.setTitle(filename);
                                                                ContentValues values = new ContentValues();
                                                                values.put(PlaylistDatabaseHelper.COLUMN_TITLE, playlist.getTitle());
                                                                mResolver.update(PlaylistContract.CONTENT_URI, values,
                                                                        PlaylistDatabaseHelper.COLUMN_ID + " = " + playlist.getId(),
                                                                        null);
                                                            }
                                                        }
                                                    } else {
                                                        Utilities.showError(MainActivity.this, R.string.error_playlist_name_empty);
                                                    }
                                                }
                                            })
                                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    // Do nothing
                                                }
                                            })
                                            .show();
                                    break;
                                //Delete
                                case 1:
                                    //Confirm Delete
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle(playlist.getTitle())
                                            .setMessage(R.string.delete_playlist_confirm)
                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    file.delete();

                                                    if(playlist.getTitle().equals(retrieveCurrentPlaylistName())){
                                                        lyricService.clearPlaylist();
                                                        mEditor.putString(getString(R.string.pref_key_current_playlist), null);
                                                        mEditor.commit();
                                                    }

                                                    mResolver.delete(PlaylistContract.CONTENT_URI, PlaylistDatabaseHelper.COLUMN_ID
                                                            + " = " + playlist.getId(), null);
                                                }
                                            })
                                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    // Do nothing
                                                }
                                            })
                                            .show();
                                    break;
                            }
                        }
                    });
            builder.create().show();
        }
        else{
            if(mBound) {
                try {
                    lyricService.loadPlaylist(playlist.getTitle());
                    mNavigationDrawerFragment.selectItem(3);
                } catch (IOException e) {
                    //Could not read
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    //Corrupted File
                    e.printStackTrace();
                }
            }
            else{
                //TODO: Could not connect to service
            }
        }
    }

    @Override
    public void onLyricScanComplete() {
        try {
            LyricFragment fragment = (LyricFragment) getSupportFragmentManager().findFragmentById(R.id.container);
            fragment.onLyricScanTaskComplete();
        } catch (ClassCastException e) {
            //Lyric fragment not active, do nothing
        }
    }

    private void addToPlaylist(Song song){
        if(mBound){
            if(retrieveCurrentPlaylistName() != null){
                lyricService.addSong(song);
            }
            else{
                //Acquire Playlist
                Intent pickPlaylistIntent = new Intent(MainActivity.this, PlaylistPickerActivity.class);
                pickPlaylistIntent.putExtra(Constants.KEY_SONG, song);
                startActivity(pickPlaylistIntent);
            }
        }
    }

    private String retrieveCurrentPlaylistName(){
        return mSharedPref.getString(getString(R.string.pref_key_current_playlist), null);
    }

    private void updateLyricXML(String filepath, long songId){
        File lyricFile = new File(filepath);
        if(lyricFile.exists()) {
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(lyricFile);

                Element root = doc.getDocumentElement();

                Node audio = root.getElementsByTagName(Constants.KEY_AUDIO).item(0);

                if (audio == null) {
                    Node lyrics = root.getElementsByTagName(Constants.KEY_LYRICS).item(0);
                    Element a = doc.createElement(Constants.KEY_AUDIO);
                    a.appendChild(doc.createTextNode(Long.toString(songId)));
                    root.insertBefore(a, lyrics);
                } else {
                    audio.setTextContent(Long.toString(songId));
                }

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new FileOutputStream(lyricFile));
                transformer.transform(source, result);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(this, getString(R.string.error_attach_audio), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class BlankFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static BlankFragment newInstance(int sectionNumber) {
            BlankFragment fragment = new BlankFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public BlankFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_blank, container, false);
        }
    }
}