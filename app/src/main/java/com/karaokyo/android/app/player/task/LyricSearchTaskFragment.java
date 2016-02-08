package com.karaokyo.android.app.player.task;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.karaokyo.android.app.player.model.Lyric;
import com.karaokyo.android.app.player.model.Lyrics;
import com.karaokyo.android.app.player.util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

public class LyricSearchTaskFragment extends Fragment {
    private OnLyricSearchCompleteListener mCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (OnLyricSearchCompleteListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnLyricSearchCompleteListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallback = null;
    }

    public void execute() {
        new LyricSearchTask().execute();
    }

    public void execute(String title, String artist, int page) {
        new LyricSearchTask().execute(title, artist, Integer.toString(page));
    }

    public interface OnLyricSearchCompleteListener {
        void onLyricSearchComplete(Lyrics lyrics);
    }

    private class LyricSearchTask extends AsyncTask<String, Void, Lyrics> {
        private static final String TAG = "LyricSearchTask";

        private static final String LYRIC_URL = "/en/lyrics.json";

        @Override
        protected Lyrics doInBackground(String... strings) {
            Lyrics lyrics = null;

            URI uri;

            try {
                if (strings.length == 3) {
                    uri = new URI("http", Constants.KARAOKYO_ROOT_URL, LYRIC_URL, "title=" + strings[0] + "&artist=" + strings[1] + "&page=" + strings[2], "");
                } else {
                    uri = new URI("http", Constants.KARAOKYO_ROOT_URL, LYRIC_URL, "", "");
                }
            } catch (URISyntaxException e) {
                Log.e(TAG, e.toString());
                return null;
            }

            try {
                Log.i(TAG, uri.toString());

                HttpURLConnection urlConnection = (HttpURLConnection) (uri.toURL().openConnection());
                urlConnection.setConnectTimeout(1500);
                urlConnection.connect();
                if (urlConnection.getResponseCode() == 200) {
                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

                    BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    StringBuilder responseStrBuilder = new StringBuilder();
                    Log.i(TAG, urlConnection.getHeaderField(Constants.KEY_PAGINATION));

                    JSONObject pagination = new JSONObject(urlConnection.getHeaderField(Constants.KEY_PAGINATION));

                    boolean firstPage = pagination.getBoolean(Constants.KEY_FIRST_PAGE);
                    boolean lastPage = pagination.getBoolean(Constants.KEY_LAST_PAGE);
                    int nextPage = pagination.getInt(Constants.KEY_NEXT_PAGE);

                    lyrics = new Lyrics(firstPage, lastPage, nextPage);

                    String inputStr;
                    while ((inputStr = streamReader.readLine()) != null)
                        responseStrBuilder.append(inputStr);

                    JSONArray jsonArray = new JSONArray(responseStrBuilder.toString());

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = (JSONObject) jsonArray.get(i);

                        int id = json.getInt(Constants.KEY_ID);
                        String title = json.getString(Constants.KEY_TITLE);
                        String artist = json.getString(Constants.KEY_ARTIST);
                        String uploader = json.getString(Constants.KEY_UPLOADER);
                        double rating = json.getDouble(Constants.KEY_RATING);
                        String locale = json.getString(Constants.KEY_LOCALE);
                        String description = json.getString(Constants.KEY_DESCRIPTION);

                        lyrics.addLyric(new Lyric(id, title, artist, uploader, rating, locale, description));
                    }
                } else {
                    Log.e(TAG, "Response Code: " + urlConnection.getResponseCode());
                    return null;
                }
                urlConnection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Error checking internet connection", e);
                return null;
            } catch (JSONException e) {
                Log.e(TAG, "Bad Data Received", e);
                return null;
            }

            return lyrics;
        }

        @Override
        protected void onPostExecute(Lyrics lyrics) {
            if(mCallback != null){
                mCallback.onLyricSearchComplete(lyrics);
            }
        }
    }
}
