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
import android.content.res.Resources;
import android.os.AsyncTask.Status;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.cyanogenmod.wallpapers.photophase.R;
import org.cyanogenmod.wallpapers.photophase.model.Album;
import org.cyanogenmod.wallpapers.photophase.tasks.AsyncPictureLoaderTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A view that contains the info about an album
 */
public class AlbumInfo extends RelativeLayout
    implements OnClickListener, OnMenuItemClickListener {

    /**
     * A convenient listener for receive events of the AlbumPictures class
     *
     */
    public interface CallbacksListener {
        /**
         * Invoked when an album was selected
         *
         * @param album The album
         */
        void onAlbumSelected(Album album);

        /**
         * Invoked when an album was deselected
         *
         * @param album The album
         */
        void onAlbumDeselected(Album album);
    }

    private List<CallbacksListener> mCallbacks;

    /*package*/ Album mAlbum;

    /*package*/ AsyncPictureLoaderTask mTask;

    /*package*/ ImageView mIcon;
    private TextView mSelectedItems;
    private TextView mName;
    private TextView mItems;
    private View mOverflowButton;

    /**
     * Constructor of <code>AlbumInfo</code>.
     *
     * @param context The current context
     */
    public AlbumInfo(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>AlbumInfo</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public AlbumInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>AlbumInfo</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public AlbumInfo(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the internal references
     */
    private void init() {
        mCallbacks = new ArrayList<AlbumInfo.CallbacksListener>();
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
     * {@inheritDoc}
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mIcon = (ImageView)findViewById(R.id.album_thumbnail);
        mSelectedItems = (TextView)findViewById(R.id.album_selected_items);
        mName = (TextView)findViewById(R.id.album_name);
        mItems = (TextView)findViewById(R.id.album_items);
        mOverflowButton = findViewById(R.id.overflow);
        mOverflowButton.setOnClickListener(this);

        updateView(mAlbum);

        post(new Runnable() {
            @Override
            public void run() {
                // Show as icon, the first picture
                mTask = new AsyncPictureLoaderTask(getContext(), mIcon);
                mTask.execute(new File(mAlbum.getItems().get(0)));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Cancel pending tasks
        if (mTask.getStatus().compareTo(Status.PENDING) == 0) {
            mTask.cancel(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        if (v.equals(mOverflowButton)) {
            PopupMenu popup = new PopupMenu(getContext(), v);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.album_actions, popup.getMenu());
            onPreparePopupMenu(popup.getMenu());
            popup.setOnMenuItemClickListener(this);
            popup.show();
            return;
        }
    }

    /**
     * Method called prior to show the popup menu
     *
     * @param popup The popup menu
     */
    public void onPreparePopupMenu(Menu popup) {
        if (isSelected()) {
            popup.findItem(R.id.mnu_select_album).setVisible(false);
        } else {
            popup.findItem(R.id.mnu_deselect_album).setVisible(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_select_album:
                doSelection(true);
                break;

            case R.id.mnu_deselect_album:
                doSelection(false);
                break;

            default:
                return false;
        }
        return true;
    }

    /**
     * Method that select/deselect the album
     *
     * @param selected whether the album is selected
     */
    public void doSelection(boolean selected) {
        setSelected(selected);
        mAlbum.setSelected(selected);
        mAlbum.setSelectedItems(new ArrayList<String>());
        updateView(mAlbum);
        notifySelectionChanged();
    }

    /**
     * Method that notifies to all the registered callbacks that the selection
     * was changed
     */
    private void notifySelectionChanged() {
        for (CallbacksListener callback : mCallbacks) {
            if (mAlbum.isSelected()) {
                callback.onAlbumSelected(mAlbum);
            } else {
                callback.onAlbumDeselected(mAlbum);
            }
        }
    }

    /**
     * Method that updates the view
     *
     * @param album The album data
     */
    @SuppressWarnings("boxing")
    public void updateView(Album album) {
        mAlbum = album;

        if (mIcon != null) {
            Resources res = getContext().getResources();

            String count = String.valueOf(mAlbum.getSelectedItems().size());
            if (mAlbum.getItems().size() > 99) {
                count += "+";
            }
            mSelectedItems.setText(count);
            mSelectedItems.setVisibility(mAlbum.isSelected() ? View.INVISIBLE : View.VISIBLE);
            mName.setText(mAlbum.getName());
            int items = mAlbum.getItems().size();
            mItems.setText(String.format(res.getQuantityText(
                    R.plurals.album_number_of_pictures, items).toString(), items));
            setSelected(album.isSelected());
        }
    }
}
