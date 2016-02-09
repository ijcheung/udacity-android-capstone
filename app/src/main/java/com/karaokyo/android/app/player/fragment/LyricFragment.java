package com.karaokyo.android.app.player.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.activity.LyricSearchActivity;
import com.karaokyo.android.app.player.activity.MainActivity;
import com.karaokyo.android.app.player.helper.LyricLoader;
import com.karaokyo.android.app.player.helper.SongAdapter;
import com.karaokyo.android.app.player.model.Song;
import com.karaokyo.android.app.player.task.LyricScanTaskFragment;

/**
 * A fragment representing a list of Items.
 */
public class LyricFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "LyricFragment";
    private static final String TAG_SCAN_TASK_FRAGMENT = "scan_task_fragment";

    private static final String ARG_SECTION_NUMBER = "section_number";

    private LyricScanTaskFragment mScanTaskFragment;

    private ListView mList;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Cursor mCursor;

    private SongAdapter mAdapter;

    private OnFragmentInteractionListener mListener;

    private boolean mIsRefreshing;

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

        getLoaderManager().initLoader(0, null, this);
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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
                mCursor.moveToPosition(position);
                Song lyric = new Song(mCursor.getString(LyricLoader.Query.TITLE),
                        mCursor.getString(LyricLoader.Query.ARTIST),
                        mCursor.getLong(LyricLoader.Query.SONG_ID),
                        mCursor.getString(LyricLoader.Query.FILEPATH));
                mListener.onLyricFragmentInteraction(lyric);
                return true;
            }
        });
        setHasOptionsMenu(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (null != mListener) {
            mCursor.moveToPosition(position);
            Song lyric = new Song(mCursor.getString(LyricLoader.Query.TITLE),
                    mCursor.getString(LyricLoader.Query.ARTIST),
                    mCursor.getLong(LyricLoader.Query.SONG_ID),
                    mCursor.getString(LyricLoader.Query.FILEPATH));
            mListener.onLyricFragmentInteraction(lyric);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new LyricLoader(getContext());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursor = cursor;
        mAdapter = new SongAdapter(getContext(), cursor, SongAdapter.Type.LYRICS);
        setListAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        setListAdapter(null);
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

    public void onLyricScanTaskComplete(){
        mIsRefreshing = false;
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void startLyricScanTask(){
        mIsRefreshing = true;

        mScanTaskFragment.execute(getActivity());
    }
}