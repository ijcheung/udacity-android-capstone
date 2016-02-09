package com.karaokyo.android.app.player.provider;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;

public class PlaylistContract {
    public static final String AUTHORITY = "com.karaokyo.android.app.player.provider.playlists";
    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY + "/");

    public static final String PLAYLISTS_TABLE_NAME = "playlists";

    // The URI for this table.
    public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_URI, PLAYLISTS_TABLE_NAME);

    public static final String PLAYLISTS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.karaokyo.android.app.player.provider.Playlists";
    public static final String PLAYLIST_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.karaokyo.android.app.player.provider.Playlists";

    public static final int GET_ALL = 0;
    public static final int GET_ONE = 1;

    public static UriMatcher buildUriMatcher()
    {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        // Uris to match, and the code to return when matched
        matcher.addURI(PlaylistContract.AUTHORITY, PLAYLISTS_TABLE_NAME, GET_ALL);
        matcher.addURI(PlaylistContract.AUTHORITY, PLAYLISTS_TABLE_NAME + "/#", GET_ONE);
        return matcher;
    }

    public static final String _ID = "_id";
    public static final String TITLE = "title";
}
