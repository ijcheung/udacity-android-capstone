package com.karaokyo.android.app.player.fragment;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.activity.MainActivity;
import com.karaokyo.android.app.player.helper.PlaylistAdapter;
import com.karaokyo.android.app.player.helper.PlaylistLoader;
import com.karaokyo.android.app.player.model.Playlist;
import com.karaokyo.android.app.player.model.Song;
import com.karaokyo.android.app.player.provider.PlaylistContract;
import com.karaokyo.android.app.player.util.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class PlaylistsFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "PlaylistsFragment";

    private static final String ARG_SECTION_NUMBER = "section_number";

    private OnFragmentInteractionListener mListener;

    private Cursor mCursor;
    private PlaylistAdapter mAdapter;

    public static PlaylistsFragment newInstance() {
        return new PlaylistsFragment();
    }

    public static PlaylistsFragment newInstance(int sectionNumber) {
        PlaylistsFragment fragment = new PlaylistsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public PlaylistsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_list, null);

        TextView empty = (TextView) root.findViewById(android.R.id.empty);
        empty.setText(R.string.empty_playlists);

        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setEnabled(false);

        FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(getContext());
                input.setSingleLine(true);

                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.new_playlist)
                        .setView(input)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String filename = input.getText().toString();

                                if (!TextUtils.isEmpty(filename)) {
                                    File newFile = new File(getContext().getFilesDir(), filename);
                                    if (newFile.exists()) {
                                        Utilities.showError(getActivity(), R.string.error_playlist_name_exists);
                                    } else {
                                        try {
                                            //Create File
                                            ObjectOutputStream out = new ObjectOutputStream(getContext().openFileOutput(filename, 0));
                                            out.writeObject(new ArrayList<Song>());
                                            out.close();

                                            //Add to DB
                                            ContentValues values = new ContentValues();
                                            values.put(PlaylistContract.TITLE, filename);
                                            getContext().getContentResolver().insert(PlaylistContract.CONTENT_URI, values);

                                            notifyDataSetChanged();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else {
                                    Utilities.showError(getActivity(), R.string.error_playlist_name_empty);
                                }
                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
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
        if(context instanceof MainActivity) {
            ((MainActivity) context).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
                mCursor.moveToPosition(position);
                Playlist playlist = new Playlist(mCursor.getLong(PlaylistLoader.Query._ID),
                        mCursor.getString(PlaylistLoader.Query.TITLE));
                mListener.onPlaylistsFragmentInteraction(playlist, true);
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
            Playlist playlist = new Playlist(mCursor.getLong(PlaylistLoader.Query._ID),
                    mCursor.getString(PlaylistLoader.Query.TITLE));
            mListener.onPlaylistsFragmentInteraction(playlist, false);
        }
    }

    public void notifyDataSetChanged(){
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new PlaylistLoader(getContext());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursor = cursor;
        mAdapter = new PlaylistAdapter(getContext(), cursor);
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
    * <p>
    * See the Android Training lesson <a href=
    * "http://developer.android.com/training/basics/fragments/communicating.html"
    * >Communicating with Other Fragments</a> for more information.
    */
    public interface OnFragmentInteractionListener {
        public void onPlaylistsFragmentInteraction(Playlist playlist, boolean isLongClick);
    }
}
