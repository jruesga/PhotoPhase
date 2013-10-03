/*
 * Copyright (C) 2013 Jorge Ruesga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ruesga.android.wallpapers.photophase.preferences;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.animations.AlbumsFlip3dAnimationController;
import com.ruesga.android.wallpapers.photophase.model.Album;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.widgets.AlbumInfo;
import com.ruesga.android.wallpapers.photophase.widgets.AlbumPictures;
import com.ruesga.android.wallpapers.photophase.widgets.CardLayout;
import com.ruesga.android.wallpapers.photophase.widgets.VerticalEndlessScroller;
import com.ruesga.android.wallpapers.photophase.widgets.VerticalEndlessScroller.OnEndScrollListener;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A fragment class for select the picture that will be displayed on the wallpaper
 */
public class ChoosePicturesFragment extends PreferenceFragment implements OnEndScrollListener {

    private static final String TAG = "ChoosePicturesFragment";

    private static final boolean DEBUG = false;

    private static final int AMOUNT_OF_ADDED_STEPS = 5;

    private final AsyncTask<Void, Album, Void> mAlbumsLoaderTask = new AsyncTask<Void, Album, Void>() {
        /**
         * {@inheritDoc}
         */
        @Override
        protected Void doInBackground(Void... params) {
            // Query all the external content and classify the pictures in albums and load the cards
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            Album album = null;
            unregister();
            Cursor c = mContentResolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            new String[]{ MediaStore.MediaColumns.DATA },
                            null,
                            null,
                            MediaStore.MediaColumns.DATA);
            if (c != null) {
                try {
                    if (DEBUG) Log.v(TAG, "Media library:");
                    while (c.moveToNext()) {
                        // Only valid files (those i can read)
                        String p = c.getString(0);
                        if (DEBUG) Log.v(TAG, "\t" + p);
                        if (p != null) {
                            File f = new File(p);
                            if (f.isFile() && f.canRead()) {
                                  File path = f.getParentFile();
                                  String name = path.getName();
                                  if (album == null || album.getPath().compareTo(path.getAbsolutePath()) != 0) {
                                      if (album != null) {
                                          mAlbums.add(album);
                                          mOriginalAlbums.add((Album)album.clone());
                                      }
                                      album = new Album();
                                      album.setPath(path.getAbsolutePath());
                                      album.setName(name);
                                      album.setDate(df.format(new Date(path.lastModified())));
                                      album.setSelected(isSelectedItem(album.getPath()));
                                      album.setItems(new ArrayList<String>());
                                      album.setSelectedItems(new ArrayList<String>());
                                  }
                                  album.getItems().add(f.getAbsolutePath());
                                  if (isSelectedItem(f.getAbsolutePath())) {
                                      album.getSelectedItems().add(f.getAbsolutePath());
                                  }
                                  Thread.yield();
                            }
                        }
                    }

                    // Add the last album
                    if (album != null) {
                        mAlbums.add(album);
                        mOriginalAlbums.add((Album)album.clone());
                    }

                } finally {
                    c.close();
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(Void result) {
            Resources res = getActivity().getResources();
            int size = (int)(res.getDimension(R.dimen.album_size) +
                    res.getDimension(R.dimen.small_margin));
            mScroller.setEndPadding(size * AMOUNT_OF_ADDED_STEPS);
            int height = mScroller.getMeasuredHeight();
            int steps = (height / size) + AMOUNT_OF_ADDED_STEPS;

            // Create the views an force a redraw the items
            mAlbumViews = new ArrayList<View>(mAlbums.size());
            for (Album item : mAlbums) {
                mAlbumViews.add(createAlbumView(item));
            }
            doEndScroll(steps, true);

            // Load in background
            loadInBackground(1500L);
        }
    };

    /*package*/ ContentResolver mContentResolver;

    /*package*/ List<Album> mAlbums;
    /*package*/ List<View> mAlbumViews;
    /*package*/ List<Album> mOriginalAlbums;
    /*package*/ List<AlbumsFlip3dAnimationController> mAnimationControllers;

    /*package*/ Set<String> mSelectedAlbums;
    private Set<String> mOriginalSelectedAlbums;

    /*package*/ VerticalEndlessScroller mScroller;
    private CardLayout mAlbumsPanel;

    /*package*/ boolean mSelectionChanged;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContentResolver = getActivity().getContentResolver();

        // Create an empty album
        mAlbums = new ArrayList<Album>();
        mOriginalAlbums = new ArrayList<Album>();
        mAnimationControllers = new ArrayList<AlbumsFlip3dAnimationController>();

        // Change the preference manager
        getPreferenceManager().setSharedPreferencesName(PreferencesProvider.PREFERENCES_FILE);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        // Load the albums user selection
        mOriginalSelectedAlbums = Preferences.Media.getSelectedMedia();
        mSelectedAlbums = new HashSet<String>(mOriginalSelectedAlbums);
        mSelectionChanged = false;

        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAlbumsLoaderTask.getStatus().compareTo(Status.PENDING) == 0) {
            mAlbumsLoaderTask.cancel(true);
        }
        unbindDrawables(mAlbumsPanel);
        unregister();

        // Notify that the settings was changed
        Intent intent = new Intent(PreferencesProvider.ACTION_SETTINGS_CHANGED);
        if (mSelectionChanged) {
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_REDRAW, Boolean.TRUE);
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_EMPTY_TEXTURE_QUEUE, Boolean.TRUE);
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_MEDIA_RELOAD, Boolean.TRUE);
        }
        getActivity().sendBroadcast(intent);
    }

    /*package*/ void unregister() {
        mAlbums.clear();
        mOriginalAlbums.clear();
        mAnimationControllers.clear();
    }

    /**
     * Method that unbind all the drawables for a view
     *
     * @param view The root view
     */
    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mScroller =
                (VerticalEndlessScroller)inflater.inflate(
                        R.layout.choose_picture_fragment, container, false);
        mScroller.setCallback(this);
        mAlbumsPanel = (CardLayout)mScroller.findViewById(R.id.albums_panel);

        // Force Hardware acceleration
        if (!mScroller.isHardwareAccelerated()) {
            mScroller.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        if (!mAlbumsPanel.isHardwareAccelerated()) {
            mAlbumsPanel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        // Load the albums
        mAlbumsLoaderTask.execute();

        return mScroller;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.albums, menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_ok:
                getActivity().finish();
                return true;
            case R.id.mnu_restore:
                restoreData();
                return true;
            case R.id.mnu_invert_all:
                invertAll();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Method that restores the albums to its original state
     */
    private void restoreData() {
        // Restore and the albums the selection
        mSelectedAlbums = new HashSet<String>(mOriginalSelectedAlbums);
        mAlbums.clear();
        for (Album album : mOriginalAlbums) {
            mAlbums.add((Album)album.clone());
        }

        // Update all the views
        Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
        updateAll();
    }

    /**
     * Method that inverts the selection of all the albums
     */
    private void invertAll() {
        // Restore and the albums the selection
        mSelectedAlbums = new HashSet<String>();
        for (Album album : mAlbums) {
            album.setSelected(!album.isSelected());
            album.setSelectedItems(new ArrayList<String>());
            if (album.isSelected()) {
                mSelectedAlbums.add(album.getPath());
            } else {
                mSelectedAlbums.addAll(album.getSelectedItems());
            }
        }

        // Update all the views
        Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
        updateAll();
    }

    /**
     * Method that updates the current state of all the albums
     */
    private void updateAll() {
        // Update every view (albums and views should have the same size)
        int count = mAlbumsPanel.getChildCount();
        for (int i = 0; i < count; i++) {
            Album album = mAlbums.get(i);
            View v = mAlbumsPanel.getChildAt(i);
            AlbumInfo albumInfo = (AlbumInfo)v.findViewById(R.id.album_info);
            AlbumPictures albumPictures = (AlbumPictures)v.findViewById(R.id.album_pictures);
            albumInfo.updateView(album);
            albumPictures.updateView(album);
        }

        // Restore the preference
        Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
        mSelectionChanged = true;

        // Restore all the animations states
        for (AlbumsFlip3dAnimationController controller : mAnimationControllers) {
            controller.reset();
        }
    }

    /**
     * Method that creates a new album to the card layout
     *
     * @param album The album to create
     * @return View The view create
     */
    View createAlbumView(Album album) {
        LayoutInflater li = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View albumView = li.inflate(R.layout.album, mAlbumsPanel, false);
        final AlbumInfo albumInfo = (AlbumInfo)albumView.findViewById(R.id.album_info);
        final AlbumPictures albumPictures = (AlbumPictures)albumView.findViewById(R.id.album_pictures);

        // Load the album info
        albumInfo.updateView(album);
        if (album.isSelected()) {
            albumInfo.setSelected(true);
        }
        albumInfo.addCallBackListener(new AlbumInfo.CallbacksListener() {
            @Override
            public void onAlbumSelected(Album ref) {
                // Remove all pictures of the album and add the album reference
                removeAlbumItems(ref);
                mSelectedAlbums.add(ref.getPath());
                ref.setSelected(true);
                albumPictures.updateView(ref);

                Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
                mSelectionChanged = true;
            }

            @Override
            public void onAlbumDeselected(Album ref) {
                // Remove all pictures of the album
                removeAlbumItems(ref);
                ref.setSelected(false);
                albumPictures.updateView(ref);

                Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
                mSelectionChanged = true;
            }


        });

        // Load the album picture data
        albumPictures.updateView(album);
        albumPictures.addCallBackListener(new AlbumPictures.CallbacksListener() {
            @Override
            public void onBackButtonClick(View v) {
                // Ignored
            }

            @Override
            public void onSelectionChanged(Album ref) {
                // Remove, add, and persist the selection
                removeAlbumItems(ref);
                mSelectedAlbums.addAll(ref.getSelectedItems());
                ref.setSelected(false);
                albumInfo.updateView(ref);

                Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
                mSelectionChanged = true;
            }
        });

        // Register the animation controller
        AlbumsFlip3dAnimationController controller = new AlbumsFlip3dAnimationController(albumInfo, albumPictures);
        controller.register();
        mAnimationControllers.add(controller);

        return albumView;
    }

    /**
     * Method that checks if an item is selected
     *
     * @param item The item
     * @return boolean if an item is selected
     */
    /*package*/ boolean isSelectedItem(String item) {
        Iterator<String> it = mSelectedAlbums.iterator();
        while (it.hasNext()) {
            String albumPath = it.next();
            if (item.compareTo(albumPath) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method that removes the reference to all the items and itself
     *
     * @param ref The album
     */
    /*package*/ void removeAlbumItems(Album ref) {
        Iterator<String> it = mSelectedAlbums.iterator();
        while (it.hasNext()) {
            String item = it.next();
            String parent = new File(item).getParent();
            if (parent.compareTo(ref.getPath()) == 0) {
                it.remove();
            } else if (item.compareTo(ref.getPath()) == 0) {
                it.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEndScroll() {
        doEndScroll(AMOUNT_OF_ADDED_STEPS, false);
    }

    /**
     * Method that performs a scroll creating new items
     *
     * @param amount The amount of items to create
     * @param animate If the add should be animated
     */
    /*package*/ synchronized void doEndScroll(int amount, boolean animate) {
        for (int i = 0; i < amount; i++) {
            //Add to the panel of cards
            if (mAlbumViews != null && !mAlbumViews.isEmpty()) {
                mAlbumsPanel.addCard(mAlbumViews.remove(0), animate);
            }
        }
    }

    /**
     * Method that load albums in background
     *
     * @param delay The delay time
     */
    /*package*/ void loadInBackground(long delay) {
        mAlbumsPanel.postDelayed(new Runnable() {
            @Override
            public void run() {
                doEndScroll(AMOUNT_OF_ADDED_STEPS, false);
                if (mAlbumViews != null && !mAlbumViews.isEmpty()) {
                    loadInBackground(300L);
                }
            }
        }, delay);
    }
}