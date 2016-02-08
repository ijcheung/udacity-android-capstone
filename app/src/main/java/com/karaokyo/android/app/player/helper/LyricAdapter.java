package com.karaokyo.android.app.player.helper;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.model.Lyric;

import java.util.List;

public class LyricAdapter extends BaseAdapter {
    private List<Lyric> lyrics;
    private LayoutInflater mLayoutInflater;
    private Resources mResources;

    public LyricAdapter(Context context, List<Lyric> lyrics){
        this.lyrics = lyrics;
        mLayoutInflater = LayoutInflater.from(context);
        mResources = context.getResources();
    }

    @Override
    public int getCount() {
        return lyrics.size();
    }

    @Override
    public Object getItem(int index) {
        return lyrics.get(index);
    }

    @Override
    public long getItemId(int index) {
        return lyrics.get(index).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView title;
        TextView artist;
        ImageView flag;
        TextView locale;
        RatingBar rating;

        if(convertView == null){
            convertView = mLayoutInflater.inflate(R.layout.lyric, parent, false);

            title = (TextView) convertView.findViewById(R.id.title);
            artist = (TextView) convertView.findViewById(R.id.artist);
            flag = (ImageView) convertView.findViewById(R.id.flag);
            locale = (TextView) convertView.findViewById(R.id.locale);
            rating = (RatingBar) convertView.findViewById(R.id.rating);

            ViewHolder holder = new ViewHolder();
            holder.title = title;
            holder.artist = artist;
            holder.flag = flag;
            holder.locale = locale;
            holder.rating = rating;
            convertView.setTag(holder);
        }
        else{
            ViewHolder holder = (ViewHolder) convertView.getTag();
            title = holder.title;
            artist = holder.artist;
            flag = holder.flag;
            locale = holder.locale;
            rating = holder.rating;
        }

        Lyric current = lyrics.get(position);
        int flagResource = mResources.getIdentifier(current.getLocale().toLowerCase(), "drawable", "com.karaokyo.android.app.player");
        if(flagResource == 0) {
            flagResource = R.drawable.unknown;
        }
        title.setText(current.getTitle());
        artist.setText(current.getArtist());

        flag.setImageResource(flagResource);
        locale.setText(current.getLocale());
        rating.setRating((float) current.getRating());

        return convertView;
    }

    static class ViewHolder {
        TextView title;
        TextView artist;
        ImageView flag;
        TextView locale;
        RatingBar rating;
    }
}