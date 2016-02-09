package com.karaokyo.android.app.player.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;

public class PlaylistContentProvider extends ContentProvider {
    private static UriMatcher mUriMatcher = PlaylistContract.buildUriMatcher();

    private PlaylistDatabaseHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new PlaylistDatabaseHelper(getContext());
        return (mDbHelper != null);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(PlaylistContract.PLAYLISTS_TABLE_NAME);

        Cursor cursor;

        switch (mUriMatcher.match(uri)) {
            case PlaylistContract.GET_ALL:
                cursor = builder.query(mDbHelper.getWritableDatabase(), projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case PlaylistContract.GET_ONE:
                cursor = builder.query(mDbHelper.getWritableDatabase(), projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case PlaylistContract.GET_ALL:
                return PlaylistContract.PLAYLISTS_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = 0;
        switch (mUriMatcher.match(uri)) {
            case LyricContract.GET_ALL:
                id = db.insert(PlaylistContract.PLAYLISTS_TABLE_NAME, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.withAppendedPath(LyricContract.CONTENT_URI, Long.toString(id));
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        switch (mUriMatcher.match(uri)) {
            case PlaylistContract.GET_ALL:
                break;
            case PlaylistContract.GET_ONE:
                where = where + "_id = " + uri.getLastPathSegment();
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        int count = db.delete(PlaylistContract.PLAYLISTS_TABLE_NAME, where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count;
        switch (mUriMatcher.match(uri)) {
            case LyricContract.GET_ALL:
                count = db.update(PlaylistContract.PLAYLISTS_TABLE_NAME, values, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
