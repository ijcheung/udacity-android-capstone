package com.karaokyo.android.app.player.helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.model.Playlist;

import java.util.ArrayList;

public class PlaylistAdapter extends BaseAdapter {
    private ArrayList<Playlist> playlists;
    private LayoutInflater mLayoutInflater;

    public PlaylistAdapter(Context c, ArrayList<Playlist> playlists){
        this.playlists = playlists;
        mLayoutInflater=LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return playlists.size();
    }

    @Override
    public Object getItem(int arg0) {
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView title;

        if(convertView == null){
            convertView = mLayoutInflater.inflate(R.layout.playlist, parent, false);

            title = (TextView) convertView.findViewById(R.id.title);

            ViewHolder holder = new ViewHolder();
            holder.title = title;
            convertView.setTag(holder);
        }
        else{
            ViewHolder holder = (ViewHolder) convertView.getTag();
            title = holder.title;
            convertView.setTag(holder);
        }

        Playlist playlist = playlists.get(position);
        title.setText(playlist.getTitle());

        return convertView;
    }

    static class ViewHolder {
        TextView title;
    }
}