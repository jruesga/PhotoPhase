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

import org.cyanogenmod.wallpapers.photophase.TextureManager;
import org.cyanogenmod.wallpapers.photophase.transitions.Transitions.TRANSITIONS;

/**
 * A simple transition that swap an image after the transition time is ended.
 */
public class SwapTransition extends NullTransition {

    private static final float TRANSITION_TIME = 250.0f;

    private boolean mRunning;
    private long mTime;

    /**
     * Constructor of <code>SwapTransition</code>
     *
     * @param ctx The current context
     * @param tm The texture manager
     */
    public SwapTransition(Context ctx, TextureManager tm) {
        super(ctx, tm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TRANSITIONS getType() {
        return TRANSITIONS.SWAP;
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

        // Calculate the delta time
        final float delta = Math.min(SystemClock.uptimeMillis() - mTime, TRANSITION_TIME) / TRANSITION_TIME;

        // Apply the transition
        boolean ended = delta == 1;
        draw(ended ? mTransitionTarget : mTarget, matrix);
        mRunning = !ended;
    }

}
