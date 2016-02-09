package com.karaokyo.android.app.player.helper;

import android.content.Context;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;

public class SongLoader extends CursorLoader {
    public SongLoader(Context context){
        super(context, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Query.PROJECTION, null, null, MediaStore.Audio.Media.TITLE + " ASC");
    }

    public interface Query {
        String[] PROJECTION = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION
        };

        int _ID = 0;
        int TITLE = 1;
        int ARTIST = 2;
        int ALBUM = 3;
        int DURATION = 4;
    }
}
