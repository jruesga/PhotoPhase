/*
 * Copyright (C) 2014 Jorge Ruesga
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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceFragment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.adapters.AlbumCardUiAdapter;
import com.ruesga.android.wallpapers.photophase.adapters.AlbumPictureAdapter;
import com.ruesga.android.wallpapers.photophase.model.Album;
import com.ruesga.android.wallpapers.photophase.model.Picture;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.widgets.AlbumInfoView;
import com.ruesga.android.wallpapers.photophase.widgets.PictureItemView;

import java.io.File;
import java.io.IOException;
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
public class ChoosePicturesFragment extends PreferenceFragment
        implements AlbumInfoView.CallbacksListener, OnClickListener, OnBackPressedListener {

    private static final String TAG = "ChoosePicturesFragment";

    private static final boolean DEBUG = false;

    private static final int PROGRESS_STEPS = 5;

    // The album loader task
    private final AsyncTask<Void, Album, Void> mTask = new AsyncTask<Void, Album, Void>() {

        private DateFormat mDateFormat;

        /**
         * {@inheritDoc}
         */
        @Override
        protected Void doInBackground(Void... params) {
            // Query all the external content and classify the pictures in albums and load the cards
            mDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            Cursor c = getActivity().getContentResolver().query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            new String[]{ MediaStore.MediaColumns.DATA },
                            null,
                            null,
                            MediaStore.MediaColumns.DATA);
            if (c != null) {
                try {
                    long start = System.currentTimeMillis();
                    if (DEBUG) Log.v(TAG, "Media library:");
                    int count = 0;
                    List<Album> pending = new ArrayList<Album>();
                    List<Album> all = new ArrayList<Album>();
                    Album album = null;
                    while (c.moveToNext()) {
                        album = processPath(all, pending, album, c.getString(0));
                        count++;
                        if (count % PROGRESS_STEPS == 0) {
                            // Notify and clean
                            publishProgress(pending.toArray(new Album[pending.size()]));
                            pending.clear();
                        }
                    }

                    // Add the last albums
                    if (album != null) {
                     // Add to global structures
                        all.add(album);
                        mOriginalAlbums.add((Album)album.clone());

                        // Add to local structures and notify
                        pending.add(album);

                        // Notify
                        publishProgress(pending.toArray(new Album[pending.size()]));
                    }
                    long end = System.currentTimeMillis();
                    if (DEBUG) Log.v(TAG, "Library loaded in " + (end - start) + " miliseconds");

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
        protected void onProgressUpdate(Album... albums) {
            for (Album album : albums) {
                mAlbums.add(album);
            }
            mAlbumAdapter.notifyDataSetChanged();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(Void result) {
            mAlbumAdapter.notifyDataSetChanged();
        }

        /**
         * Method that process a album path
         *
         * @param all All the albums
         * @param pending Pending albums to notify
         * @param data The current album data
         * @param path The path to analyze
         * @return Album The new current album
         */
        private Album processPath(List<Album> all, List<Album> pending, Album data, String path) {
            // Only valid files (those i can read)
            if (DEBUG) Log.v(TAG, "\t" + path);
            Album album = data;
            if (path != null) {
                File f = new File(path);
                if (f.isFile() && f.canRead()) {
                      File p = f.getParentFile();
                      String name = p.getName();
                      if (album == null || album.getPath().compareTo(p.getAbsolutePath()) != 0) {
                          if (album != null) {
                              // Add to global structures
                              all.add(album);
                              mOriginalAlbums.add((Album)album.clone());

                              // Add to local structures and notify
                              pending.add(album);
                          }
                          album = new Album();
                          album.setPath(p.getAbsolutePath());
                          album.setName(name);
                          album.setDate(mDateFormat.format(new Date(p.lastModified())));
                          album.setSelected(isSelectedItem(album.getPath()));
                          album.setItems(new ArrayList<Picture>());
                          album.setSelectedItems(new ArrayList<String>());
                      }
                      boolean selected = isSelectedItem(f.getAbsolutePath());
                      album.getItems().add(new Picture(f.getAbsolutePath(), selected));
                      if (selected) {
                          album.getSelectedItems().add(f.getAbsolutePath());
                      }
                }
            }
            return album;
        }

        /**
         * Method that checks if an item is selected
         *
         * @param item The item
         * @return boolean if an item is selected
         */
        private boolean isSelectedItem(String item) {
            if (mSelectedAlbums != null) {
                Iterator<String> it = mSelectedAlbums.iterator();
                while (it.hasNext()) {
                    String albumPath = it.next();
                    if (item.compareTo(albumPath) == 0) {
                        return true;
                    }
                }
            }
            return false;
        }
    };

    private final Handler.Callback mCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_LOAD_PICTURES) {
                loadPictures((Album)msg.obj);
                return true;
            }
            return false;
        }
    };

    OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mShowingAlbums) {
                onHeaderPressed(parent, view, position);
            } else {
                onPicturePressed(view);
            }
        }
    };

    private static final int MSG_LOAD_PICTURES = 1;

    List<Album> mAlbums;
    List<Album> mOriginalAlbums;

    Set<String> mSelectedAlbums;
    private Set<String> mOriginalSelectedAlbums;

    ViewGroup mContainer;

    ListView mAlbumsPanel;
    AlbumCardUiAdapter mAlbumAdapter;

    GridView mPicturesPanel;
    AlbumPictureAdapter mPictureAdapter;

    private boolean mSelectionChanged;

    // Animation references
    ViewGroup mSrcParent;
    View mSrcView;
    ViewGroup mDstParent;
    View mDstView;

    Album mAlbum;

    private int mPicturesAnimDurationIn;
    private int mPicturesAnimDurationOut;

    Handler mHandler;
    LayoutInflater mInflater;

    boolean mShowingAlbums;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler(mCallback);
        mShowingAlbums = true;

        // Create an empty album
        mAlbums = new ArrayList<Album>();
        mOriginalAlbums = new ArrayList<Album>();

        // Change the preference manager
        getPreferenceManager().setSharedPreferencesName(PreferencesProvider.PREFERENCES_FILE);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        // Load the albums user selection
        mOriginalSelectedAlbums = removeObsoleteAlbumsData(Preferences.Media.getSelectedMedia());
        mSelectedAlbums = new HashSet<String>(mOriginalSelectedAlbums);
        mSelectionChanged = false;

        final Resources res = getResources();
        mPicturesAnimDurationIn = res.getInteger(R.integer.pictures_anim_in);
        mPicturesAnimDurationOut = res.getInteger(R.integer.pictures_anim_out);

        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        unbindDrawables(mAlbumsPanel);
        unregister();

        if (!mShowingAlbums) {
            mPicturesPanel.setVisibility(View.GONE);
            mDstView.setVisibility(View.GONE);
            mDstParent.removeView(mPicturesPanel);
            mDstParent.removeView(mDstView);
        }

        // Notify that the settings was changed
        Intent intent = new Intent(PreferencesProvider.ACTION_SETTINGS_CHANGED);
        if (mSelectionChanged) {
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_REDRAW, Boolean.TRUE);
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_EMPTY_TEXTURE_QUEUE, Boolean.TRUE);
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_MEDIA_RELOAD, Boolean.TRUE);
        }
        getActivity().sendBroadcast(intent);

        super.onDestroy();
    }

    private void unregister() {
        mAlbums.clear();
        mOriginalAlbums.clear();
    }

    /**
     * Method that unbind all the drawables for a view
     *
     * @param view The root view
     */
    void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            Bundle savedInstanceState) {

        mContainer = container;
        mInflater = inflater;

        // Inflate the layout for this fragment
        FrameLayout root =
                (FrameLayout)mInflater.inflate(
                        R.layout.choose_picture_fragment, container, false);
        mAlbumsPanel = (ListView)root.findViewById(R.id.albums_panel);
        mAlbumsPanel.setSmoothScrollbarEnabled(true);
        mAlbumAdapter = new AlbumCardUiAdapter(getActivity(), mAlbumsPanel, mAlbums, this);
        mAlbumsPanel.setAdapter(mAlbumAdapter);
        mAlbumsPanel.setOnItemClickListener(mOnItemClickListener);

        // Force Hardware acceleration
        if (!root.isHardwareAccelerated()) {
            root.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        if (!mAlbumsPanel.isHardwareAccelerated()) {
            mAlbumsPanel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        // Load the albums
        unregister();

        return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mTask.execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetach() {
        if (mTask.getStatus().compareTo(Status.PENDING) == 0) {
            mTask.cancel(true);
        }
        super.onDetach();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        // Hide the albums picture with animation
        if (v.equals(mDstView)) {
            hideAlbumPictures(mDstParent, mDstView, mSrcParent, mSrcView);
        }
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
            case R.id.mnu_invert:
                if (mShowingAlbums) {
                    invertAll();
                } else {
                    invertAlbum(mAlbum);
                }
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
        int count = mAlbums.size();
        for (int i = 0; i < count ; i++) {
            Album album = mAlbums.get(i);
            Album originalAlbum = mOriginalAlbums.get(i);

            // Update selected status
            album.setSelected(originalAlbum.isSelected());
            album.setItems(new ArrayList<Picture>(originalAlbum.getItems()));
            album.setSelectedItems(new ArrayList<String>(originalAlbum.getSelectedItems()));
        }
        mAlbumAdapter.notifyDataSetChanged();

        // Update settings
        Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
        mSelectionChanged = true;

        if (!mShowingAlbums) {
            hideAlbumPictures(mDstParent, mDstView, mSrcParent, mSrcView);
        }
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
            for (Picture picture : album.getItems()) {
                picture.setSelected(false);
            }
        }
        mAlbumAdapter.notifyDataSetChanged();

        // Update settings
        Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
        mSelectionChanged = true;
    }

    /**
     * Method that inverts the selection of an album
     *
     * @param album The album which to invert its selection
     */
    private void invertAlbum(Album album) {
        // Remove all pictures of the album
        removeAlbumItems(album);
        List<String> origSelectedItems = new ArrayList<String>(album.getSelectedItems());
        List<String> selectedItems =  album.getSelectedItems();
        album.getSelectedItems().clear();
        for (Picture picture : album.getItems()) {
            boolean selected = !origSelectedItems.contains(picture.getPath());
            if (selected) {
                selectedItems.add(picture.getPath());
            }
            picture.setSelected(selected);
        }

        // Notify pictures dataset changed
        mPictureAdapter.notifyDataSetChanged();
        updateAlbumInfo(mDstView, album);

        mSelectedAlbums.addAll(album.getSelectedItems());
        Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
        mSelectionChanged = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAlbumSelected(Album album) {
        updateAlbumSelection(album, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAlbumDeselected(Album album) {
        updateAlbumSelection(album, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAllPicturesSelected(Album album) {
        updateAllPicturesSelection(album, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAllPicturesDeselected(Album album) {
        updateAllPicturesSelection(album, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onBackPressed() {
        if (!mShowingAlbums) {
            // Hide album pictures
            hideAlbumPictures(mDstParent, mDstView, mSrcParent, mSrcView);
            return true;
        }
        return false;
    }

    /**
     * Method that update the album selection
     *
     * @param album The album to update
     * @param selected If the album is selected
     */
    private void updateAlbumSelection(Album album, boolean selected) {
        // Remove all pictures of the album
        removeAlbumItems(album);
        album.setSelected(selected);
        album.getSelectedItems().clear();
        for (Picture picture : album.getItems()) {
            picture.setSelected(false);
        }
        if (selected) {
            mSelectedAlbums.add(album.getPath());
        }

        if (!mShowingAlbums) {
            // Notify pictures dataset changed
            updateAlbumInfo(mDstView, album);
            mPictureAdapter.notifyDataSetChanged();
        } else {
            mAlbumAdapter.notifyDataSetChanged();
        }

        Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
        mSelectionChanged = true;
    }

    /**
     * Method that update the whole picture selection
     *
     * @param album The album to update
     * @param selected If all the picture were selected
     */
    private void updateAllPicturesSelection(Album album, boolean selected) {
        // Remove all pictures of the album
        removeAlbumItems(album);
        List<String> selectedItems = album.getSelectedItems();
        selectedItems.clear();
        for (Picture picture : album.getItems()) {
            if (selected) {
                selectedItems.add(picture.getPath());
            }
            picture.setSelected(selected);
        }
        album.setSelected(false);

        // Notify pictures dataset changed
        mPictureAdapter.notifyDataSetChanged();
        updateAlbumInfo(mDstView, album);

        mSelectedAlbums.addAll(album.getSelectedItems());
        Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
        mSelectionChanged = true;
    }

    /**
     * Method that removes the reference to all the items and itself
     *
     * @param ref The album
     */
    private void removeAlbumItems(Album ref) {
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
     * Method that shows the album pictures while animating the view
     *
     * @param srcParent The source parent view
     * @param srcView The source view
     * @param dstParent The destination parent view
     * @param dstView The destination view
     */
    void showAlbumPictures(final ViewGroup srcParent, final View srcView,
            final ViewGroup dstParent, final View dstView) {

        // Hide the source view
        srcView.setAlpha(0.0f);

        // Animation from bottom to top
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(dstView, "translationY",
                srcView.getY(), srcParent.getPaddingTop());
        anim1.setDuration(mPicturesAnimDurationIn);
        anim1.setInterpolator(new AccelerateDecelerateInterpolator());
        anim1.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Ignore
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // Ignore
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Re-layout the view in its new position
                dstView.setOnClickListener(ChoosePicturesFragment.this);

                // And now finally show the new album pictures layout
                // and fill it
                mPicturesPanel.setVisibility(View.VISIBLE);
                mHandler.sendMessage(mHandler.obtainMessage(MSG_LOAD_PICTURES, mAlbum));
                mShowingAlbums = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // Ignore
            }
        });
        anim1.start();

        // Hide the parent view
        AlphaAnimation anim2 = new AlphaAnimation(1.0f, 0.0f);
        anim2.setDuration(mPicturesAnimDurationIn);
        anim2.setFillAfter(true);
        anim2.setZAdjustment(Animation.ZORDER_BOTTOM);
        anim2.setInterpolator(new AccelerateDecelerateInterpolator());
        srcParent.setEnabled(false);
        srcParent.startAnimation(anim2);

        // Save the references
        mSrcParent = srcParent;
        mSrcView = srcView;
        mDstParent = dstParent;
        mDstView = dstView;
    }

    /**
     * Method that hides the album pictures while animating the view
     *
     * @param srcParent The source parent view
     * @param srcView The source view
     * @param dstParent The destination parent view
     * @param dstView The destination view
     */
    void hideAlbumPictures(final ViewGroup srcParent, final View srcView,
            final ViewGroup dstParent, final View dstView) {

        mPicturesPanel.setVisibility(View.INVISIBLE);

        // Animation from top to bottom
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(srcView, "translationY",
                dstParent.getPaddingTop(), dstView.getY());
        anim1.setDuration(mPicturesAnimDurationOut);
        anim1.setInterpolator(new AccelerateDecelerateInterpolator());
        anim1.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Ignore
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // Ignore
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Remove the source view and show the destination view
                srcParent.removeView(srcView);
                dstView.setAlpha(1.0f);
                dstParent.setEnabled(true);
                unbindDrawables(mPicturesPanel);
                srcParent.removeView(mPicturesPanel);
                mAlbumAdapter.notifyDataSetChanged();

                mShowingAlbums = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // Ignore
            }
        });
        anim1.start();

        // Hide the parent view
        AlphaAnimation anim2 = new AlphaAnimation(0.0f, 1.0f);
        anim2.setDuration(mPicturesAnimDurationOut);
        anim2.setFillAfter(true);
        anim2.setZAdjustment(Animation.ZORDER_BOTTOM);
        anim2.setInterpolator(new AccelerateDecelerateInterpolator());
        dstParent.startAnimation(anim2);
    }

    /**
     * Method that updates an album info
     *
     * @param v The header view
     * @param album The album data
     */
    void updateAlbumInfo(View v, Album album) {
        final Resources res = getResources();

        AlbumInfoView info = (AlbumInfoView)v.findViewById(R.id.album_info);
        info.setAlbum(album);

        ImageView icon = (ImageView)info.findViewById(R.id.album_thumbnail);
        TextView name = (TextView)info.findViewById(R.id.album_name);
        TextView items = (TextView)info.findViewById(R.id.album_items);
        TextView selectedItems = (TextView)info.findViewById(R.id.album_selected_items);

        icon.setImageDrawable(album.getIcon());
        name.setText(album.getName());

        int size = album.getItems().size();
        items.setText(String.format(res.getQuantityText(
                R.plurals.album_number_of_pictures, size).toString(), Integer.valueOf(size)));

        int selected = album.getSelectedItems().size();
        String count = String.valueOf(selected);
        if (selected > 99) {
            count = "99+";
        }
        selectedItems.setText(count);
        selectedItems.setVisibility(!album.isSelected() ? View.VISIBLE : View.INVISIBLE);
        info.setSelected(album.isSelected());
    }

    /**
     * Method that load all the pictures of the album
     *
     * @param album
     */
    void loadPictures(Album album) {
        List<Picture> items = album.getItems();

        // Calculate the grid dimensions
        final Resources res = getResources();
        int pictureWidth = (int)res.getDimension(R.dimen.picture_size);
        int gridWidth = mPicturesPanel.getWidth();
        int columns = gridWidth / pictureWidth;
        int space = (gridWidth / pictureWidth) / (columns - 1);
        if (columns < items.size()) {
            space = (int)res.getDimension(R.dimen.small_padding);
        }
        mPicturesPanel.setHorizontalSpacing(space);
        mPicturesPanel.setVerticalSpacing(space);
        mPicturesPanel.setStretchMode(GridView.STRETCH_SPACING);
        mPicturesPanel.setColumnWidth(pictureWidth);
        mPicturesPanel.setNumColumns(columns);

        mPictureAdapter = new AlbumPictureAdapter(getActivity(), items, mPicturesPanel);
        mPicturesPanel.setAdapter(mPictureAdapter);
    }

    /**
     * Method invoked when an album header was pressed
     *
     * @param parent The parent view
     * @param view The header view
     * @param position The position
     */
    void onHeaderPressed(AdapterView<?> parent, View view, int position) {
        mAlbum = mAlbumAdapter.getItem(position);

        // Header view
        View header = mInflater.inflate(R.layout.album_info, null);
        header.setTranslationY(view.getY() + parent.getPaddingTop());
        header.setLayoutParams(new ViewGroup.LayoutParams(view.getWidth(), view.getHeight()));

        // Pictures view
        mPicturesPanel = (GridView) mInflater.inflate(R.layout.pictures_view, null);
        if (!mPicturesPanel.isHardwareAccelerated()) {
            mPicturesPanel.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        mPicturesPanel.setY(view.getHeight());
        mPicturesPanel.setLayoutParams(new ViewGroup.LayoutParams(view.getWidth(),
                parent.getHeight() - view.getHeight() -
                parent.getPaddingTop()  - parent.getPaddingBottom()));
        mPicturesPanel.setVisibility(View.INVISIBLE);
        mPicturesPanel.setOnItemClickListener(mOnItemClickListener);

        // Add to the container
        mContainer.addView(mPicturesPanel);
        mContainer.addView(header);

        // Update and display the album pictures view
        AlbumInfoView info = (AlbumInfoView)header.findViewById(R.id.album_info);
        info.addCallBackListener(ChoosePicturesFragment.this);
        info.setAlbumMode(false);
        updateAlbumInfo(header, mAlbum);
        showAlbumPictures(parent, view, mContainer, header);
    }

    /**
     * Method invoked when a picture view was pressed
     *
     * @param view The picture view
     */
    void onPicturePressed(View view) {
        PictureItemView pictureView = (PictureItemView)view.findViewById(R.id.picture);
        if (pictureView != null) {
            Picture picture = pictureView.getPicture();
            removeAlbumItems(mAlbum);
            List<String> selectedItems = mAlbum.getSelectedItems();
            if (selectedItems.contains(picture.getPath())) {
                selectedItems.remove(picture.getPath());
                picture.setSelected(false);
            } else {
                selectedItems.add(picture.getPath());
                picture.setSelected(true);
                mAlbum.setSelected(false);
            }

            // Notify all the views
            pictureView.updateView(picture, false);
            updateAlbumInfo(mDstView, mAlbum);
            mAlbumAdapter.notifyDataSetChanged();

            // Update settings
            mSelectedAlbums.addAll(mAlbum.getSelectedItems());
            Preferences.Media.setSelectedMedia(getActivity(), mSelectedAlbums);
            mSelectionChanged = true;
        }
    }

    /**
     * Method that removes all the inexistent albums and pictures
     *
     * @param data The data to filter
     * @return Set<String> The data filtered
     */
    private Set<String> removeObsoleteAlbumsData(Set<String> data) {
        Set<String> validDataList = new HashSet<String>();
        Iterator<String> it = data.iterator();
        while (it.hasNext()) {
            File f = new File(it.next());
            if (f.exists()) {
                try {
                    validDataList.add(f.getCanonicalPath());
                } catch (IOException ioex) {
                    // Ignore
                }
            }
        }
        if (data.size() != validDataList.size()) {
            // Obsolete entries were removed
            data.clear();
            data.addAll(validDataList);
            Preferences.Media.setSelectedMedia(getActivity(), mOriginalSelectedAlbums);
        }
        return data;
    }
}
