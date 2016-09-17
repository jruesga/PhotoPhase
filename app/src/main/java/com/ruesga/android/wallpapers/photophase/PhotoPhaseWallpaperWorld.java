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

package com.ruesga.android.wallpapers.photophase;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.model.Disposition;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.textures.PhotoPhaseTextureManager;
import com.ruesga.android.wallpapers.photophase.transitions.Transition;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions.TRANSITIONS;
import com.ruesga.android.wallpapers.photophase.utils.DispositionUtil;
import com.ruesga.android.wallpapers.photophase.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents the wallpapers with all its photo frames.
 */
public class PhotoPhaseWallpaperWorld {

    private static final String TAG = "PhotoPhaseWPWorld";

    private static final boolean DEBUG = false;

    // The frame padding
    private static final int PHOTO_FRAME_PADDING = 2;

    private final Context mContext;
    private final PhotoPhaseTextureManager mTextureManager;

    private List<PhotoFrame> mPhotoFrames;
    private List<Transition> mTransitions;
    private final List<Transition> mUnusedTransitions;

    private List<Integer> mTransitionsQueue;
    private List<Integer> mUsedTransitionsQueue;
    private int mCurrent;

    private int mWidth;
    private int mHeight;

    private boolean mRecycled;

    private final String[] mPortraitDispositions;
    private final String[] mLandscapeDispositions;

    /**
     * Constructor <code>PhotoPhaseWallpaperWorld</code>
     *
     * @param ctx The current context
     * @param textureManager The texture manager
     */
    public PhotoPhaseWallpaperWorld(
            Context ctx, PhotoPhaseTextureManager textureManager) {
        super();
        mContext = ctx;
        mTextureManager = textureManager;
        mCurrent = -1;
        mUnusedTransitions = new ArrayList<>();
        mRecycled = false;

        Resources res = ctx.getResources();
        mPortraitDispositions = res.getStringArray(R.array.portrait_disposition_templates);
        mLandscapeDispositions = res.getStringArray(R.array.landscape_disposition_templates);
    }

    /**
     * Method that returns an unused transition for the type of transition
     *
     * @param type The type of transition
     * @return Transition The unused transition
     */
    private Transition getUnusedTransition(TRANSITIONS type) {
        for (Transition transition : mUnusedTransitions) {
            if (transition.getType().compareTo(type) == 0) {
                mUnusedTransitions.remove(transition);
                return transition;
            }
        }
        return null;
    }

    /**
     * Method that returns or creates a transition for the type of transition
     *
     * @param type The type of transition
     * @return Transition The unused transition
     */
    private Transition getOrCreateTransition(TRANSITIONS type) {
        Transition transition = getUnusedTransition(type);
        if (transition == null) {
            transition = Transitions.createTransition(mContext, mTextureManager, type);
        }
        transition.reset();
        return transition;
    }

    /**
     * Method that ensures the transitions queue
     */
    private void ensureTransitionsQueue() {
        if (mTransitionsQueue.isEmpty()) {
            mTransitionsQueue.addAll(mUsedTransitionsQueue);
            mUsedTransitionsQueue.clear();
        }
    }

    /**
     * Method that selects a transition and assign it to a random photo frame.
     */
    public void selectRandomTransition() {
        // Ensure queue
        ensureTransitionsQueue();
        if (mTransitionsQueue.isEmpty()) {
            return;
        }

        // Get a random frame to apply the transition to
        int item = Utils.getNextRandom(0, mTransitionsQueue.size() - 1);
        int pos = mTransitionsQueue.remove(item);
        mUsedTransitionsQueue.add(pos);
        PhotoFrame frame = mPhotoFrames.get(pos);

        // Select the transition
        selectTransition(frame, pos);
    }

    /**
     * Method that selects a transition and assign it to the photo frame.
     *
     * @param frame The photo frame to select
     */
    public void selectTransition(PhotoFrame frame) {
        // Ensure queue
        ensureTransitionsQueue();

        // Get a the frame to apply the transition to
        int pos = mPhotoFrames.indexOf(frame);
        if (pos == -1) {
            return;
        }
        mTransitionsQueue.remove(Integer.valueOf(pos));
        mUsedTransitionsQueue.add(pos);

        // Select the transition
        selectTransition(frame, pos);
    }

    /**
     * Method that selects a transition and assign it to a photo frame.
     *
     * @param frame The frame to select
     * @param pos The position
     */
    private void selectTransition(PhotoFrame frame, int pos) {
        // Create or use a transition
        Transition transition = null;
        boolean isSelectable = false;
        while (transition == null || !isSelectable) {
            boolean isRandom = Preferences.General.Transitions.getSelectedTransitions(mContext).isEmpty();
            TRANSITIONS type = Transitions.getNextTypeOfTransition(mContext);
            transition = getOrCreateTransition(type);
            isSelectable = transition.isSelectable(frame);
            if (!isSelectable) {
                mUnusedTransitions.add(transition);
                if (!isRandom) {
                    // If is not possible to select a valid transition then select a swap
                    // transition (this one doesn't relies on any selection)
                    transition = getOrCreateTransition(TRANSITIONS.SWAP);
                    isSelectable = true;
                }
            }
        }
        mTransitions.set(pos, transition);
        transition.select(frame);
        mCurrent = pos;
    }

