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
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
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
         * Invoked when a picture was pressed
         *
         * @param v The view
         */
        void onPictureItemViewPressed(View v);
    }

    private OnCheckedChangeListener mSelectionListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            for (CallbacksListener cb : mCallbacks) {
                cb.onPictureItemViewPressed(PictureItemView.this);
            }
        }
    };

    private List<CallbacksListener> mCallbacks;

    private Picture mPicture;

    private AsyncPictureLoaderTask mTask;

    private Animation mScaleInAnimation;
    private Animation mScaleOutAnimation;

    private ImageView mIcon;
    private CheckBox mCheckbox;
    private View mExpand;
    private View mOverlay;

    private boolean mInEditMode;
    private boolean mLongClickFired;

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

        mScaleInAnimation = new ScaleAnimation(
                1f, 0.98f, 1f, 0.98f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mScaleInAnimation.setFillAfter(true);
        mScaleInAnimation.setDuration(100L);
        mScaleInAnimation.setInterpolator(new AccelerateInterpolator());

        mScaleOutAnimation = new ScaleAnimation(
                0.98f, 1f, 0.98f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mScaleOutAnimation.setFillAfter(true);
        mScaleOutAnimation.setDuration(100L);
        mScaleOutAnimation.setInterpolator(new AccelerateInterpolator());

        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mLongClickFired = true;
                mScaleInAnimation.cancel();
                mScaleOutAnimation.cancel();
                for (CallbacksListener cb : mCallbacks) {
                    cb.onPictureItemViewPressed(PictureItemView.this);
                }
                return false;
            }
        });

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mInEditMode) {
                    final int action = MotionEventCompat.getActionMasked(event);
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            mLongClickFired = false;
                            mScaleOutAnimation.cancel();
                            startAnimation(mScaleInAnimation);
                            break;

                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_UP:
                            mScaleInAnimation.cancel();
                            startAnimation(mScaleOutAnimation);
                            if (action == MotionEvent.ACTION_UP && !mLongClickFired) {
                                playSoundEffect(SoundEffectConstants.CLICK);
                                performDisplayPicture();
                            }
                            break;
                    }
                }
                return false;
            }
        });
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
        if (mExpand == null) {
            mExpand = findViewById(R.id.picture_expand);
            mExpand.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    performDisplayPicture();
                }
            });
        }
        if (mOverlay == null) {
            mOverlay = findViewById(R.id.picture_overlay);
        }

        // Update the views
        setPicture(picture);
        mInEditMode = editMode;
        if (picture != null) {
            setSelected(picture.isSelected());
            mCheckbox.setOnCheckedChangeListener(null);
            mCheckbox.setChecked(picture.isSelected());
            mCheckbox.setVisibility(editMode ? View.VISIBLE : View.GONE);
            mCheckbox.setOnCheckedChangeListener(mSelectionListener);
            mExpand.setVisibility(editMode ? View.VISIBLE : View.GONE);
            mOverlay.setVisibility(editMode ? View.VISIBLE : View.GONE);

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

    private void performDisplayPicture() {
    }
}
