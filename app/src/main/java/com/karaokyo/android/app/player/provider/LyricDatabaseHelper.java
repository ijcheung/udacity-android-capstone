package com.karaokyo.android.app.player.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LyricDatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "lyrics";

    private static final String CREATE_LYRICS_TABLE = " CREATE TABLE "
            + LyricContract.LYRICS_TABLE_NAME + " ("
            + LyricContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + LyricContract.TITLE + " TEXT NOT NULL, "
            + LyricContract.ARTIST + " TEXT NOT NULL, "
            + LyricContract.SONG_ID + " INTEGER, "
            + LyricContract.FILEPATH + " TEXT NOT NULL);";

    public LyricDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_LYRICS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LyricContract.LYRICS_TABLE_NAME);
        onCreate(db);
    }
}
