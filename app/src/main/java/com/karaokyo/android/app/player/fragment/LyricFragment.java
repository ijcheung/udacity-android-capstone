package com.karaokyo.android.app.player.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.activity.LyricSearchActivity;
import com.karaokyo.android.app.player.activity.MainActivity;
import com.karaokyo.android.app.player.helper.SongAdapter;
import com.karaokyo.android.app.player.model.Song;
import com.karaokyo.android.app.player.provider.LyricContract;
import com.karaokyo.android.app.player.task.LyricScanTaskFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * A fragment representing a list of Items.
 */
public class LyricFragment extends ListFragment {
    private static final String TAG = "LyricFragment";
    private static final String TAG_SCAN_TASK_FRAGMENT = "scan_task_fragment";

    private static final String ARG_SECTION_NUMBER = "section_number";

    private LyricScanTaskFragment mScanTaskFragment;

    private ListView mList;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ArrayList<Song> mLyrics;

    private SongAdapter mAdapter;

    private OnFragmentInteractionListener mListener;

    private View mRefresh;
    private MenuItem mRefreshItem;
    private boolean mIsRefreshing;
    private Animation mRotate;

    public static LyricFragment newInstance(int sectionNumber) {
        LyricFragment fragment = new LyricFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public LyricFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getActivity().getSupportFragmentManager();
        mScanTaskFragment = (LyricScanTaskFragment) fm
                .findFragmentByTag(TAG_SCAN_TASK_FRAGMENT);

        if(mScanTaskFragment == null){
            mScanTaskFragment = new LyricScanTaskFragment();
            fm.beginTransaction()
                    .add(mScanTaskFragment, TAG_SCAN_TASK_FRAGMENT)
                    .commit();
        }

        mIsRefreshing = mScanTaskFragment.isScanning();

        mRotate = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate);
        mRotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if(!mIsRefreshing){
                    if(mRefreshItem.getActionView() != null){
                        mRefreshItem.getActionView().clearAnimation();
                    }
                    mRefreshItem.setActionView(null);
                }
            }
        });

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRefresh = inflater.inflate(R.layout.refresh, null);

        mLyrics = getLyricList();
        mAdapter = new SongAdapter(getActivity(), mLyrics);
        setListAdapter(mAdapter);

        /*getListView().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mListener.onFragmentInteraction(songs.get(Integer.parseInt(view.getTag().toString())));
                return true;
            }
        });*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_list, null);

        mList = (ListView) root.findViewById(android.R.id.list);
        mSwipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipe_refresh_layout);

        mSwipeRefreshLayout.setColorSchemeResources(R.color.accent);
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                startLyricScanTask();
            }
        });

        mList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition =
                        (mList == null || mList.getChildCount() == 0) ?
                                0 : mList.getChildAt(0).getTop();
                mSwipeRefreshLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
            }
        });

        TextView empty = (TextView) root.findViewById(android.R.id.empty);
        empty.setText(R.string.empty_lyrics);

        FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), LyricSearchActivity.class));
            }
        });

        return root;
    }

    /*@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.lyric, menu);
        mRefreshItem = menu.findItem(R.id.action_refresh);

        if(mIsRefreshing){
            mRefreshItem.setActionView(mRefresh);
            mRefresh.startAnimation(mRotate);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_refresh:
                if(!mIsRefreshing) {
                    mRefreshItem.setActionView(mRefresh);
                    mRefresh.startAnimation(mRotate);
                    startLyricScanTask();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        ((MainActivity) context).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        /*if(mRefreshItem != null && mRefreshItem.getActionView() != null){
            mRefreshItem.getActionView().clearAnimation();
            mRefreshItem.setActionView(null);
        }*/
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                mListener.onLyricFragmentInteraction(mLyrics.get(i));
                return true;
            }
        });
        setHasOptionsMenu(true);

        //Auto scan if there are no lyrics from the provider
        if(mLyrics.size() == 0){
            startLyricScanTask();
        }
    }

    @Override
    public void onStart() {
        refreshLyrics();

        super.onStart();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (null != mListener) {
            mListener.onLyricFragmentInteraction(mLyrics.get(position));
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onLyricFragmentInteraction(Song song);
    }

    public void deleteLyric(Song song){
        mLyrics.remove(song);
        mAdapter.notifyDataSetChanged();
    }

    public void onLyricScanTaskComplete(){
        mIsRefreshing = false;
        mSwipeRefreshLayout.setRefreshing(false);
        refreshLyrics();
    }

    private ArrayList<Song> getLyricList() {
        ArrayList<Song> songs = new ArrayList<Song>();
        ContentResolver lyricResolver = getActivity().getContentResolver();
        Uri lyricUri = LyricContract.CONTENT_URI;
        Cursor lyricCursor = lyricResolver.query(lyricUri, null, null, null, null);
        if(lyricCursor != null && lyricCursor.moveToFirst()){
            int titleColumn = lyricCursor.getColumnIndex(LyricContract.TITLE);
            int artistColumn = lyricCursor.getColumnIndex(LyricContract.ARTIST);
            int songIdColumn = lyricCursor.getColumnIndex(LyricContract.SONG_ID);
            int filepathColumn = lyricCursor.getColumnIndex(LyricContract.FILEPATH);
            do {
                String title = lyricCursor.getString(titleColumn);
                String artist = lyricCursor.getString(artistColumn);
                long songId = lyricCursor.getLong(songIdColumn);
                String filepath = lyricCursor.getString(filepathColumn);
                songs.add(new Song(title, artist, songId, filepath));
            }
            while(lyricCursor.moveToNext());
            lyricCursor.close();
        }

        Collections.sort(songs, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        return songs;
    }

    private void startLyricScanTask(){
        mIsRefreshing = true;

        mScanTaskFragment.execute(getActivity());
    }

    private void refreshLyrics(){
        mLyrics.clear();
        mLyrics.addAll(getLyricList());
        mAdapter.notifyDataSetChanged();
    }
}