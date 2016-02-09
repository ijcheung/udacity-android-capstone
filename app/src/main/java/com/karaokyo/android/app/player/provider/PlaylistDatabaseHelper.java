package com.karaokyo.android.app.player.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PlaylistDatabaseHelper extends SQLiteOpenHelper{
    private static final String TAG = "PlaylistDatabaseHelper";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_PLAYLISTS = "playlists";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";

    private static final String TABLE_CREATE = "CREATE TABLE " + TABLE_PLAYLISTS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TITLE + " TEXT)";

    public PlaylistDatabaseHelper(Context context) {
        super(context, TABLE_PLAYLISTS, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(TABLE_CREATE);
        Log.i(TAG, "Table Created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
        onCreate(sqLiteDatabase);
    }
}
