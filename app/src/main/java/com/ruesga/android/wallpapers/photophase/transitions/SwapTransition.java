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

package com.ruesga.android.wallpapers.photophase.transitions;

import android.content.Context;

import com.ruesga.android.wallpapers.photophase.textures.TextureManager;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions.TRANSITIONS;

/**
 * A simple transition that swap an image after the transition time is ended.
 */
public class SwapTransition extends NullTransition {

    private static final float TRANSITION_TIME = 250.0f;

    public SwapTransition(Context ctx, TextureManager tm) {
        super(ctx, tm);
    }

    @Override
    public TRANSITIONS getType() {
        return TRANSITIONS.SWAP;
    }

    @Override
    public float getTransitionTime() {
        return TRANSITION_TIME;
    }

    @Override
    public boolean hasTransitionTarget() {
        return true;
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public void applyTransition(float delta, float[] matrix) {
        draw(delta < 1f ? mTransitionTarget : mTarget, matrix);
    }

}
