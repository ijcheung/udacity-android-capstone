package com.karaokyo.android.app.player.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.helper.LyricAdapter;
import com.karaokyo.android.app.player.model.Lyric;
import com.karaokyo.android.app.player.model.Lyrics;
import com.karaokyo.android.app.player.task.LyricSearchTaskFragment;
import com.karaokyo.android.app.player.util.Constants;
import com.karaokyo.android.app.player.util.Utilities;

import java.util.ArrayList;

public class LyricSearchActivity extends SelfClosingActivity implements
        LyricSearchTaskFragment.OnLyricSearchCompleteListener {
    private static final String TAG = "LyricSearchActivity";
    private static final String TAG_SEARCH_TASK_FRAGMENT = "search_task_fragment";

    private LyricSearchTaskFragment mSearchTaskFragment;

    private EditText mTitleField;
    private EditText mArtistField;
    private Button mSearch;

    private TextView mNoLyrics;
    private Button mLoad;
    private ProgressBar mLoadingIndicator;

    private ListView mListView;
    private ArrayList<Lyric> mLyrics;
    private LyricAdapter mAdapter;

    private String mTitle = "";
    private String mArtist = "";
    private int mNextPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyric_search);

        FragmentManager fm = getSupportFragmentManager();
        mSearchTaskFragment = (LyricSearchTaskFragment) fm
                .findFragmentByTag(TAG_SEARCH_TASK_FRAGMENT);

        if(mSearchTaskFragment == null){
            mSearchTaskFragment = new LyricSearchTaskFragment();
            fm.beginTransaction()
                    .add(mSearchTaskFragment, TAG_SEARCH_TASK_FRAGMENT)
                    .commit();
        }

        View footerView = getLayoutInflater().inflate(R.layout.lyric_footer_view, null);

        mTitleField = (EditText) findViewById(R.id.title);
        mArtistField = (EditText) findViewById(R.id.artist);
        mSearch = (Button) findViewById(R.id.search);
        mListView = (ListView) findViewById(R.id.listView);

        mNoLyrics = (TextView) footerView.findViewById(R.id.no_lyrics);
        mLoad = (Button) footerView.findViewById(R.id.load);
        mLoadingIndicator = (ProgressBar) footerView.findViewById(R.id.loadingIndicator);

        mListView.addFooterView(footerView, null, false);

        mSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTitle = mTitleField.getText().toString();
                mArtist = mArtistField.getText().toString();
                mNoLyrics.setVisibility(View.GONE);
                mLoad.setVisibility(View.GONE);
                mLoadingIndicator.setVisibility(View.VISIBLE);
                mSearchTaskFragment.execute(mTitle, mArtist, 1);
            }
        });

        mLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNoLyrics.setVisibility(View.GONE);
                mLoad.setVisibility(View.GONE);
                mLoadingIndicator.setVisibility(View.VISIBLE);
                mSearchTaskFragment.execute(mTitle, mArtist, mNextPage);
            }
        });

        mLyrics = new ArrayList<Lyric>();
        mAdapter = new LyricAdapter(this, mLyrics);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(LyricSearchActivity.this, LyricDisplayActivity.class);
                intent.putExtra(Constants.KEY_LYRICS, mLyrics.get(position));

                startActivity(intent);
            }
        });

        if(savedInstanceState == null){
            if(Utilities.isNetworkAvailable(this)){
                mSearchTaskFragment.execute();
            }
            else{
                Utilities.showError(this, R.string.error_no_connection);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        getTracker().setScreenName("Lyric Search");
        getTracker().send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("lyrics", mLyrics);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        mLyrics = (ArrayList<Lyric>) state.getSerializable("lyrics");
        mAdapter = new LyricAdapter(this, mLyrics);
        mListView.setAdapter(mAdapter);
        super.onRestoreInstanceState(state);
    }

    @Override
    public void onLyricSearchComplete(Lyrics lyrics){
        mLoad.setVisibility(View.GONE);
        mLoadingIndicator.setVisibility(View.GONE);

        if(lyrics != null) {
            Log.i(TAG, "firstPage: " + lyrics.isFirstPage());
            Log.i(TAG, "lastPage: " + lyrics.isLastPage());
            Log.i(TAG, "nextPage: " + lyrics.getNextPage());

            if (lyrics.isFirstPage()) {
                mLyrics.clear();
                if (lyrics.getLyrics().size() == 0) {
                    mNoLyrics.setVisibility(View.VISIBLE);
                }
            }
            if (!lyrics.isLastPage()) {
                mLoad.setVisibility(View.VISIBLE);
            }
            mNextPage = lyrics.getNextPage();
            mLyrics.addAll(lyrics.getLyrics());
            mAdapter.notifyDataSetChanged();
        }
        else{
            Utilities.showError(this, R.string.error_server_down);
        }
    }
}
