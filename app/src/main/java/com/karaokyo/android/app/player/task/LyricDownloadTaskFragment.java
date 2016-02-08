package com.karaokyo.android.app.player.task;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.karaokyo.android.app.player.util.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricDownloadTaskFragment extends Fragment {
    private OnLyricDownloadCompleteListener mCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (OnLyricDownloadCompleteListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnLyricDownloadCompleteListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallback = null;
    }

    public void execute(int i){
        new LyricDownloadTask().execute(i);
    }

    public interface OnLyricDownloadCompleteListener {
        void onLyricDownloadTaskComplete(String filepath);
    }

    private class LyricDownloadTask extends AsyncTask<Integer, Void, String> {
        private final String TAG = "LyricDownloadTask";
        private final Pattern FILENAME = Pattern.compile("filename=\".+\"");

        @Override
        protected String doInBackground(Integer... integers) {
            URI uri;

            try {
                uri = new URI("http", Constants.KARAOKYO_ROOT_URL, "/en/lyrics/" + integers[0] + "/download", "", "");
            } catch (URISyntaxException e) {
                return null;
            }

            try {
                HttpURLConnection urlConnection = (HttpURLConnection) (uri.toURL().openConnection());
                urlConnection.setConnectTimeout(1500);
                urlConnection.connect();

                if(urlConnection.getResponseCode() == 200){
                    int fileLength = urlConnection.getContentLength();

                    Map<String, List<String>> fields = urlConnection.getHeaderFields();

                    String filename = urlConnection.getHeaderField("Content-Disposition");
                    Matcher matcher = FILENAME.matcher(filename);
                    if(!matcher.find()){
                        Log.i(TAG, "No filename found");
                        return null;
                    }
                    filename = filename.substring(matcher.start() + 10, matcher.end() - 5);
                    Log.i(TAG, filename);

                    File lyricsFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/lyrics");
                    if (!lyricsFolder.exists()) {
                        lyricsFolder.mkdir();
                    }

                    File file = new File(lyricsFolder, filename + ".klf");

                    for(int i = 1; file.exists(); i++){
                        file = new File(lyricsFolder, filename + "("+ i + ").klf");
                    }

                    Log.i(TAG, "Saving file as: " + file.getAbsolutePath());

                    // download the file
                    InputStream input = urlConnection.getInputStream();
                    FileOutputStream output = new FileOutputStream(file);

                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        total += count;

                        Log.i(TAG, Long.toString(total * 100 / fileLength));

                        output.write(data, 0, count);
                    }

                    Log.i(TAG, "Download Complete");

                    //Add to content provider
                    return file.getAbsolutePath();
                }
                else{
                    Log.e(TAG, "Response Code: " + urlConnection.getResponseCode());
                    return null;
                }
            } catch (MalformedURLException e) {
                Log.e(TAG, e.toString());
                return null;
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String filepath) {
            if(mCallback != null){
                mCallback.onLyricDownloadTaskComplete(filepath);
            }
        }
    }
}