    /**
     * Method that deselect the current transition.
     *
     * @param matrix The model-view-projection matrix
     * @param offset The current x offset
     */
    public void deselectTransition(float[] matrix, float offset) {
        if (mCurrent != -1 && mCurrent < mTransitions.size()) {
            // Retrieve the finally target
            Transition currentTransition = mTransitions.get(mCurrent);
            PhotoFrame currentTarget = currentTransition.getTarget();
            PhotoFrame finalTarget = currentTransition.getTransitionTarget();
            mUnusedTransitions.add(currentTransition);

            if (finalTarget != null) {
                Transition transition = getOrCreateTransition(TRANSITIONS.NO_TRANSITION);
                mTransitions.set(mCurrent, transition);

                currentTarget.recycle();
                mPhotoFrames.set(mCurrent, finalTarget);
                transition.select(finalTarget);

                // Draw the transition once
                transition.apply(matrix, offset);
            }
            mCurrent = -1;
        }
    }

    /**
     * Method that removes all internal references.
     */
    public void recycle() {
        // Destroy the previous world
        if (mTransitions != null) {
            int cc = mTransitions.size() - 1;
            for (int i = cc; i >= 0; i--) {
                Transition transition = mTransitions.get(i);
                transition.recycle();
                mTransitions.remove(i);
            }
        }
        mCurrent = -1;
        if (mUnusedTransitions != null) {
            int cc = mUnusedTransitions.size() - 1;
            for (int i = cc; i >= 0; i--) {
                Transition transition = mUnusedTransitions.get(i);
                transition.recycle();
                mUnusedTransitions.remove(i);
            }
        }
        if (mTransitionsQueue != null) {
            mTransitionsQueue.clear();
        }
        if (mUsedTransitionsQueue != null) {
            mUsedTransitionsQueue.clear();
        }
        mRecycled = true;
    }

