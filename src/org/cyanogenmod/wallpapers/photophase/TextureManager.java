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

package org.cyanogenmod.wallpapers.photophase;

import android.content.Context;
import android.graphics.Rect;
import android.media.effect.EffectContext;
import android.opengl.GLES20;
import android.util.Log;
import android.widget.Toast;

import org.cyanogenmod.wallpapers.photophase.FixedQueue.EmptyQueueException;
import org.cyanogenmod.wallpapers.photophase.GLESUtil.GLESTextureInfo;
import org.cyanogenmod.wallpapers.photophase.MediaPictureDiscoverer.OnMediaPictureDiscoveredListener;
import org.cyanogenmod.wallpapers.photophase.effects.Effects;
import org.cyanogenmod.wallpapers.photophase.preferences.PreferencesProvider.Preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class that manages the acquisition of new textures.
 */
public class TextureManager implements OnMediaPictureDiscoveredListener {

    private static final String TAG = "TextureManager";

    private static final int QUEUE_SIZE = 3;

    static final List<GLESTextureInfo> sRecycledBitmaps = new ArrayList<GLESTextureInfo>();

    final Context mContext;
    final EffectContext mEffectContext;
    final Object mSync;
    final List<TextureRequestor> mPendingRequests;
    final FixedQueue<GLESTextureInfo> mQueue = new FixedQueue<GLESTextureInfo>(QUEUE_SIZE);
    BackgroundPictureLoaderThread mBackgroundTask;
    private final MediaPictureDiscoverer mPictureDiscoverer;

    /*package*/ Rect mScreenDimensions;
    /*package*/ Rect mDimensions;

    final GLESSurfaceDispatcher mDispatcher;

