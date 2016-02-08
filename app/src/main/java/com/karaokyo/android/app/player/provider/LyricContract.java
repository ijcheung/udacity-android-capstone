package com.karaokyo.android.app.player.provider;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;

public class LyricContract {
    public static final String AUTHORITY = "com.karaokyo.android.app.player.provider.lyrics";
    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY + "/");

    public static final String LYRICS_TABLE_NAME = "lyrics";

    // The URI for this table.
    public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_URI, LYRICS_TABLE_NAME);

    public static final String LYRICS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.karaokyo.android.app.player.provider.Lyrics";
    public static final String LYRIC_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.karaokyo.android.app.player.provider.Lyrics";

    public static final int GET_ALL = 0;
    public static final int GET_ONE = 1;

    public static UriMatcher buildUriMatcher()
    {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        // Uris to match, and the code to return when matched
        matcher.addURI(LyricContract.AUTHORITY, LYRICS_TABLE_NAME, GET_ALL);
        matcher.addURI(LyricContract.AUTHORITY, LYRICS_TABLE_NAME + "/#", GET_ONE);
        return matcher;
    }

    public static final String _ID = "_id";
    public static final String TITLE = "title";
    public static final String ARTIST = "artist";
    public static final String SONG_ID = "song_id";
    public static final String FILEPATH = "filepath";
}
