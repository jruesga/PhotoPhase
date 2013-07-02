/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package org.cyanogenmod.wallpapers.photophase.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.cyanogenmod.wallpapers.photophase.R;
import org.cyanogenmod.wallpapers.photophase.model.Album;

import java.util.ArrayList;
import java.util.List;

/**
 * A view that contains the pictures of an album
 */
public class AlbumPictures extends RelativeLayout
    implements OnClickListener, OnMenuItemClickListener {

    private static final int SELECTION_SELECT_ALL = 1;
    private static final int SELECTION_DESELECT_ALL = 2;
    private static final int SELECTION_INVERT = 3;

    /**
     * A convenient listener for receive events of the AlbumPictures class
     *
     */
    public interface CallbacksListener {
        /**
         * Invoked when the user pressed the back button
         */
        void onBackButtonClick(View v);

        /**
         * Invoked when the selection was changed
         *
         * @param album The album
         */
        void onSelectionChanged(Album album);
    }

    private List<CallbacksListener> mCallbacks;

    private PicturesView mScroller;
    private LinearLayout mHolder;
    private View mBackButton;
    private View mOverflowButton;

    private Album mAlbum;

    /**
     * Constructor of <code>AlbumPictures</code>.
     *
     * @param context The current context
     */
    public AlbumPictures(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>AlbumPictures</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public AlbumPictures(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>AlbumPictures</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public AlbumPictures(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the internal references
     */
    private void init() {
        mCallbacks = new ArrayList<AlbumPictures.CallbacksListener>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mScroller = (PicturesView)findViewById(R.id.album_pictures_scroller);
        mHolder = (LinearLayout)findViewById(R.id.album_pictures_holder);
        mBackButton = findViewById(R.id.back);
        mBackButton.setOnClickListener(this);
        mOverflowButton = findViewById(R.id.overflow);
        mOverflowButton.setOnClickListener(this);
        TextView title = (TextView)findViewById(R.id.album_pictures_title);
        title.setText(mAlbum.getName());

        updateView(mAlbum);
    }

    /**
     * Method that adds the class that will be listen for events of this class
     *
     * @param callback The callback class
     */
    public void addCallBackListener(CallbacksListener callback) {
        this.mCallbacks.add(callback);
    }

    /**
     * Method that removes the class from the current callbacks
     *
     * @param callback The callback class
     */
    public void removeCallBackListener(CallbacksListener callback) {
        this.mCallbacks.remove(callback);
    }

    /**
     * Method that set the data of the view
     *
     * @param album The album data
     */
    public void updateView(Album album) {
        mAlbum = album;

        if (mHolder != null) {
            int pictures = mHolder.getChildCount();
            if (pictures != album.getItems().size()) {
                // Recreate the pictures
                final LayoutInflater inflater = (LayoutInflater) getContext().
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                mScroller.cancelTasks();
                mHolder.removeAllViews();
                for (final String picture : mAlbum.getItems()) {
                    View v = createPicture(inflater, picture, isPictureSelected(picture));
                    mHolder.addView(v);
                }
            } else {
                int i = 0;
                for (final String picture : mAlbum.getItems()) {
                    View v = mHolder.getChildAt(i);
                    v.setSelected(isPictureSelected(picture));
                    i++;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        // Check which is the view pressed
        if (v.equals(mBackButton)) {
            for (CallbacksListener callback : mCallbacks) {
                callback.onBackButtonClick(v);
            }
            return;
        }
        if (v.equals(mOverflowButton)) {
            PopupMenu popup = new PopupMenu(getContext(), v);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.pictures_actions, popup.getMenu());
            popup.setOnMenuItemClickListener(this);
            popup.show();
            return;
        }

        // A picture view
        v.setSelected(!v.isSelected());
        notifySelectionChanged();
    }

    /**
     * Method that notifies to all the registered callbacks that the selection
     * was changed
     */
    private void notifySelectionChanged() {
        List<String> selection = new ArrayList<String>();
        int count = mHolder.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = mHolder.getChildAt(i);
            if (v.isSelected()) {
                selection.add((String)v.getTag());
            }
        }
        mAlbum.setSelectedItems(selection);
        mAlbum.setSelected(false);

        for (CallbacksListener callback : mCallbacks) {
            callback.onSelectionChanged(mAlbum);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_select_all:
                doSelection(SELECTION_SELECT_ALL);
                break;

            case R.id.mnu_deselect_all:
                doSelection(SELECTION_DESELECT_ALL);
                break;

            case R.id.mnu_invert_selection:
                doSelection(SELECTION_INVERT);
                break;

            default:
                return false;
        }
        return true;
    }

    /**
     * Operate over the selection of the pictures of this album.
     *
     * @param action Takes the next values:
     * <ul>
     * <li>SELECTION_SELECT_ALL: select all</li>
     * <li>SELECTION_DESELECT_ALL: deselect all</li>
     * <li>SELECTION_INVERT: invert selection</li>
     * </ul>
     */
    private void doSelection(int action) {
        int count = mHolder.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = mHolder.getChildAt(i);

            boolean selected = true;
            if (action == SELECTION_DESELECT_ALL) {
                selected = false;
            } else if (action == SELECTION_INVERT) {
                selected = !v.isSelected();
            }
            v.setSelected(selected);
        }
        notifySelectionChanged();
    }

    /**
     * Method invoked when the view is displayed
     */
    public void onShow() {
        mScroller.requestLoadOfPendingPictures();
    }

    /**
     * Method that creates a new picture view
     *
     * @param inflater The inflater of the parent view
     * @param picture The path of the picture
     * @param selected If the picture is selected
     */
    private View createPicture(LayoutInflater inflater, String picture, boolean selected) {
        final View v = inflater.inflate(R.layout.picture_item, mHolder, false);
        v.setTag(picture);
        v.setSelected(selected);
        v.setOnClickListener(this);
        return v;
    }

    /**
     * Method that check if a picture is selected
     *
     * @param picture The picture to check
     * @return boolean whether the picture is selected
     */
    private boolean isPictureSelected(String picture) {
        for (String item : mAlbum.getSelectedItems()) {
            if (item.compareTo(picture) == 0) {
                return true;
            }
        }
        return false;
    }
}
