package com.karaokyo.android.app.player.helper;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.karaokyo.android.app.player.R;

public class PlaylistAdapter extends BaseAdapter {
    private Cursor mCursor;
    private LayoutInflater mLayoutInflater;

    public PlaylistAdapter(Context c, Cursor cursor){
        mCursor = cursor;
        mLayoutInflater = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(PlaylistLoader.Query._ID);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        mCursor.moveToPosition(position);

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

        title.setText(mCursor.getString(PlaylistLoader.Query.TITLE));

        return convertView;
    }

    static class ViewHolder {
        TextView title;
    }
}