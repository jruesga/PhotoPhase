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
package org.cyanogenmod.wallpapers.photophase.animations;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

import org.cyanogenmod.wallpapers.photophase.model.Album;
import org.cyanogenmod.wallpapers.photophase.widgets.AlbumInfo;
import org.cyanogenmod.wallpapers.photophase.widgets.AlbumPictures;
import org.cyanogenmod.wallpapers.photophase.widgets.AlbumPictures.CallbacksListener;

/**
 * A class that manages a flip 3d effect of an album
 */
public class AlbumsFlip3dAnimationController {

    private static final int DURATION = 200;

    View mFront;
    View mBack;
    boolean mFrontFace;

    /**
     * Constructor of <code>AlbumsFlip3dAnimationController</code>
     *
     * @param front The front view
     * @param back The back view
     */
    public AlbumsFlip3dAnimationController(AlbumInfo front, AlbumPictures back) {
        super();
        mFront = front;
        mBack = back;
        mBack.setVisibility(View.GONE);
        mFrontFace = true;
    }

    /**
     * Method that register the controller
     */
    public void register() {
        getFrontView().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getBackView().setVisibility(View.INVISIBLE);
                applyAnimation(false);
            }
        });
        ((AlbumPictures)getBackView()).addCallBackListener(new CallbacksListener() {
            @Override
            public void onBackButtonClick(View v) {
                getBackView().setVisibility(View.INVISIBLE);
                applyAnimation(true);
            }

            @Override
            public void onSelectionChanged(Album album) {
                // Ignore
            }
        });
    }

    /**
     * Method that unregister the controller
     */
    public void unregister() {
        getFrontView().setOnClickListener(null);
        getBackView().setOnClickListener(null);
    }

    /**
     * Method that reset the controller
     */
    public void reset() {
        if (!mFrontFace) {
            applyAnimation(true);
        }
    }

    /**
     * Method that applies the animation over the views
     *
     * @param inverse Applies the inverse animation
     */
    /*package*/ void applyAnimation(boolean inverse) {
        applyTransformation(getFrontView(), 0, 90 * (inverse ? -1 : 1), true);
    }

    /*package*/ void applyTransformation(final View v, float start, float end, final boolean step1) {
        final float centerX = v.getWidth() / 2.0f;
        final float centerY = v.getHeight() / 2.0f;

        final Flip3dAnimation anim = new Flip3dAnimation(start, end, centerX, centerY);
        anim.setDuration(DURATION);
        anim.setFillAfter(true);
        anim.setInterpolator(new AccelerateInterpolator());

        anim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (!step1) {
                    getBackView().setVisibility(View.VISIBLE);
                }
                getFrontView().setOnClickListener(null);
                getBackView().setOnClickListener(null);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Ignore
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                getFrontView().setAnimation(null);
                getBackView().setAnimation(null);
                if (step1) {
                    getFrontView().setVisibility(View.INVISIBLE);
                    applyTransformation(getBackView(), -90 * (!mFrontFace ? -1 : 1), 0, false);
                } else {
                    mFrontFace = !mFrontFace;
                    getBackView().setVisibility(View.GONE);
                    if (mFrontFace) {
                        getFrontView().setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getBackView().setVisibility(View.INVISIBLE);
                                applyAnimation(false);
                            }
                        });
                    } else {
                        ((AlbumPictures)getFrontView()).onShow();
                    }
                }
            }
        });
        v.startAnimation(anim);
    }

    /*package*/ View getFrontView() {
        return mFrontFace ? mFront : mBack;
    }

    /*package*/ View getBackView() {
        return !mFrontFace ? mFront : mBack;
    }
}

