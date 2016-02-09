package com.karaokyo.android.app.player.helper;

import android.content.Context;
import android.support.v4.content.CursorLoader;

import com.karaokyo.android.app.player.provider.PlaylistContract;

public class PlaylistLoader extends CursorLoader {
    public PlaylistLoader(Context context) {
        super(context, PlaylistContract.CONTENT_URI, Query.PROJECTION, null, null, PlaylistContract.TITLE + " ASC");
    }

    public interface Query {
        String[] PROJECTION = {
                PlaylistContract._ID,
                PlaylistContract.TITLE,
        };

        int _ID = 0;
        int TITLE = 1;
    }
}
