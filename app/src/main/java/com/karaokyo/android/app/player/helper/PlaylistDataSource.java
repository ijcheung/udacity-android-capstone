package com.karaokyo.android.app.player.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.karaokyo.android.app.player.model.Playlist;

import java.util.ArrayList;

public class PlaylistDataSource {
    // Database fields
    private SQLiteDatabase database;
    private PlaylistDatabaseHelper dbHelper;
    private String[] allColumns = { PlaylistDatabaseHelper.COLUMN_ID,
            PlaylistDatabaseHelper.COLUMN_TITLE};

    public PlaylistDataSource(Context context) {
        dbHelper = new PlaylistDatabaseHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Playlist createPlaylist(String title) {
        ContentValues values = new ContentValues();
        values.put(PlaylistDatabaseHelper.COLUMN_TITLE, title);
        long insertId = database.insert(PlaylistDatabaseHelper.TABLE_PLAYLISTS, null,
                values);
        Cursor cursor = database.query(PlaylistDatabaseHelper.TABLE_PLAYLISTS,
                allColumns, PlaylistDatabaseHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        Playlist newPlaylist = cursorToPlaylist(cursor);
        cursor.close();
        return newPlaylist;
    }

    public void updatePlaylist(Playlist playlist){
        ContentValues values = new ContentValues();
        values.put(PlaylistDatabaseHelper.COLUMN_TITLE, playlist.getTitle());
        database.update(PlaylistDatabaseHelper.TABLE_PLAYLISTS, values,
                PlaylistDatabaseHelper.COLUMN_ID + " = " + playlist.getId(),
                null);
    }

    public void deletePlaylist(Playlist playlist) {
        database.delete(PlaylistDatabaseHelper.TABLE_PLAYLISTS, PlaylistDatabaseHelper.COLUMN_ID
                + " = " + playlist.getId(), null);
    }

    public ArrayList<Playlist> getAllPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<Playlist>();

        Cursor cursor = database.query(PlaylistDatabaseHelper.TABLE_PLAYLISTS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Playlist playlist = cursorToPlaylist(cursor);
            playlists.add(playlist);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return playlists;
    }

    private Playlist cursorToPlaylist(Cursor cursor) {
        Playlist playlist = new Playlist(cursor.getLong(0), cursor.getString(1));
        return playlist;
    }
}
