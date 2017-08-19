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

import android.animation.Animator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.model.Album;
import com.ruesga.android.wallpapers.photophase.widgets.AlbumInfoView;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link ArrayAdapter} supporting "Google Now Card Layout" like layout
 */
public class AlbumCardUiAdapter extends ArrayAdapter<Album> {

    /**
     * A class that conforms with the ViewHolder pattern to performance
     * the list view rendering.
     */
    private static class ViewHolder {
        public ViewHolder() {
            super();
        }
        AlbumInfoView mAlbumInfoView;
        Animation mCardAnimation;
        Animator mCardAnimator;
    }

    private List<Album> mData = new ArrayList<>();

    private final AlbumInfoView.CallbacksListener mAlbumInfoCallback;
    private final AlbumInfoView.CastProxy mCastProxy;

    /**
     * Constructor of <code>AlbumCardUiAdapter</code>.
     *
     * @param context The current context
     * @param data The array with all the album data
     * @param castProxy A helper class to deal con cast status information
     * @param callback The album info view callback
     */
    public AlbumCardUiAdapter(Context context, List<Album> data, AlbumInfoView.CastProxy castProxy,
            AlbumInfoView.CallbacksListener callback) {
        super(context, R.layout.album_info, R.id.album_name, data);
        mData = data;
        mAlbumInfoCallback = callback;
        mCastProxy = castProxy;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        // Retrieve album
        final Album album = this.mData.get(position);

        // Check to reuse view
        View v = convertView;
        if (v == null) {
            //Create the view holder
            LayoutInflater li =
                    (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(R.layout.album_info, parent, false);

            // Create the controller for the view
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.mAlbumInfoView = v.findViewById(R.id.album_info);
            viewHolder.mAlbumInfoView.setAlbum(album);
            viewHolder.mAlbumInfoView.addCallBackListener(mAlbumInfoCallback);
            viewHolder.mAlbumInfoView.setCastProxy(mCastProxy);
            v.setTag(viewHolder);
        }

        // Retrieve the view holder
        ViewHolder viewHolder = (ViewHolder)v.getTag();

        // Retrieve the view holder and update the view
        viewHolder.mAlbumInfoView.updateView(album);

        // Animate the view?
        if (!album.isDisplayed()) {
            album.setDisplayed(true);

            // Reset the animation
            if (viewHolder.mCardAnimation != null) {
                viewHolder.mCardAnimation.cancel();
            }
            if (viewHolder.mCardAnimator != null) {
                viewHolder.mCardAnimator.cancel();
            }

            // Card Animation
            viewHolder.mCardAnimation = AnimationUtils.loadAnimation(
                    getContext(), R.anim.cards_slide_up);
            viewHolder.mCardAnimation.setStartOffset(Math.min(250, position * 30));
            v.startAnimation(viewHolder.mCardAnimation);
        }

        // Return the view
        return v;
    }

}
