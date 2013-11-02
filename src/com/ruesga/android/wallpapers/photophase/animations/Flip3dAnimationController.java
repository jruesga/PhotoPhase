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
package com.ruesga.android.wallpapers.photophase.animations;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

/**
 * A class that manages a flip 3d effect of two views
 */
public class Flip3dAnimationController {

    private static final int DURATION = 200;

    View mFront;
    View mBack;
    boolean mFrontFace;

    final Object mLock = new Object();

    /**
     * Constructor of <code>Flip3dAnimationController</code>
     *
     * @param front The front view
     * @param back The back view
     */
    public Flip3dAnimationController(View front, View back) {
        super();
        mFront = front;
        mBack = back;
        mBack.setVisibility(View.GONE);
        mFrontFace = true;
    }

    public boolean isFrontFace() {
        return mFrontFace;
    }

    /**
     * Method that reset the controller
     */
    public void reset() {
        changeToFrontFace(true);
    }

    /**
     * Method that change the view to the front face
     *
     * @param animate Do with animation
     */
    public void changeToFrontFace(boolean animate) {
        if (!mFrontFace) {
            if (animate) {
                applyAnimation(true);
            } else {
                mBack.setVisibility(View.GONE);
                mFront.setVisibility(View.VISIBLE);
                mFrontFace = true;
            }
        }
    }

    /**
     * Method that change the view to the back face
     *
     * @param animate Do with animation
     */
    public void changeToBackFace(boolean animate) {
        if (mFrontFace) {
            if (animate) {
                applyAnimation(true);
            } else {
                mFront.setVisibility(View.GONE);
                mBack.setVisibility(View.VISIBLE);
                mFrontFace = false;
            }
        }
    }

    /**
     * Method that applies the animation over the views
     *
     * @param inverse Applies the inverse animation
     */
    void applyAnimation(boolean inverse) {
        applyTransformation(getFrontView(), 0, 90 * (inverse ? -1 : 1), true);
    }

    void applyTransformation(final View v, float start, float end, final boolean step1) {
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
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Ignore
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                synchronized (mLock) {
                    getFrontView().setAnimation(null);
                    getBackView().setAnimation(null);
                    if (step1) {
                        getFrontView().setVisibility(View.INVISIBLE);
                        applyTransformation(getBackView(),
                                -90 * (!mFrontFace ? -1 : 1), 0, false);
                    } else {
                        mFrontFace = !mFrontFace;
                        getBackView().setVisibility(View.GONE);
                    }
                }
            }
        });
        v.startAnimation(anim);
    }

    View getFrontView() {
        return mFrontFace ? mFront : mBack;
    }

    View getBackView() {
        return !mFrontFace ? mFront : mBack;
    }
}

