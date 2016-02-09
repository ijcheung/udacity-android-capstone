package com.karaokyo.android.app.player.helper;

import android.content.Context;
import android.support.v4.content.CursorLoader;

import com.karaokyo.android.app.player.provider.LyricContract;

public class LyricLoader extends CursorLoader {
    public LyricLoader(Context context) {
        super(context, LyricContract.CONTENT_URI, Query.PROJECTION, null, null, LyricContract.TITLE + " ASC");
    }

    public interface Query {
        String[] PROJECTION = {
                LyricContract._ID,
                LyricContract.TITLE,
                LyricContract.ARTIST,
                LyricContract.SONG_ID,
                LyricContract.FILEPATH,
        };

        int _ID = 0;
        int TITLE = 1;
        int ARTIST = 2;
        int SONG_ID = 3;
        int FILEPATH = 4;
    }
}
