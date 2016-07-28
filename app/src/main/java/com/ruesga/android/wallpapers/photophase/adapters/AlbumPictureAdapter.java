/*
 * Copyright (C) 2015 Jorge Ruesga
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

package com.ruesga.android.wallpapers.photophase.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.model.Album;
import com.ruesga.android.wallpapers.photophase.model.Picture;
import com.ruesga.android.wallpapers.photophase.widgets.PictureItemView;
import com.ruesga.android.wallpapers.photophase.widgets.PictureItemView.CallbacksListener;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link ArrayAdapter} an album picture
 */
public class AlbumPictureAdapter extends ArrayAdapter<Picture> {

    /**
     * A class that conforms with the ViewHolder pattern to performance
     * the list view rendering.
     */
    private static class ViewHolder {
        public ViewHolder() {
            super();
        }
        PictureItemView mPictureItemView;
    }

    private final Album mAlbum;
    private List<Picture> mData = new ArrayList<>();
    private final AdapterView<?> mParent;
    private final CallbacksListener mCallback;

    /**
     * Constructor of <code>AlbumPictureAdapter</code>.
     *
     * @param context The current context
     * @param data The pictures data
     */
    public AlbumPictureAdapter(Context context, Album album, List<Picture> data,
            AdapterView<?> parent, CallbacksListener cb) {
        super(context, R.layout.picture_item, R.id.photo, data);
        mAlbum = album;
        mData = data;
        mParent = parent;
        mCallback = cb;
    }

    public void notifyViewChanged() {
        int count = mParent.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = mParent.getChildAt(i);
            getView(mParent.getPositionForView(v), v, mParent, false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent, true);
    }

    /**
     * Method that returns a view for a position
     *
     * @param position The position
     * @param convertView A reusable view or null
     * @param parent The parent of the view
     * @param refreshIcon If the view should refresh its icon
     * @return View The view
     */
    private View getView(int position, View convertView, ViewGroup parent, boolean refreshIcon) {
        final Picture picture = this.mData.get(position);

        // Check to reuse view
        View v = convertView;
        if (v == null) {
            //Create the view holder
            LayoutInflater li = LayoutInflater.from(getContext());
            v = li.inflate(R.layout.picture_item, parent, false);

            // Create the controller for the view
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.mPictureItemView = (PictureItemView) v.findViewById(R.id.picture);
            viewHolder.mPictureItemView.setPicture(picture);
            viewHolder.mPictureItemView.addCallBackListener(mCallback);
            v.setTag(viewHolder);
        }

        // Retrieve the view holder
        ViewHolder viewHolder = (ViewHolder)v.getTag();

        // Retrieve the view holder and update the view
        final boolean editMode = mAlbum.getSelectedItems().size() > 0;
        viewHolder.mPictureItemView.updateView(picture, editMode, refreshIcon);

        // Return the view
        return v;
    }

}
