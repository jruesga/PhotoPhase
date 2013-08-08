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

package org.cyanogenmod.wallpapers.photophase.transitions;

import android.content.Context;
import android.opengl.GLException;
import android.os.SystemClock;

import org.cyanogenmod.wallpapers.photophase.Colors;
import org.cyanogenmod.wallpapers.photophase.PhotoFrame;
import org.cyanogenmod.wallpapers.photophase.TextureManager;
import org.cyanogenmod.wallpapers.photophase.shapes.ColorShape;
import org.cyanogenmod.wallpapers.photophase.transitions.Transitions.TRANSITIONS;

/**
 * A transition that applies a fade transition to the picture.
 */
public class FadeTransition extends NullTransition {

    private static final float TRANSITION_TIME = 600.0f;

    private boolean mRunning;
    private long mTime;

    ColorShape mOverlay;

    /**
     * Constructor of <code>FadeTransition</code>
     *
     * @param ctx The current context
     * @param tm The texture manager
     */
    public FadeTransition(Context ctx, TextureManager tm) {
        super(ctx, tm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TRANSITIONS getType() {
        return TRANSITIONS.FADE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasTransitionTarget() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunning() {
        return mRunning;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();
        mTime = -1;
        mRunning = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void select(PhotoFrame target) {
        super.select(target);
        mOverlay = new ColorShape(mContext, target.getFrameVertex(), Colors.getBackground());
        mOverlay.setAlpha(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void apply(float[] matrix) throws GLException {
        // Check internal vars
        if (mTarget == null ||
            mTarget.getPositionBuffer() == null ||
            mTarget.getTextureBuffer() == null) {
            return;
        }
        if (mTransitionTarget == null ||
            mTransitionTarget.getPositionBuffer() == null ||
            mTransitionTarget.getTextureBuffer() == null) {
            return;
        }

        // Set the time the first time
        if (mTime == -1) {
            mTime = SystemClock.uptimeMillis();
        }

        final float delta = Math.min(SystemClock.uptimeMillis() - mTime, TRANSITION_TIME) / TRANSITION_TIME;
        if (delta <= 0.5) {
            // Draw the src target
            draw(mTarget, matrix);
            mOverlay.setAlpha(delta * 2.0f);
        } else {
            // Draw the dst target
            draw(mTransitionTarget, matrix);
            mOverlay.setAlpha((1 - delta) * 2.0f);
        }
        mOverlay.draw(matrix);

        // Transition ended
        if (delta == 1) {
            mRunning = false;
        }
    }

}