    /**
     * Method that returns if there are any transition running in the world.
     *
     * @return boolean If there are any transition running in the world
     */
    public boolean hasRunningTransition() {
        if (mTransitions != null) {
            for (Transition transition : mTransitions) {
                if (transition.isRunning()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method that creates and fills the world with {@link PhotoFrame} objects.
     *
     * @param w The new width dimension
     * @param h The new height dimension
     */
    public synchronized void recreateWorld(int w, int h) {
        if (DEBUG) Log.d(TAG, "Recreating the world. New surface: " + w + "x" + h);

        // Destroy the previous world
        if (mRecycled) {
            recycle();
            mRecycled = false;
        }

        // Save the new dimensions of the wallpaper
        mWidth = w;
        mHeight = h;

        // Calculate the new world
        int orientation = mContext.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        int cols = portrait ? Preferences.Layout.getCols(mContext)
                : Preferences.Layout.getRows(mContext);
        int rows = portrait ? Preferences.Layout.getRows(mContext)
                : Preferences.Layout.getCols(mContext);
        float cellw = 2.0f / cols;
        float cellh = 2.0f / rows;
        List<Disposition> dispositions = getWorldDispositions(portrait);
        if (DEBUG) {
            Log.d(TAG, "Dispositions: " + dispositions.size() + " | " +
                    String.valueOf(dispositions));
        }
        mPhotoFrames = new ArrayList<>(dispositions.size());
        mTransitions = new ArrayList<>(dispositions.size());
        mTransitionsQueue = new ArrayList<>(dispositions.size());
        mUsedTransitionsQueue = new ArrayList<>(dispositions.size());
        int i = 0;
        int count = dispositions.size();
        for (Disposition disposition : dispositions) {
            // Create the photo frame
            float[] frameVertices = getVerticesFromDisposition(disposition, cellw, cellh);
            float[] photoVertices = getFramePadding(frameVertices,
                    portrait ? w : h, portrait ? h : w, count > 1);
            PhotoFrame frame =
                    new PhotoFrame(
                            disposition,
                            mTextureManager,
                            frameVertices,
                            photoVertices,
                            Colors.getInstance(mContext).getBackground());
            mPhotoFrames.add(frame);

            // Assign a null transition to the photo frame
            Transition transition = getOrCreateTransition(TRANSITIONS.NO_TRANSITION);
            transition.select(frame);
            mTransitions.add(transition);

            if (disposition.hasFlag(Disposition.BACKGROUND_FLAG) &&
                    disposition.hasFlag(Disposition.TRANSITION_FLAG)) {
                mTransitionsQueue.add(i);
            }
            i++;
        }
    }

    /**
     * Method that returns a photo frame from a coordinates in screen
     *
     * @param coordinates The coordinates
     * @return The photo frame reference or null if none found
     */
    public PhotoFrame getFrameFromCoordinates(PointF coordinates) {
        // Translate pixels coordinates to GLES coordinates
        float tx = ((coordinates.x * 2) / mWidth) - 1;
        float ty = (((coordinates.y * 2) / mHeight) - 1) * -1;

        // Locate the frame
        for (PhotoFrame frame : mPhotoFrames) {
            RectF vertex = Utils.rectFromVertex(frame.getPhotoVertex());
            if (vertex.left < tx && vertex.right > tx && vertex.top > ty && vertex.bottom < ty) {
                return frame;
            }
        }
        return null;
    }

    /**
     * Method that draws all the photo frames.
     *
     * @param matrix The model-view-projection matrix
     * @param offset The current x offset
     */
    public void draw(float[] matrix, float offset) {
        // Apply every transition
        if (mTransitions != null) {
            // First draw the non-running transitions; then the active ones
            for (Transition transition : mTransitions) {
                // Don't draw frames with no background flagged
                if (!transition.getTarget().getDisposition().hasFlag(Disposition.BACKGROUND_FLAG)) {
                    continue;
                }

                if (!transition.isRunning()) {
                    transition.apply(matrix, offset);
                }
            }
            for (Transition transition : mTransitions) {
                // Don't draw frames with no background flagged
                if (!transition.getTarget().getDisposition().hasFlag(Disposition.BACKGROUND_FLAG)) {
                    continue;
                }

                if (transition.isRunning()) {
                    transition.apply(matrix, offset);
                }
            }
        }
    }

    /**
     * Method that returns a coordinates per vertex array from a disposition
     *
     * @param disposition The source disposition
     * @param cellw The cell width based on the surface
     * @param cellh The cell height based on the surface
     * @return float[] The coordinates per vertex array
     */
    private static float[] getVerticesFromDisposition(
            Disposition disposition, float cellw, float cellh) {
        return new float[]
                {
                    // bottom left
                    -1.0f + (disposition.x * cellw),
                     1.0f - ((disposition.y * cellh) + (disposition.h * cellh)),

                    // bottom right
                    -1.0f + ((disposition.x * cellw) + (disposition.w * cellw)),
                     1.0f - ((disposition.y * cellh) + (disposition.h * cellh)),

                    // top left
                    -1.0f + (disposition.x * cellw),
                     1.0f - (disposition.y * cellh),

                    // top right
                    -1.0f + ((disposition.x * cellw) + (disposition.w * cellw)),
                     1.0f - (disposition.y * cellh)
                };
    }

    /**
     * Method that applies a padding to the frame
     *
     * @param coords The source coordinates
     * @param screenWidth The screen width
     * @param screenHeight The screen height
     * @return float[] The new coordinates
     */
    private float[] getFramePadding(float[] coords, int screenWidth, int screenHeight,
            boolean needsFramePadding) {
        float[] paddingCoords = new float[coords.length];
        System.arraycopy(coords, 0, paddingCoords, 0, coords.length);
        if (needsFramePadding && Preferences.General.isFrameSpacer(mContext)) {
            final float pxw = (1 / (float) screenWidth) * PHOTO_FRAME_PADDING;
            final float pxh = (1 / (float) screenHeight) * PHOTO_FRAME_PADDING;
            paddingCoords[0] += pxw;
            paddingCoords[1] += pxh;
            paddingCoords[2] -= pxw;
            paddingCoords[3] += pxh;
            paddingCoords[4] += pxw;
            paddingCoords[5] -= pxh;
            paddingCoords[6] -= pxw;
            paddingCoords[7] -= pxh;
        }
        return paddingCoords;
    }

    /**
     * Method that returns the dispositions to draw in the world
     *
     * @param portrait If the orientation is portrait (true) or landscape (false)
     * @return List<Disposition> The list of dispositions
     */
    private List<Disposition> getWorldDispositions(boolean portrait) {
        // If user selected a random disposition, then use one of the predefined layouts
        if (Preferences.Layout.isRandomDispositions(mContext)) {
            // Random
            if (portrait) {
                // Portrait
                int next = Utils.getNextRandom(0, mPortraitDispositions.length -1);
                return DispositionUtil.toDispositions(mPortraitDispositions[next]);
            }
            // Landscape
            int next = Utils.getNextRandom(0, mLandscapeDispositions.length -1);
            return DispositionUtil.toDispositions(mLandscapeDispositions[next]);
        }
        // User-defined
        return portrait
                ? Preferences.Layout.getPortraitDisposition(mContext)
                : Preferences.Layout.getLandscapeDisposition(mContext);
    }
}
