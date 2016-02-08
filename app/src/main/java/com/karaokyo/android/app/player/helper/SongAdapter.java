package com.karaokyo.android.app.player.helper;

import android.content.Context;
import android.content.res.Resources;
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
    private ArrayList<Song> songs;
    private LayoutInflater mLayoutInflater;
    private Resources mResources;
    private boolean mSortable;
    private int mSelected = -1;

    public SongAdapter(Context context, ArrayList<Song> songs){
        this.songs = songs;
        mLayoutInflater = LayoutInflater.from(context);
        mResources = context.getResources();
    }

    public SongAdapter(Context context, ArrayList<Song> songs, boolean sortable){
        this(context, songs);

        mSortable = sortable;
    }

    public int getActiveSong(){
        return mSelected;
    }

    public void setActiveSong(int index){
        mSelected = index;
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int index) {
        return songs.get(index);
    }

    @Override
    public long getItemId(int index) {
        return songs.get(index).getSongId();
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

        Song currSong = songs.get(position);

        title.setText(currSong.getTitle());
        artist.setText(currSong.getArtist());

        return convertView;
    }

    static class ViewHolder {
        TextView title;
        TextView artist;
    }
}