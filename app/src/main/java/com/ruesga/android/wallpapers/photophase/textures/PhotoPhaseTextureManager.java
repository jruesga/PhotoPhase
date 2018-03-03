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

package com.ruesga.android.wallpapers.photophase.textures;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.effect.EffectContext;
import android.opengl.GLES20;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ruesga.android.wallpapers.photophase.FixedQueue;
import com.ruesga.android.wallpapers.photophase.FixedQueue.EmptyQueueException;
import com.ruesga.android.wallpapers.photophase.GLESSurfaceDispatcher;
import com.ruesga.android.wallpapers.photophase.MediaPictureDiscoverer;
import com.ruesga.android.wallpapers.photophase.MediaPictureDiscoverer.OnMediaPictureDiscoveredListener;
import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.borders.Borders;
import com.ruesga.android.wallpapers.photophase.effects.Effects;
import com.ruesga.android.wallpapers.photophase.model.Disposition;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.utils.BitmapUtils;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLESTextureInfo;
import com.ruesga.android.wallpapers.photophase.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A class that manages the acquisition of new textures.
 */
public class PhotoPhaseTextureManager extends TextureManager
        implements OnMediaPictureDiscoveredListener {

    private static final String TAG = "TextureManager";

    private static final int QUEUE_SIZE = 1;

    private final Context mContext;
    private final Handler mHandler;
    private final Object mEffectsSync = new Object();
    private Effects mEffects;
    private Borders mBorders;
    private final Object mSync;
    private final List<TextureRequestor> mPendingRequests;
    private final FixedQueue<GLESTextureInfo> mQueue = new FixedQueue<>(QUEUE_SIZE);
    private BackgroundPictureLoaderThread mBackgroundTask;
    private final MediaPictureDiscoverer mPictureDiscoverer;

    private Rect mScreenDimensions;
    private Rect mDimensions;

    private final GLESSurfaceDispatcher mDispatcher;

    // The status of the texture manager:
    // 0 - Loading
    // 1 - Loaded
    // 2 - Error
    private byte mStatus;

    private boolean mFirstLoad = true;

    /**
     * A private runnable that will run in the GLThread
     */
    private class PictureDispatcher implements Runnable {
        File mImage;
        GLESTextureInfo ti = null;
        final Object mWait = new Object();

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                // Load the bitmap and create a fake gles information
                ti = GLESUtil.loadFakeTexture(mImage, mDimensions);

                boolean enqueue;
                synchronized (mSync) {
                    enqueue = mPendingRequests.size() == 0;
                }
                synchronized (mSync) {
                    // Notify the new images to all pending frames
                    if (!enqueue) {
                        // Invalid textures are also reported, so requestor can handle it
                        TextureRequestor requestor = mPendingRequests.remove(0);
                        applyToRequestor(requestor, ti);

                    } else {
                        // Add to the queue (only valid textures)
                        if (ti.bitmap != null) {
                            mQueue.insert(ti);
                        }
                    }
                }

            } catch (Throwable e) {
                Log.e(TAG, "Something was wrong loading the texture: " +
                        mImage.getAbsolutePath(), e);

            } finally {
                // Notify that we have a new image
                synchronized (mWait) {
                    mWait.notify();
                }
            }
        }
    }

    /**
     * Constructor of <code>PhotoPhaseTextureManager</code>
     *
     * @param ctx The current context
     * @param effectCtx The current effect context
     * @param dispatcher The GLES dispatcher
     * @param requestors The number of requestors
     * @param screenDimensions The screen dimensions
     */
    public PhotoPhaseTextureManager(final Context ctx, final Handler handler,
            final EffectContext effectCtx, GLESSurfaceDispatcher dispatcher,
            int requestors, Rect screenDimensions) {
        super();
        mContext = ctx;
        mHandler = handler;
        mEffects = new Effects(ctx, effectCtx);
        mBorders = new Borders(ctx, effectCtx);
        mDispatcher = dispatcher;
        mScreenDimensions = screenDimensions;
        mDimensions = screenDimensions; // For now, use the screen dimensions as the preferred dimensions for bitmaps
        mSync = new Object();
        mPendingRequests = new ArrayList<>(requestors);
        mPictureDiscoverer = new MediaPictureDiscoverer(mContext);

        // Run the media discovery thread
        mBackgroundTask = new BackgroundPictureLoaderThread();
        mBackgroundTask.mTaskPaused = false;
        reloadMedia(false);
    }

    /**
     * Method that update the effect context if the EGL context change
     *
     * @param effectCtx The new effect context
     */
    public void updateEffectContext(final EffectContext effectCtx) {
        synchronized (mEffectsSync) {
            if (mEffects != null) {
                mEffects.release();
                mEffects = null;
            }
            mEffects = new Effects(mContext, effectCtx);
            if (mBorders != null) {
                mBorders.release();
                mBorders = null;
            }
            mBorders = new Borders(mContext, effectCtx);
        }
        emptyTextureQueue(true);
    }

    /**
     * Method that allow to change the preferred dimensions of the bitmaps loaded
     *
     * @param dimensions The new dimensions
     */
    public void setDimensions(Rect dimensions) {
        mDimensions = dimensions;
    }

    /**
     * Method that allow to change the screen dimensions
     *
     * @param dimensions The new dimensions
     */
    public void setScreenDimesions(Rect dimensions) {
        mScreenDimensions = dimensions;
    }

    /**
     * Method that returns if the texture manager is paused
     *
     * @return boolean whether the texture manager is paused
     */
    @SuppressWarnings("unused")
    public boolean isPaused() {
        return mBackgroundTask != null && mBackgroundTask.mTaskPaused;
    }

    /**
     * Method that pauses the internal threads
     *
     * @param pause If the thread is paused (true) or resumed (false)
     */
    public synchronized void setPause(boolean pause) {
        synchronized (mBackgroundTask.mLoadSync) {
            mBackgroundTask.mTaskPaused = pause;
            if (!mBackgroundTask.mTaskPaused) {
                mBackgroundTask.mLoadSync.notify();
            }
        }
    }

    /**
     * Method that reload the references of media pictures
     *
     * @param userRequest If the request was generated by the user
     */
    public void reloadMedia(final boolean userRequest) {
        Log.d(TAG, "Reload media picture data");
        // Discovery new media
        // GLThread doesn't run in the UI thread and AsyncThread can't create a
        // valid handler in ICS (it's fixed in JB+) so we force to run the async
        // thread in a valid UI thread
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mPictureDiscoverer.discover(userRequest, PhotoPhaseTextureManager.this);
            }
        });
    }

    @Override
    public void request(TextureRequestor requestor) {
        synchronized (mSync) {
            try {
                GLESTextureInfo ti = mQueue.remove();
                applyToRequestor(requestor, ti);

            } catch (EmptyQueueException eqex) {
                // Add to queue of pending request to be notified when
                // we have a new bitmap in the queue
                mPendingRequests.add(requestor);
            }
        }

        synchronized (mBackgroundTask.mLoadSync) {
            mBackgroundTask.mLoadSync.notify();
        }
    }

    /**
     * Method that removes all the textures from the queue
     *
     * @param reload Forces a reload of the queue
     */
    public void emptyTextureQueue(boolean reload) {
        synchronized (mSync) {
            // Recycle the textures
            try {
                List<GLESTextureInfo> all = mQueue.removeAll();
                for (GLESTextureInfo info : all) {
                    if (GLES20.glIsTexture(info.handle)) {
                        int[] textures = new int[] {info.handle};
                        if (GLESUtil.DEBUG_GL_MEMOBJS) {
                            Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteTextures: ["
                                    + info.handle + "]");
                        }
                        GLES20.glDeleteTextures(1, textures, 0);
                        GLESUtil.glesCheckError("glDeleteTextures");
                    }
                    // Return the bitmap
                    info.bitmap.recycle();
                    info.bitmap = null;
                }
            } catch (EmptyQueueException eqex) {
                // Ignore
            }

            // Remove all pictures in the queue
            try {
                mQueue.removeAll();
            } catch (EmptyQueueException ex) {
                // Ignore
            }

            // Reload the queue
            if (reload) {
                synchronized (mBackgroundTask.mLoadSync) {
                    mBackgroundTask.resetAvailableImages();
                    mBackgroundTask.mLoadSync.notify();
                }
            }
        }
    }

    /**
     * Method that cancels a request did it previously.
     *
     * @param requestor The requestor of the texture
     */
    @SuppressWarnings("unused")
    public void cancelRequest(TextureRequestor requestor) {
        synchronized (mSync) {
            if (mPendingRequests.contains(requestor)) {
                mPendingRequests.remove(requestor);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartMediaDiscovered(boolean userRequest) {
        // No images but thread should start here to received partial data
        this.mStatus = 0; // Loading
        if (mBackgroundTask != null) {
            // In order to continue with the last media shown, we need all the
            // images to process them
            if (!Preferences.Media.isRememberLastMediaShown(mContext) || mFirstLoad) {
                mBackgroundTask.setAvailableImages(new File[]{});
            }
            if (!mBackgroundTask.mRun) {
                mBackgroundTask.start();
            } else {
                synchronized (mBackgroundTask.mLoadSync) {
                    mBackgroundTask.mLoadSync.notify();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPartialMediaDiscovered(File[] images, boolean userRequest) {
        if (mBackgroundTask != null) {
            // In order to continue with the last media shown, we need all the
            // images to process them
            if (!Preferences.Media.isRememberLastMediaShown(mContext)) {
                mBackgroundTask.setPartialAvailableImages(images);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("boxing")
    public void onEndMediaDiscovered(File[] images, boolean userRequest) {
        // Now we have the paths of the images to use. Notify to the thread to
        // load pictures in background
        if (mBackgroundTask != null) {
            mBackgroundTask.setAvailableImages(images);
            if (images != null && images.length > 0) {
                mFirstLoad = false;
            }
            synchronized (mBackgroundTask.mLoadSync) {
                mBackgroundTask.mLoadSync.notify();
            }
            this.mStatus = 1; // Loaded

            // Audit
            int found = images == null ? 0 : images.length;
            Log.d(TAG, "Media picture data reloaded: " + found + " images found.");
            if (userRequest) {
                CharSequence msg =
                        String.format(mContext.getResources().getQuantityText(
                                R.plurals.msg_media_reload_complete, found).toString(), found);
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
            }
        } else {
            this.mStatus = 2; // Error
        }
    }

    /**
     * Method that destroy the references of this class
     */
    public void recycle() {
        // Destroy the media discovery task
        mPictureDiscoverer.recycle();
        synchronized (mEffectsSync) {
            if (mEffects != null) {
                mEffects.release();
            }
            if (mBorders != null) {
                mBorders.release();
            }
        }

        // Destroy the background task
        if (mBackgroundTask != null) {
            mBackgroundTask.mRun = false;
            try {
                synchronized (mBackgroundTask.mLoadSync) {
                    mBackgroundTask.interrupt();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        mBackgroundTask = null;
    }


    /**
     * Returns the status of the texture manager
     *
     * @return byte The status
     */
    public byte getStatus() {
        return mStatus;
    }

    /**
     * Returns if the texture manager is empty
     *
     * @return boolean If the texture manager is empty
     */
    public boolean isEmpty() {
        return mBackgroundTask != null && mBackgroundTask.mEmpty;
    }

    /**
     * Method that load the gles texture and apply to the requestor frame (which includes
     * fix the aspect ratio and/or effects and borders)
     *
     * @param requestor The requestor target
     * @param ti The original texture information (the one with the bitmap one)
     */
    private void applyToRequestor(TextureRequestor requestor, GLESTextureInfo ti) {
        // Transform requestor dimensions to screen dimensions
        RectF dimens = requestor.getRequestorDimensions();
        Rect pixels = new Rect(
                0,
                0,
                (int)(mScreenDimensions.width() * dimens.width() / 2),
                (int)(mScreenDimensions.height() * dimens.height() / 2));

        final Disposition disposition = requestor.getDisposition();
        synchronized (mEffectsSync) {
            if (disposition.hasFlag(Disposition.EFFECT_FLAG)) {
                ti.effect = mEffects.getNextEffect();
            }
            if (disposition.hasFlag(Disposition.BORDER_FLAG)) {
                ti.border = mBorders.getNextBorder();
            }
        }

        // Check if we have to apply any correction to the image
        GLESTextureInfo dst;
        if (ti.bitmap != null && Preferences.General.isFixAspectRatio(mContext)) {

            // Create a texture of power of two here to avoid scaling the bitmap twice
            int w = pixels.width();
            int h = pixels.height();
            if (!BitmapUtils.isPowerOfTwo(w, h) &&
                    PreferencesProvider.Preferences.General.isPowerOfTwo(mContext)) {
                w = h = BitmapUtils.calculateUpperPowerOfTwo(Math.min(w, h));
            }

            // Create a thumbnail of the image
            Bitmap thumb = BitmapUtils.createScaledBitmap(
                    ti.bitmap, w, h, BitmapUtils.ScalingLogic.CROP);
            if (!thumb.equals(ti.bitmap)) {
                ti.bitmap.recycle();
            }
            dst = GLESUtil.loadTexture(mContext, thumb, ti.effect, ti.border, pixels);
        } else {
            // Load the texture without any correction
            dst = GLESUtil.loadTexture(
                    mContext, ti.bitmap, ti.effect, ti.border, pixels);
        }

        // Swap references
        ti.bitmap = dst.bitmap;
        ti.handle = dst.handle;
        ti.effect = null;
        ti.border = null;
        dst.handle = 0;
        dst.bitmap = null;

        // And notify to the requestor
        requestor.setTextureHandle(ti);

        // Clean up memory
        if (ti.bitmap != null) {
            ti.bitmap.recycle();
            ti.bitmap = null;
        }
    }

    /**
     * An internal thread to load pictures in background
     */
    private class BackgroundPictureLoaderThread extends Thread {

        final Object mLoadSync = new Object();
        boolean mRun;
        boolean mTaskPaused;

        boolean mEmpty;
        private final List<File> mNewImages;
        private final List<File> mUsedImages;

        /**
         * Constructor of <code>BackgroundPictureLoaderThread</code>.
         */
        public BackgroundPictureLoaderThread() {
            super();
            mNewImages = new ArrayList<>();
            mUsedImages = new ArrayList<>();
        }

        /**
         * Method that sets the current available images.
         *
         * @param images The current images
         */
        public void setAvailableImages(File[] images) {
            synchronized (mLoadSync) {
                List<File> filtered = new ArrayList<>(Arrays.asList(images));
                mUsedImages.retainAll(filtered);
                filtered.removeAll(mUsedImages);
                mNewImages.clear();
                mNewImages.addAll(filtered);

                if (!mFirstLoad) {
                    reuseLastShownMedia();
                }

                // Retain used images
                int count = mUsedImages.size() - 1;
                for (int i = count; i >= 0; i--) {
                    File image = mUsedImages.get(i);
                    if (!mNewImages.contains(image)) {
                        mUsedImages.remove(image);
                    } else {
                        mNewImages.remove(image);
                    }
                }

                mEmpty = images.length == 0;
            }
        }

        /**
         * Method that adds some available images.
         *
         * @param images The current images
         */
        public void setPartialAvailableImages(File[] images) {
            synchronized (mLoadSync) {
                mNewImages.addAll(Arrays.asList(images));
                mEmpty = images.length == 0;
            }
        }

        /**
         * Method that reset the current available images queue.
         */
        public void resetAvailableImages() {
            synchronized (mLoadSync) {
                mNewImages.addAll(mUsedImages);
                mUsedImages.clear();
            }
        }

        private void reuseLastShownMedia() {
            if (!Preferences.Media.isRandomSequence(mContext) &&
                    Preferences.Media.isRememberLastMediaShown(mContext)) {
                String lastMedia = Preferences.Media.getLastMediaShown(mContext);
                if (!TextUtils.isEmpty(lastMedia)) {
                    File lastMediaFile = new File(lastMedia);
                    mNewImages.addAll(mUsedImages);
                    mUsedImages.clear();
                    Collections.sort(mNewImages);
                    int pos = mNewImages.indexOf(lastMediaFile);
                    // Only if not exists or is not the next in the list
                    if (pos > 0) {
                        // Remove all items to used
                        for (int i = pos - 1; i >= 0; i--) {
                            mUsedImages.add(mNewImages.remove(i));
                        }
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            mRun = true;
            while (mRun) {
                // Check if we need to load more images
                while (!mTaskPaused && PhotoPhaseTextureManager.this.mQueue.items() <
                        PhotoPhaseTextureManager.this.mQueue.size()) {
                    File image;
                    synchronized (mLoadSync) {
                        // Swap arrays if needed
                        if (mNewImages.size() == 0) {
                            mNewImages.addAll(mUsedImages);
                            mUsedImages.clear();
                        }
                        if (mNewImages.size() == 0) {
                            if (!mEmpty) {
                                reloadMedia(false);
                            }
                            break;
                        }

                        // Extract a random or sequential image
                        int low = 0;
                        int high = mNewImages.size() - 1;
                        if (Preferences.Media.isRandomSequence(mContext)) {
                            image = mNewImages.remove(Utils.getNextRandom(low, high));
                        } else {
                            image = mNewImages.remove(0);
                        }
                        Preferences.Media.setLastMediaShown(mContext, image.getPath());

                        // Add to used images
                        mUsedImages.add(image);
                    }

                    // Run commands in the GLThread
                    if (!mRun) break;
                    PictureDispatcher pd = new PictureDispatcher();
                    pd.mImage = image;
                    mDispatcher.dispatch(pd);

                    // Wait until the texture is loaded
                    try {
                        synchronized (pd.mWait) {
                            pd.mWait.wait();
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }

                // Wait for new request
                synchronized (mLoadSync) {
                    try {
                        mLoadSync.wait();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }

    }
}
