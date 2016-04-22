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

package com.ruesga.android.wallpapers.photophase.widgets;

import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.model.Picture;
import com.ruesga.android.wallpapers.photophase.tasks.AsyncPictureLoaderTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A view that contains the view of the picture of an abbum
 */
public class PictureItemView extends FrameLayout {

    /**
     * A convenient listener for receive events of the PictureItemView class
     *
     */
    public interface CallbacksListener {
        /**
         * Invoked when a picture was selected
         *
         * @param v The view
         */
        void onPictureSelected(View v);

        /**
         * Invoked when an picture was deselected
         *
         * @param v The view
         */
        void onPictureDeselected(View v);
    }

    private OnCheckedChangeListener mSelectionListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            for (CallbacksListener cb : mCallbacks) {
                if (isChecked) {
                    cb.onPictureSelected(PictureItemView.this);
                } else {
                    cb.onPictureDeselected(PictureItemView.this);
                }
            }
        }
    };

    private List<CallbacksListener> mCallbacks;

    private Picture mPicture;

    private AsyncPictureLoaderTask mTask;

    private ImageView mIcon;
    private CheckBox mCheckbox;

    /**
     * Constructor of <code>PictureItemView</code>.
     *
     * @param context The current context
     */
    public PictureItemView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>PictureItemView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public PictureItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>PictureItemView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public PictureItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the internal references
     */
    private void init() {
        mCallbacks = new ArrayList<>();
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
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Cancel pending tasks
        if (mTask != null && mTask.getStatus().compareTo(Status.PENDING) == 0) {
            mTask.cancel(true);
        }
    }

    /**
     * Method that returns the picture
     *
     * @return Picture The picture
     */
    public Picture getPicture() {
        return mPicture;
    }

    /**
     * Method that sets the picture
     *
     * @param picture The picture
     */
    public void setPicture(Picture picture) {
        mPicture = picture;
    }

    /**
     * Method that updates the view
     *
     * @param picture The picture data
     */
    public void updateView(final Picture picture, boolean editMode, boolean refreshIcon) {
        // Destroy the update drawable task
        if (mTask != null && (mTask.getStatus() == AsyncTask.Status.RUNNING ||
                mTask.getStatus() == AsyncTask.Status.PENDING)) {
            mTask.cancel(true);
        }

        // Retrieve the views references
        if (mIcon == null) {
            mIcon = (ImageView) findViewById(R.id.picture_thumbnail);
        }
        if (mCheckbox == null) {
            mCheckbox = (CheckBox) findViewById(R.id.picture_selector);
        }

        // Update the views
        setPicture(picture);
        if (picture != null) {
            setSelected(picture.isSelected());
            mCheckbox.setOnCheckedChangeListener(null);
            mCheckbox.setChecked(picture.isSelected());
            mCheckbox.setVisibility(editMode ? View.VISIBLE : View.GONE);
            mCheckbox.setOnCheckedChangeListener(mSelectionListener);

            // Do no try to cache the images (this generates a lot of memory and we want
            // to have a low memory footprint)
            if (refreshIcon) {
                mIcon.setImageDrawable(null);

                // Show as icon, the first picture
                int minSize = (int) getResources().getDimension(R.dimen.picture_size);
                mTask = new AsyncPictureLoaderTask(getContext(), mIcon, minSize, minSize, null);
                mTask.execute(new File(picture.getPath()));
            }
        }
    }

}
