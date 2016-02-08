package com.karaokyo.android.app.player.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
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
import com.karaokyo.android.app.player.helper.PlaylistDataSource;
import com.karaokyo.android.app.player.model.Playlist;
import com.karaokyo.android.app.player.model.Song;
import com.karaokyo.android.app.player.util.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class PlaylistsFragment extends ListFragment {
    private static final String TAG = "PlaylistsFragment";

    private static final String ARG_SECTION_NUMBER = "section_number";

    private ArrayList<Playlist> playlists;

    private OnFragmentInteractionListener mListener;

    private PlaylistAdapter mAdapter;
    private PlaylistDataSource dataSource;

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

        dataSource = new PlaylistDataSource(getActivity());
        dataSource.open();

        playlists = dataSource.getAllPlaylists();
        Collections.sort(playlists, new Comparator<Playlist>() {
            public int compare(Playlist a, Playlist b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        dataSource.close();

        mAdapter = new PlaylistAdapter(getActivity(), playlists);

        setListAdapter(mAdapter);
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

                                if(!TextUtils.isEmpty(filename)) {
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
                                            dataSource.open();
                                            playlists.add(dataSource.createPlaylist(filename));
                                            dataSource.close();

                                            notifyDataSetChanged();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                else{
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

    /*@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.playlists, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_new:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        if(activity instanceof MainActivity) {
            ((MainActivity) activity).onSectionAttached(
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
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                mListener.onPlaylistsFragmentInteraction(playlists.get(i), true);
                return true;
            }
        });
        setHasOptionsMenu(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (null != mListener) {
            mListener.onPlaylistsFragmentInteraction(playlists.get(position), false);
        }
    }

    public void deletePlaylist(Playlist playlist){
        playlists.remove(playlist);
        mAdapter.notifyDataSetChanged();
    }

    public void notifyDataSetChanged(){
        sortList();
        mAdapter.notifyDataSetChanged();
    }

    private void sortList(){
        Collections.sort(playlists, new Comparator<Playlist>() {
            public int compare(Playlist a, Playlist b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
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
