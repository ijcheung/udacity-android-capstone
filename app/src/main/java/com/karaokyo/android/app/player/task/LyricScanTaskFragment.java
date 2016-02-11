package com.karaokyo.android.app.player.task;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.karaokyo.android.app.player.model.Song;
import com.karaokyo.android.app.player.provider.LyricContract;
import com.karaokyo.android.app.player.util.Constants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import mf.javax.xml.transform.stream.StreamSource;
import mf.javax.xml.validation.Schema;
import mf.javax.xml.validation.SchemaFactory;
import mf.javax.xml.validation.Validator;
import mf.org.apache.xerces.jaxp.validation.XMLSchemaFactory;

public class LyricScanTaskFragment extends Fragment {
    private OnLyricScanCompleteListener mCallback;

    private boolean mScanning;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (OnLyricScanCompleteListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnLyricScanCompleteListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallback = null;
    }

    public void execute(Context context) {
        mScanning = true;
        new LyricScanTask(context).execute();
    }

    public void execute(Context context, String filepath) {
        new LyricScanTask(context).execute(filepath);
    }

    public boolean isScanning(){
        return mScanning;
    }

    public interface OnLyricScanCompleteListener {
        void onLyricScanComplete();
    }

    private class LyricScanTask extends AsyncTask<String, Void, Void> {
        private static final String TAG = "LyricScanTask";

        private ContentResolver mResolver;
        private Validator mValidator;

        public LyricScanTask(Context context) {
            Log.i(TAG, "Task created");

            try {
                SchemaFactory factory = new XMLSchemaFactory();
                Schema schema = factory.newSchema(new StreamSource(context.getAssets().open("schema.xsd")));
                mValidator = schema.newValidator();
            } catch (SAXException | IOException e) {
                Log.e(TAG, e.toString());
            }

            mResolver = context.getContentResolver();
        }

        @Override
        protected Void doInBackground(String... strings) {
            Log.i(TAG, "Scan started");

            if (strings.length > 0) {
                for (String filepath : strings) {
                    scanForLyrics(filepath);
                }
            } else {
                scanForLyrics();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.i(TAG, "Scan complete");
            mScanning = false;
            if(mCallback != null){
                mCallback.onLyricScanComplete();
            }
        }

        private void scanForLyrics() {
            ArrayList<Song> songs = new ArrayList<Song>();

            //Update all provider entries
            Cursor lyricCursor = mResolver.query(LyricContract.CONTENT_URI, null, null, null, null);

            if (lyricCursor != null && lyricCursor.moveToFirst()) {
                int idColumn = lyricCursor.getColumnIndex(LyricContract._ID);
                int titleColumn = lyricCursor.getColumnIndex(LyricContract.TITLE);
                int artistColumn = lyricCursor.getColumnIndex(LyricContract.ARTIST);
                int songIdColumn = lyricCursor.getColumnIndex(LyricContract.SONG_ID);
                int filepathColumn = lyricCursor.getColumnIndex(LyricContract.FILEPATH);

                do {
                    long id = lyricCursor.getLong(idColumn);
                    String title = lyricCursor.getString(titleColumn);
                    String artist = lyricCursor.getString(artistColumn);
                    long songId = lyricCursor.getLong(songIdColumn);
                    String filepath = lyricCursor.getString(filepathColumn);

                    File lyricFile = new File(filepath);

                    //Check is file exists and is valid
                    if (lyricFile.exists() && isValidLyricFile(lyricFile)) {
                        //Get values from file
                        try {
                            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                            Document doc = dBuilder.parse(lyricFile);
                            Element root = doc.getDocumentElement();
                            String currentTitle = root.getElementsByTagName(Constants.KEY_TITLE).item(0).getTextContent();
                            String currentArtist = root.getElementsByTagName(Constants.KEY_ARTIST).item(0).getTextContent();
                            Node audio = root.getElementsByTagName(Constants.KEY_AUDIO).item(0);
                            long currentSongId = -1;
                            if (audio != null) {
                                try {
                                    currentSongId = Long.parseLong(audio.getTextContent());
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, e.getMessage());
                                }
                            }

                            //Update provider entry
                            if (!title.equals(currentTitle) || !artist.equals(currentArtist) || songId != currentSongId) {
                                ContentValues values = new ContentValues();

                                //values.put(LyricContract._ID, id);
                                values.put(LyricContract.TITLE, currentTitle);
                                values.put(LyricContract.ARTIST, currentArtist);
                                values.put(LyricContract.SONG_ID, currentSongId);
                                //values.put(LyricContract.FILEPATH, filepath);

                                mResolver.update(LyricContract.CONTENT_URI, values, LyricContract._ID + " = ?", new String[]{Long.toString(id)});
                                Log.i(TAG, "Updated entry: " + currentTitle + " - " + currentArtist);
                            }

                            songs.add(new Song(currentTitle, currentArtist, currentSongId, filepath));
                        } catch (ParserConfigurationException | SAXException | IOException e) {
                            Log.e(TAG, e.toString());
                        }
                    } else {
                        //Delete entry from content provider
                        mResolver.delete(LyricContract.CONTENT_URI, LyricContract._ID + " = ?", new String[]{Long.toString(id)});
                        Log.i(TAG, "Deleted entry: " + title + " - " + artist);
                    }
                }
                while (lyricCursor.moveToNext());
                lyricCursor.close();
            }

            List<ContentValues> newEntries = scanForLyrics(new File(Environment.getExternalStorageDirectory().getAbsolutePath()), songs);

            //Add new entries to provider
            mResolver.bulkInsert(LyricContract.CONTENT_URI, newEntries.toArray(new ContentValues[newEntries.size()]));
        }