    /**
     * A private runnable that will run in the GLThread
     */
    /*package*/ class PictureDispatcher implements Runnable {
        File mImage;
        GLESTextureInfo ti;
        final Object mWait = new Object();

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
                // If we have bitmap to reused then pick up from the recycled list
                if (sRecycledBitmaps.size() > 0) {
                    // Bind to the GLES context
                    GLESTextureInfo oldTextureInfo = sRecycledBitmaps.remove(0);
                    ti = GLESUtil.loadTexture(oldTextureInfo.bitmap,
                            Effects.getNextEffect(mEffectContext), mScreenDimensions);
                    ti.path = oldTextureInfo.path;
                    oldTextureInfo.bitmap = null;
                } else {
                    // Load and bind to the GLES context
                    ti = GLESUtil.loadTexture(mImage, mDimensions,
                            Effects.getNextEffect(mEffectContext), mScreenDimensions, false);
                }

                synchronized (mSync) {
                    // Notify the new images to all pending frames
                    if (mPendingRequests.size() > 0) {
                        // Invalid textures are also reported, so requestor can handle it
                        mPendingRequests.remove(0).setTextureHandle(ti);
                    } else {
                        // Add to the queue (only valid textures)
                        if (ti.handle > 0) {
                            mQueue.insert(ti);
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Something was wrong loading the texture: " + mImage.getAbsolutePath(), e);

            } finally {
                // Notify that we have a new image
                synchronized (mWait) {
                    mWait.notify();
                }
            }
        }
    }

    /**
     * Constructor of <code>TextureManager</code>
     *
     * @param ctx The current context
     * @param effectCtx The current effect context
     * @param dispatcher The GLES dispatcher
     * @param requestors The number of requestors
     * @param screenDimensions The screen dimensions
     */
    public TextureManager(final Context ctx, final EffectContext effectCtx,
                        GLESSurfaceDispatcher dispatcher, int requestors, Rect screenDimensions) {
        super();
        mContext = ctx;
        mEffectContext = effectCtx;
        mDispatcher = dispatcher;
        mScreenDimensions = screenDimensions;
        mDimensions = screenDimensions; // For now, use the screen dimensions as the preferred dimensions for bitmaps
        mSync = new Object();
        mPendingRequests = new ArrayList<TextureRequestor>(requestors);
        mPictureDiscoverer = new MediaPictureDiscoverer(mContext, this);

        // Run the media discovery thread
        mBackgroundTask = new BackgroundPictureLoaderThread();
        mBackgroundTask.mTaskPaused = false;
        reloadMedia(false);
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
    void reloadMedia(boolean userRequest) {
        Log.d(TAG, "Reload media picture data");
        // Discover new media
        mPictureDiscoverer.discover(Preferences.Media.getSelectedAlbums(), userRequest);
    }

    /**
     * Method that returns a bitmap to be reused
     *
     * @param ti The bitmap to release
     */
    @SuppressWarnings("static-method")
    public void releaseBitmap(GLESTextureInfo ti) {
        if (ti != null && ti.bitmap != null) {
            sRecycledBitmaps.add(0, ti);
        }
    }

    /**
     * Method that request a new picture for the {@link TextureRequestor}
     *
     * @param requestor The requestor of the texture
     */
    public void request(TextureRequestor requestor) {
        synchronized (mSync) {
            try {
                requestor.setTextureHandle(mQueue.remove());
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
                        GLES20.glDeleteTextures(1, textures, 0);
                        GLESUtil.glesCheckError("glDeleteTextures");
                    }
                    // Return the bitmap
                    info.bitmap.recycle();
                }
            } catch (EmptyQueueException eqex) {
                // Ignore
            }
            // Recycle the bitmaps
            for (GLESTextureInfo ti : sRecycledBitmaps) {
                ti.bitmap.recycle();
            }
            sRecycledBitmaps.clear();

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
    @SuppressWarnings("boxing")
    public void onMediaDiscovered(MediaPictureDiscoverer mpc, File[] images, boolean userRequest) {
        // Now we have the paths of the images to use. Start a image loader
        // thread to load pictures in background
        mBackgroundTask.setAvailableImages(images);
        if (!mBackgroundTask.mRun) {
            mBackgroundTask.start();
        } else {
            synchronized (mBackgroundTask.mLoadSync) {
                mBackgroundTask.mLoadSync.notify();
            }
        }
        int found = images == null ? 0 : images.length;
        Log.d(TAG, "Media picture data reloaded: " + found + " images found.");
        if (userRequest) {
            CharSequence msg =
                    String.format(mContext.getResources().getQuantityText(
                            R.plurals.msg_media_reload_complete, found).toString(), found);
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Method that destroy the references of this class
     */
    public void recycle() {
        // Destroy the media discovery task
        mPictureDiscoverer.recycle();

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
     * An internal thread to load pictures in background
     */
    private class BackgroundPictureLoaderThread extends Thread {

        final Object mLoadSync = new Object();
        boolean mRun;
        boolean mTaskPaused;

        private boolean mEmpty;
        private final List<File> mNewImages;
        private final List<File> mUsedImages;

        /**
         * Constructor of <code>BackgroundPictureLoaderThread</code>.
         */
        public BackgroundPictureLoaderThread() {
            super();
            mNewImages = new ArrayList<File>();
            mUsedImages = new ArrayList<File>();
        }

        /**
         * Method that sets the current available images.
         *
         * @param images The current images
         */
        public void setAvailableImages(File[] images) {
            synchronized (mLoadSync) {
                mNewImages.clear();
                mNewImages.addAll(Arrays.asList(images));
                mUsedImages.clear();
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

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            mRun = true;
            while (mRun) {
                // Check if we need to load more images
                while (!mTaskPaused && TextureManager.this.mQueue.items() < TextureManager.this.mQueue.size()) {
                    File image = null;
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

                        // Extract a random image
                        int low = 0;
                        int hight = mNewImages.size()-1;
                        int index = low + (int)(Math.random() * ((hight - low) + 1));
                        image = mNewImages.remove(index);
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

                    // Add to used images
                    mUsedImages.add(image);
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
