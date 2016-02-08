package com.karaokyo.android.app.player.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.model.Lyric;
import com.karaokyo.android.app.player.task.LyricDownloadTaskFragment;
import com.karaokyo.android.app.player.task.LyricScanTaskFragment;
import com.karaokyo.android.app.player.util.Constants;
import com.karaokyo.android.app.player.util.Utilities;

import in.uncod.android.bypass.Bypass;

public class LyricDisplayActivity extends SelfClosingActivity implements
        LyricDownloadTaskFragment.OnLyricDownloadCompleteListener,
        LyricScanTaskFragment.OnLyricScanCompleteListener {
    private static final String TAG_DOWNLOAD_TASK_FRAGMENT = "download_task_fragment";
    private static final String TAG_SCAN_TASK_FRAGMENT = "scan_task_fragment";

    private LyricDownloadTaskFragment mDownloadTaskFragment;
    private LyricScanTaskFragment mScanTaskFragment;

    private int mLyricId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyric_display);

        Bypass bypass = new Bypass(this);

        FragmentManager fm = getSupportFragmentManager();
        mDownloadTaskFragment = (LyricDownloadTaskFragment) fm
                .findFragmentByTag(TAG_DOWNLOAD_TASK_FRAGMENT);
        mScanTaskFragment = (LyricScanTaskFragment) fm
                .findFragmentByTag(TAG_SCAN_TASK_FRAGMENT);

        if(mDownloadTaskFragment == null){
            mDownloadTaskFragment = new LyricDownloadTaskFragment();
            fm.beginTransaction()
                    .add(mDownloadTaskFragment, TAG_DOWNLOAD_TASK_FRAGMENT)
                    .commit();
        }

        if(mScanTaskFragment == null){
            mScanTaskFragment = new LyricScanTaskFragment();
            fm.beginTransaction()
                    .add(mScanTaskFragment, TAG_SCAN_TASK_FRAGMENT)
                    .commit();
        }

        Lyric lyric = (Lyric) getIntent().getSerializableExtra(Constants.KEY_LYRICS);

        mLyricId = lyric.getId();

        TextView title = (TextView) findViewById(R.id.title);
        TextView artist = (TextView) findViewById(R.id.artist);
        TextView uploader = (TextView) findViewById(R.id.uploader);
        RatingBar rating = (RatingBar) findViewById(R.id.rating);
        TextView description = (TextView) findViewById(R.id.description);

        title.setText(lyric.getTitle());
        artist.setText(lyric.getArtist());
        uploader.setText(lyric.getUploader());
        rating.setRating((float) lyric.getRating());
        description.setText(bypass.markdownToSpannable(lyric.getDescription()));
        description.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onLyricDownloadTaskComplete(String filepath) {
        if(filepath != null){
            Utilities.showError(this, R.string.download_complete);
            mScanTaskFragment.execute(this, filepath);
        }
        else{
            Utilities.showError(this, R.string.download_failed);
        }
    }

    @Override
    public void onLyricScanComplete() {

    }

    public void download(View view){
        mDownloadTaskFragment.execute(mLyricId);
    }
    public void openBrowser(View view){
        String url = getString(R.string.link_lyric) + mLyricId;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }
}