        private List<ContentValues> scanForLyrics(File directory, ArrayList<Song> providerEntries) {
            List<ContentValues> newEntries = new ArrayList<ContentValues>();

            File files[] = directory.listFiles();

            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        newEntries.addAll(scanForLyrics(file, providerEntries));
                    } else if (file.getName().endsWith(".klf")) {
                        Log.i(TAG, file.getAbsolutePath());
                        //Step 1: Check if file already exists in the provider
                        if (!doesExistInProvider(file.getAbsolutePath(), providerEntries)) {
                            try {
                                //Step 2: Simple XML Validation
                                if (isValidLyricFile(file)) {
                                    //Step 3: Extract Data
                                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                                    Document doc = dBuilder.parse(file);
                                    Element root = doc.getDocumentElement();
                                    String title = root.getElementsByTagName(Constants.KEY_TITLE).item(0).getTextContent();
                                    String artist = root.getElementsByTagName(Constants.KEY_ARTIST).item(0).getTextContent();
                                    Node audio = root.getElementsByTagName(Constants.KEY_AUDIO).item(0);
                                    long songId = -1;
                                    if (audio != null) {
                                        try {
                                            songId = Long.parseLong(audio.getTextContent());
                                        } catch (NumberFormatException e) {
                                            Log.e(TAG, e.getMessage());
                                        }
                                    }

                                    //Step 4: Add entry to list of entries to be added
                                    ContentValues values = new ContentValues();

                                    values.put(LyricContract.TITLE, title);
                                    values.put(LyricContract.ARTIST, artist);
                                    values.put(LyricContract.SONG_ID, songId);
                                    values.put(LyricContract.FILEPATH, file.getAbsolutePath());

                                    newEntries.add(values);
                                    Log.i(TAG, "Found Lyric: " + title + " - " + artist);
                                } else {
                                    Log.i(TAG, "Invalid Lyric File: " + file.getName());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage());
                            }
                        }
                    }
                }
            }

            return newEntries;
        }

        private void scanForLyrics(String filepath) {
            Log.i(TAG, "Scan requested for: " + filepath);
            File file = new File(filepath);

            if (file.exists()) {
                try {
                    //XML Validation
                    if (isValidLyricFile(file)) {
                        //Extract Data
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(file);
                        Element root = doc.getDocumentElement();
                        String title = root.getElementsByTagName(Constants.KEY_TITLE).item(0).getTextContent();
                        String artist = root.getElementsByTagName(Constants.KEY_ARTIST).item(0).getTextContent();
                        Node audio = root.getElementsByTagName(Constants.KEY_AUDIO).item(0);
                        long songId = -1;
                        if (audio != null) {
                            try {
                                songId = Long.parseLong(audio.getTextContent());
                            } catch (NumberFormatException e) {
                                Log.w(TAG, e.getMessage());
                            }
                        }

                        //Add entry to list of entries to be added
                        ContentValues values = new ContentValues();

                        values.put(LyricContract.TITLE, title);
                        values.put(LyricContract.ARTIST, artist);
                        values.put(LyricContract.SONG_ID, songId);
                        values.put(LyricContract.FILEPATH, file.getAbsolutePath());

                        mResolver.insert(LyricContract.CONTENT_URI, values);
                        Log.i(TAG, "Found Lyric: " + title + " - " + artist);
                    } else {
                        //Invalid newly downloaded lyric file
                        //TODO: Toast
                        file.delete();
                    }
                } catch (ParserConfigurationException | SAXException | IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean isValidLyricFile(File file) {
            try {
                mValidator.validate(new StreamSource(file));
            } catch (IOException | SAXException e) {
                Log.e(TAG, e.getMessage());
                return false;
            }
            return true;
        }

        private boolean doesExistInProvider(String filepath, ArrayList<Song> providerEntries) {
            for (Song song : providerEntries) {
                if (song.getLyricFile().equals(filepath)) {
                    return true;
                }
            }

            return false;
        }
    }
}