package com.karaokyo.android.app.player.helper;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.model.Song;

import java.util.ArrayList;

public class SongAdapter extends BaseAdapter {
    private ArrayList<Song> mSongs;
    private Cursor mCursor;
    private LayoutInflater mLayoutInflater;
    private Resources mResources;
    private boolean mSortable;
    private int mSelected = -1;
    private Type mType;

    public enum Type {
        LIBRARY, LYRICS, PLAYLIST
    }

    // Used by LibraryFragment & LyricFragment
    public SongAdapter(Context context, Cursor cursor, Type type){
        mCursor = cursor;
        mLayoutInflater = LayoutInflater.from(context);
        mResources = context.getResources();
        mType = type;
    }

    // Used by PlaylistFragment
    public SongAdapter(Context context, ArrayList<Song> songs){
        mSongs = songs;
        mLayoutInflater = LayoutInflater.from(context);
        mResources = context.getResources();

        mSortable = true;
        mType = Type.PLAYLIST;
    }

    public int getActiveSong(){
        return mSelected;
    }

    public void setActiveSong(int index){
        mSelected = index;
    }

    @Override
    public int getCount() {
        return mCursor == null ? mSongs.size() : mCursor.getCount();
    }

    @Override
    public Object getItem(int index) {
        if(mCursor == null) {
            return mSongs.get(index);
        }
        else {
            return null;
        }
    }

    @Override
    public long getItemId(int index) {
        if(mCursor == null) {
            return mSongs.get(index).getSongId();
        }
        else {
            mCursor.moveToPosition(index);
            return mCursor.getLong(0);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView title;
        TextView artist;

        if(convertView == null){
            convertView = mLayoutInflater.inflate(R.layout.song, parent, false);

            ImageView handle = (ImageView) convertView.findViewById(R.id.handle);

            if(!mSortable || getCount() == 1){
                handle.setVisibility(View.GONE);
            }

            title = (TextView) convertView.findViewById(R.id.title);
            artist = (TextView) convertView.findViewById(R.id.artist);

            ViewHolder holder = new ViewHolder();
            holder.title = title;
            holder.artist = artist;
            convertView.setTag(holder);
        }
        else{
            ViewHolder holder = (ViewHolder) convertView.getTag();
            title = holder.title;
            artist = holder.artist;
        }

        if (mSelected == position) {
            convertView.setBackgroundColor(mResources.getColor(R.color.accent));
        }
        else{
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        switch(mType){
            case LIBRARY:
                mCursor.moveToPosition(position);
                title.setText(mCursor.getString(SongLoader.Query.TITLE));
                artist.setText(mCursor.getString(SongLoader.Query.ARTIST));
                break;
            case LYRICS:
                mCursor.moveToPosition(position);
                title.setText(mCursor.getString(LyricLoader.Query.TITLE));
                artist.setText(mCursor.getString(LyricLoader.Query.ARTIST));
                break;
            case PLAYLIST:
                Song current = mSongs.get(position);
                title.setText(current.getTitle());
                artist.setText(current.getArtist());
                break;
        }

        return convertView;
    }

    static class ViewHolder {
        TextView title;
        TextView artist;
    }
}