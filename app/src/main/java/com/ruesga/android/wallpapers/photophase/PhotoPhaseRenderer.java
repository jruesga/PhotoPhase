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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.effect.EffectContext;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.cast.CastService;
import com.ruesga.android.wallpapers.photophase.cast.CastServiceConstants;
import com.ruesga.android.wallpapers.photophase.cast.CastUtils;
import com.ruesga.android.wallpapers.photophase.model.Disposition;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.preferences.TouchAction;
import com.ruesga.android.wallpapers.photophase.shapes.ColorShape;
import com.ruesga.android.wallpapers.photophase.shapes.OopsShape;
import com.ruesga.android.wallpapers.photophase.textures.PhotoPhaseTextureManager;
import com.ruesga.android.wallpapers.photophase.transitions.Transition;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLColor;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLESTextureInfo;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.ruesga.android.wallpapers.photophase.providers.TemporaryContentAccessProvider.createAuthorizationUri;

/**
 * The EGL renderer of PhotoPhase Live Wallpaper.
 */
public class PhotoPhaseRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "PhotoPhaseRenderer";

    private static final boolean DEBUG = false;

    private final long mInstance;
    private static long sInstances;

    private final boolean mIsPreview;
    private boolean mIsPaused;
    private boolean mRecreateWorld;
    private boolean mIsDestroyed;

    private final Context mContext;
    private EffectContext mEffectContext;
    private final Handler mHandler;
    private final GLESSurfaceDispatcher mDispatcher;
    private PhotoPhaseTextureManager mTextureManager;

    private final AlarmManager mAlarmManager;
    private PendingIntent mRecreateDispositionPendingIntent;

    private PhotoPhaseWallpaperWorld mWorld;
    private ColorShape mOverlay;
    private OopsShape mOopsShape;

    private boolean mManualTransition;
    private long mLastRunningTransition;
    private long mLastTransition;

    private long mLastTouchTime;
    private static final long TOUCH_BARRIER_TIME = 1000L;

    private int mWidth = -1;
    private int mHeight = -1;
    private int mStatusBarHeight = 0;
    private int mMeasuredHeight  = -1;
    private boolean mUseWallpaperOffset;
    private float mOffsetX = -1f;

    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
    private float mMVPMatrixOffset;

    private final Object mDrawing = new Object();
    private boolean mRecycle;

    private final Object mMediaSync = new Object();
    private PendingIntent mMediaScanIntent;

    private ICastService mCastService;
    private boolean mCastConnecting;

    private final BroadcastReceiver mSettingsChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (PreferencesProvider.ACTION_SETTINGS_CHANGED.equals(action)) {
                // Check what flags are been requested
                boolean recreateWorld = intent.getBooleanExtra(
                        PreferencesProvider.EXTRA_FLAG_RECREATE_WORLD, false);
                boolean redraw = intent.getBooleanExtra(PreferencesProvider.EXTRA_FLAG_REDRAW, false);
                boolean emptyTextureQueue = intent.getBooleanExtra(
                        PreferencesProvider.EXTRA_FLAG_EMPTY_TEXTURE_QUEUE, false);
                boolean mediaReload = intent.getBooleanExtra(
                        PreferencesProvider.EXTRA_FLAG_MEDIA_RELOAD, false);
                boolean mediaIntervalChanged = intent.getBooleanExtra(
                        PreferencesProvider.EXTRA_FLAG_MEDIA_INTERVAL_CHANGED, false);
                int dispositionInterval = intent.getIntExtra(
                        PreferencesProvider.EXTRA_FLAG_DISPOSITION_INTERVAL_CHANGED, -1);

                // Update wallpaper offset
                mUseWallpaperOffset = PreferencesProvider.Preferences.General
                        .isWallpaperOffset(context);

                // Empty texture queue?
                if (emptyTextureQueue) {
                    if (mTextureManager != null) {
                        mTextureManager.emptyTextureQueue(true);
                    }
                }

                // Media reload. Purging resources and performs a media query
                if (mediaReload) {
                    synchronized (mMediaSync) {
                        if (mTextureManager != null) {
                            boolean userReloadRequest = intent.getBooleanExtra(
                                    PreferencesProvider.EXTRA_ACTION_MEDIA_USER_RELOAD_REQUEST, false);
                            mTextureManager.reloadMedia(userReloadRequest);
                            scheduleOrCancelMediaScan();
                        }
                    }
                }

                // Media scan interval was changed. Reschedule
                if (mediaIntervalChanged) {
                    scheduleOrCancelMediaScan();
                }

                // Media scan interval was changed. Reschedule
                if (dispositionInterval != -1) {
                    scheduleDispositionRecreation();
                }

                // Recreate the whole world?
                if (recreateWorld && mWorld != null) {
                    recreateWorld();
                }

                // Performs a redraw?
                if (redraw) {
                    forceRedraw();
                }

                // Preference could be changed, should disconnect the cast service?
                handleCastStatusChanged();
            } else if (CastServiceConstants.ACTION_CONNECTIVITY_CHANGED.equals(action)) {
                // Have a valid cast connectivity?
                handleCastStatusChanged();
            }
        }
    };

    private final Runnable mTransitionThread = new Runnable() {
        @Override
        public void run() {
            // Run in GLES's thread
            mDispatcher.dispatch(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!mIsPaused) {
                            // Select a new transition
                            mWorld.selectRandomTransition();
                            mLastRunningTransition = System.currentTimeMillis();
                            mLastTransition = System.currentTimeMillis();

                            // Now force continuously render while transition is applied
                            mDispatcher.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                        }
                    } catch (Throwable ex) {
                        Log.e(TAG, "Something was wrong selecting the transition", ex);
                    }
                }
            });
        }
    };

    private final Runnable mEGLContextWatchDog = new Runnable() {
        @Override
        public void run() {
            // Restart the wallpaper
            AndroidHelper.restartWallpaper();
        }
    };

    private final ServiceConnection mCastConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mCastService = ICastService.Stub.asInterface(binder);
            mCastConnecting = false;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mCastService = null;
            boolean enabled = PreferencesProvider.Preferences.Cast.isEnabled(mContext);
            boolean validNetwork = CastUtils.hasValidCastNetwork(mContext);
            if (enabled && validNetwork && !mIsDestroyed) {
                // Reconnect
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bindToCastService();
                    }
                }, 5000L);
            }
        }
    };

    /**
     * Constructor of <code>PhotoPhaseRenderer<code>
     *
     * @param ctx The current context
     * @param dispatcher The GLES dispatcher
     * @param isPreview Indicates if the renderer is in preview mode
     */
    public PhotoPhaseRenderer(Context ctx, GLESSurfaceDispatcher dispatcher, boolean isPreview) {
        super();
        mContext = ctx;
        mHandler = new Handler();
        mDispatcher = dispatcher;
        mInstance = sInstances;
        mIsPreview = isPreview;
        mIsPaused = true;
        mRecreateWorld = false;
        sInstances++;
        mAlarmManager = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        mUseWallpaperOffset = PreferencesProvider.Preferences.General.isWallpaperOffset(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (mInstance ^ (mInstance >>> 32));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PhotoPhaseRenderer other = (PhotoPhaseRenderer) obj;
        return mInstance == other.mInstance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "PhotoPhaseRenderer [instance: " + mInstance + "]";
    }

    /**
     * Method called when renderer is created
     */
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "onCreate [" + mInstance + "]");
        // Register a receiver to listen for media reload request
        IntentFilter filter = new IntentFilter();
        filter.addAction(CastServiceConstants.ACTION_CONNECTIVITY_CHANGED);
        mContext.registerReceiver(mSettingsChangedReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(PreferencesProvider.ACTION_SETTINGS_CHANGED);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mSettingsChangedReceiver, filter);

        // Check whether the media scan is active
        int interval = Preferences.Media.getRefreshFrequency(mContext);
        if (interval != Preferences.Media.MEDIA_RELOAD_DISABLED) {
            // Schedule a media scan
            scheduleMediaScan(interval);
        }

        // Link to cast service, only if we need to do
        boolean castEnabled = PreferencesProvider.Preferences.Cast.isEnabled(mContext);
        boolean validNetwork = CastUtils.hasValidCastNetwork(mContext);
        if (castEnabled && validNetwork) {
            bindToCastService();
        }
    }

    /**
     * Method called when renderer is destroyed
     */
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy [" + mInstance + "]");
        mIsDestroyed = true;

        // Register a receiver to listen for media reload request
        unbindFromCastService();
        mContext.unregisterReceiver(mSettingsChangedReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mSettingsChangedReceiver);
        recycle();
        if (mEffectContext != null) {
            mEffectContext.release();
        }
        mEffectContext = null;
        mWidth = -1;
        mHeight = -1;
        mMeasuredHeight = -1;
    }

    /**
     * Method called when system runs under low memory
     */
    public void onLowMemory() {
        if (mTextureManager != null) {
            mTextureManager.emptyTextureQueue(false);
        }
    }

    /**
     * Method called when the renderer should be paused
     */
    public void onPause() {
        if (DEBUG) Log.d(TAG, "onPause [" + mInstance + "]");
        mIsPaused = true;
        mHandler.removeCallbacks(mTransitionThread);
        if (mTextureManager != null) {
            mTextureManager.setPause(true);
        }
    }

    /**
     * Method called when the renderer should be resumed
     */
    public void onResume() {
        if (DEBUG) Log.d(TAG, "onResume [" + mInstance + "]");
        if (mTextureManager != null) {
            mTextureManager.setPause(false);
        }
        mIsPaused = false;
        if (mRecreateWorld) {
            recreateWorld();
        } else {
            mDispatcher.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }

        // Set a watchdog to detect EGL bad context and restart the wallpaper
        if (!mIsPreview) {
            mHandler.postDelayed(mEGLContextWatchDog, 15000L);
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void onOffsetChanged(float x , float y) {
        mOffsetX = x;
        mDispatcher.requestRender();
    }

    /**
     * Method called when the renderer should process a touch event over the screen
     *
     * @param x The x coordinate
     * @param y The y coordinate
     */
    public void onTouch(float x , float y, boolean ignoreBarrier) {
        if (mWorld != null) {
            // Do user action
            TouchAction touchAction = Preferences.General.Touch.getTouchAction(mContext);
            if (touchAction.compareTo(TouchAction.NONE) != 0) {
                // Avoid to handle multiple touchs
                long touchTime = System.currentTimeMillis();
                long diff = touchTime - mLastTouchTime;
                mLastTouchTime = touchTime;
                if (!ignoreBarrier && diff < TOUCH_BARRIER_TIME) {
                    return;
                }

                // Retrieve the photo frame for its coordinates
                final PhotoFrame frame = mWorld.getFrameFromCoordinates(new PointF(x, y));
                if (frame == null) {
                    Log.w(TAG, "No frame from coordenates");
                    return;
                }
                if (!frame.getDisposition().hasFlag(Disposition.BACKGROUND_FLAG)) {
                    // Ignore touch
                    return;
                }

                // Apply the action
                if (touchAction.compareTo(TouchAction.TRANSITION) == 0) {
                    if (!frame.getDisposition().hasFlag(Disposition.TRANSITION_FLAG)) {
                        // Ignore touch
                        return;
                    }

                    try {
                        // Select the frame with a transition
                        // Run in GLES's thread
                        mDispatcher.dispatch(new Runnable() {
                            @Override
                            public void run() {
                                // Select a new transition
                                deselectCurrentTransition();
                                mWorld.selectTransition(frame);
                                mLastRunningTransition = System.currentTimeMillis();
                                mManualTransition = true;

                                // Now force continuously render while transition is applied
                                mDispatcher.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                            }
                        });

                    } catch (NotFoundException ex) {
                        Log.e(TAG, "The frame not exists " + frame.getTextureInfo().path, ex);
                    }

                } else if (touchAction.compareTo(TouchAction.OPEN) == 0) {
                    // Open the image
                    if (PreferencesProvider.Preferences.General.Touch.getTouchOpenWith(mContext)) {
                        // Internal
                        File file = getFileFromFrame(frame);
                        if (file != null) {
                            Intent intent = new Intent(mContext, PhotoViewerActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra(PhotoViewerActivity.EXTRA_PHOTO, file.getAbsolutePath());
                            mContext.startActivity(intent);
                        }
                    } else {
                        // External
                        Uri uri = getUriFromFrame(frame);
                        if (uri != null) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                Uri temporaryUri = createAuthorizationUri(uri);
                                intent.setDataAndType(temporaryUri, "image/*");
                                mContext.startActivity(intent);
                            } catch (ActivityNotFoundException ex) {
                                Log.e(TAG, "Open action not found for " + frame.getTextureInfo().path, ex);
                            }
                        }
                    }

                } else if (touchAction.compareTo(TouchAction.SHARE) == 0) {
                    Uri uri = getUriFromFrame(frame);
                    if (uri != null) {
                        AndroidHelper.sharePicture(mContext, uri);
                    }

                } else if (touchAction.compareTo(TouchAction.CAST) == 0) {
                    // Send the current photo of the target frame to a cast device
                    File file = getFileFromFrame(frame);
                    if (file != null && mCastService != null) {
                        try {
                            mCastService.cast(file.toString());
                        } catch (RemoteException e) {
                            Log.w(TAG, "Got a remote exception while casting " + file, e);
                        }
                    }
                } else if (touchAction.compareTo(TouchAction.SHOW_DETAILS) == 0) {
                    File file = getFileFromFrame(frame);
                    if (file != null) {
                        Intent intent = new Intent(mContext, PhotoViewerActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra(PhotoViewerActivity.EXTRA_PHOTO, file.getAbsolutePath());
                        intent.putExtra(PhotoViewerActivity.EXTRA_SHOW_DETAILS_ONLY, true);
                        mContext.startActivity(intent);
                    }
                }
            }
        }
    }

    /**
     * Method that returns an Uri reference from a photo frame
     *
     * @param frame The photo frame
     * @return Uri The image uri
     */
    private static Uri getUriFromFrame(final PhotoFrame frame) {
        File file = getFileFromFrame(frame);
        if (file == null) {
            return null;
        }
        return Uri.fromFile(file);
    }

    private static File getFileFromFrame(final PhotoFrame frame) {
        // Sanity checks
        GLESTextureInfo info = frame.getTextureInfo();
        if (info == null) {
            Log.e(TAG, "The frame has not a valid reference right now." +
                    "Touch action is not available.");
            return null;
        }
        if (info.path == null || !info.path.isFile()) {
            Log.e(TAG, "The image do not exists. Touch action is not available.");
            return null;
        }

        // Return the uri from the path
        return frame.getTextureInfo().path;
    }

    private void bindToCastService() {
        // Bind to cast service
        if (!mIsPreview && mCastService == null && !mCastConnecting) {
            mCastConnecting = true;
            try {
                Intent i = new Intent(mContext, CastService.class);
                boolean ret = mContext.bindService(i, mCastConnection, Context.BIND_AUTO_CREATE);
                if (!ret) {
                    mCastConnecting = false;
                }
            } catch (SecurityException se) {
                Log.w(TAG, "Can't bound to CastService", se);
                mCastConnecting = false;
            }
        }
    }

    private void unbindFromCastService() {
        if (mCastService != null) {
            mCastService = null;
            mContext.unbindService(mCastConnection);
        }
    }

    /**
     * Method that deselect the current transition
     */
    private synchronized void deselectCurrentTransition() {
        mHandler.removeCallbacks(mTransitionThread);
        mWorld.deselectTransition(mMVPMatrix, mMVPMatrixOffset);
        mLastRunningTransition = 0;
    }

    private void scheduleOrCancelMediaScan() {
        // Ignored in preview mode
        if (mIsPreview) {
            return;
        }

        int interval = Preferences.Media.getRefreshFrequency(mContext);
        if (interval != Preferences.Media.MEDIA_RELOAD_DISABLED) {
            scheduleMediaScan(interval);
        } else {
            cancelMediaScan();
        }
    }

    /**
     * Method that schedules a new media scan
     *
     * @param interval The new interval
     */
    private void scheduleMediaScan(int interval) {
        // Ignored in preview mode
        if (mIsPreview) {
            return;
        }

        Intent i = new Intent(PreferencesProvider.ACTION_SETTINGS_CHANGED);
        i.putExtra(PreferencesProvider.EXTRA_FLAG_MEDIA_RELOAD, Boolean.TRUE);
        mMediaScanIntent = PendingIntent.getBroadcast(
                mContext, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

        long milliseconds = interval * 1000L;
        long nextTime = System.currentTimeMillis() + milliseconds;
        mAlarmManager.set(AlarmManager.RTC, nextTime, mMediaScanIntent);
    }

    /**
     * Method that cancels a pending media scan
     */
    private void cancelMediaScan() {
        if (mMediaScanIntent != null) {
            mAlarmManager.cancel(mMediaScanIntent);
            mMediaScanIntent = null;
        }
    }

    /**
     * Method that schedule a new recreation of the current disposition
     */
    private void scheduleDispositionRecreation() {
        // Ignored in preview mode
        if (mIsPreview) {
            return;
        }

        // Cancel current alarm
        cancelDispositionRecreation();

        // Is random disposition enabled?
        if (!Preferences.Layout.isRandomDispositions(mContext)) {
            return;
        }

        // Schedule the next recreation if interval has been configured
        int interval = Preferences.Layout.getRandomDispositionsInterval(mContext);
        if (interval > 0) {
            // Created the intent
            Intent intent = new Intent(PreferencesProvider.ACTION_SETTINGS_CHANGED);
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_RECREATE_WORLD, Boolean.TRUE);
            mRecreateDispositionPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

            // Schedule the pending intent
            long nextTime = System.currentTimeMillis() + interval;
            mAlarmManager.set(AlarmManager.RTC, nextTime, mRecreateDispositionPendingIntent);
        }
    }

    /**
     * Method that cancels a pending media scan
     */
    private void cancelDispositionRecreation() {
        // Cancel current alarm
        if (mRecreateDispositionPendingIntent != null) {
            mAlarmManager.cancel(mRecreateDispositionPendingIntent);
        }
    }

    /**
     * Recreate the world
     */
    private void recreateWorld() {
        if (mIsPaused) {
            mRecreateWorld = true;
            return;
        }
        mRecreateWorld = false;

        // Recreate the wallpaper world (under a GLES context)
        mDispatcher.dispatch(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mDrawing) {
                        mLastRunningTransition = 0;
                        mWorld.recreateWorld(mWidth, mMeasuredHeight);
                    }
                } catch (GLException e) {
                    Log.e(TAG, "Cannot recreate the wallpaper world.", e);
                } finally {
                    mDispatcher.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                }
                scheduleDispositionRecreation();
            }
        });
    }

    /**
     * Force a redraw of the screen
     */
    private void forceRedraw() {
        mDispatcher.requestRender();
    }

    /**
     * Method that destroy all the internal references
     */
    private void recycle() {
        if (DEBUG) Log.d(TAG, "recycle [" + mInstance + "]");
        // Remove any pending handle
        if (mHandler != null && mTransitionThread != null) {
            mHandler.removeCallbacks(mTransitionThread);
        }

        // Delete the world
        synchronized (mDrawing) {
            mRecycle = true;
            if (mWorld != null) mWorld.recycle();
            if (mTextureManager != null) mTextureManager.recycle();
            if (mOverlay != null) mOverlay.recycle();
            if (mOopsShape != null) mOopsShape.recycle();
            mWorld = null;
            mTextureManager = null;
            mOverlay = null;
            mOopsShape = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        if (DEBUG) Log.d(TAG, "onSurfaceCreated [" + mInstance + "]");

        mWidth = -1;
        mHeight = -1;
        mMeasuredHeight = -1;
        mStatusBarHeight = 0;

        mLastTransition = System.currentTimeMillis();

        // We have a 2d (fake) scenario, disable all unnecessary tests. Deep are
        // necessary for some 3d effects
        GLES20.glDisable(GL10.GL_DITHER);
        GLESUtil.glesCheckError("glDisable");
        GLES20.glDisable(GL10.GL_CULL_FACE);
        GLESUtil.glesCheckError("glDisable");
        GLES20.glEnable(GL10.GL_DEPTH_TEST);
        GLESUtil.glesCheckError("glEnable");
        GLES20.glDepthMask(false);
        GLESUtil.glesCheckError("glDepthMask");
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLESUtil.glesCheckError("glDepthFunc");

        // Create an effect context
        if (mEffectContext != null) {
            mEffectContext.release();
        }
        mEffectContext = EffectContext.createWithCurrentGlContext();

        // Create the texture manager and recycle the old one
        if (mTextureManager == null) {
            // Precalculate the window size for the PhotoPhaseTextureManager. In onSurfaceChanged
            // the best fixed size will be set. The disposition size is simple for a better
            // performance of the internal arrays
            final Configuration conf = mContext.getResources().getConfiguration();
            int orientation = mContext.getResources().getConfiguration().orientation;
            int w = (int) AndroidHelper.convertDpToPixel(mContext, conf.screenWidthDp);
            int h = (int) AndroidHelper.convertDpToPixel(mContext, conf.screenHeightDp);
            Rect dimensions = new Rect(0, 0, w, h);
            int cc = (orientation == Configuration.ORIENTATION_PORTRAIT)
                        ? Preferences.Layout.getPortraitDisposition(mContext).size()
                        : Preferences.Layout.getLandscapeDisposition(mContext).size();

            // Recycle the current texture manager and create a new one
            recycle();
            mTextureManager = new PhotoPhaseTextureManager(
                    mContext, mHandler, mEffectContext, mDispatcher, cc, dimensions);
        } else {
            mTextureManager.updateEffectContext(mEffectContext);
        }

        // Schedule dispositions random recreation (if need it)
        scheduleDispositionRecreation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        if (DEBUG) Log.d(TAG, "onSurfaceChanged [" + mInstance + "," + width + "x" + height + "]");

        // Check if the size was changed
        if (mWidth == width && mHeight == height) {
            return;
        }

        // Save the width and height to avoid recreate the world
        mWidth = width;
        mHeight = height;
        mStatusBarHeight = AndroidHelper.calculateStatusBarHeight(mContext);
        mMeasuredHeight = mHeight + mStatusBarHeight;

        // Calculate a better fixed size for the pictures
        Rect dimensions = new Rect(0, 0, width, height);
        Rect screenDimensions = new Rect(0, AndroidHelper.isKitKatOrGreater() ? 0 : mStatusBarHeight,
                width, AndroidHelper.isKitKatOrGreater() ? height + mStatusBarHeight : height);
        mTextureManager.setDimensions(dimensions);
        mTextureManager.setScreenDimesions(screenDimensions);
        mTextureManager.setPause(false);

        // Create the wallpaper (destroy the previous)
        if (mWorld != null) {
            mWorld.recycle();
        }
        mWorld = new PhotoPhaseWallpaperWorld(mContext, mTextureManager);

        // Create the overlay shape
        final float[] vertex = {
                                -1.0f, -1.0f,
                                 1.0f, -1.0f,
                                -1.0f,  1.0f,
                                 1.0f,  1.0f
                               };
        mOverlay = new ColorShape(mContext, vertex, Colors.getInstance(mContext).getOverlay());

        // Create the Oops shape
        mOopsShape = new OopsShape(mContext);

        // Set the viewport and the fustrum
        GLES20.glViewport(0, AndroidHelper.isKitKatOrGreater() ? 0 : -mStatusBarHeight, mWidth,
                AndroidHelper.isKitKatOrGreater() ? mHeight + mStatusBarHeight : mHeight);
        GLESUtil.glesCheckError("glViewport");
        Matrix.frustumM(mProjMatrix, 0, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 2.0f);

        // Recreate the wallpaper world
        try {
            mWorld.recreateWorld(width, mMeasuredHeight);
        } catch (GLException e) {
            Log.e(TAG, "Cannot recreate the wallpaper world.", e);
        }

        // Force an immediate redraw of the screen (draw thread could be in dirty mode only)
        deselectCurrentTransition();
        mRecycle = false;
        mDispatcher.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDrawFrame(GL10 glUnused) {
        // Check whether we have a valid surface
        if (!mDispatcher.hasValidSurface()) {
            return;
        }

        if (mRecycle) {
            return;
        }

        // Remove the EGL context watchdog
        if (!mIsPreview) {
            mHandler.removeCallbacks(mEGLContextWatchDog);
        }

        // Calculate wallpaper offset
        int widthOffset = 0;
        if (!mIsPreview && mUseWallpaperOffset && mOffsetX != -1) {
            widthOffset = (int) (mWidth / 3f);
        }
        mMVPMatrixOffset = (!mIsPreview && mUseWallpaperOffset && mOffsetX != -1)
                ? -0.5f * mOffsetX : 0.0f;

        // Set the projection, view and model
        GLES20.glViewport(0, -mStatusBarHeight, mWidth + widthOffset, mHeight);
        Matrix.setLookAtM(mVMatrix, 0, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        if (mMVPMatrixOffset != 0.0f) {
            Matrix.translateM(mVMatrix, 0, mMVPMatrixOffset, 0.0f, 0.0f);
        }
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

        if (mTextureManager != null) {
            if (mTextureManager.getStatus() == 1 && mTextureManager.isEmpty()) {
                // Advise the user and stop
                drawOops();
                mDispatcher.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

            } else {
                // Draw the background
                drawBackground();

                if (!mIsPaused && mWorld != null) {
                    // Now draw the world (all the photo frames with effects)
                    mWorld.draw(mMVPMatrix, mMVPMatrixOffset);

                    // Check if we have some pending transition or transition has
                    // exceed its timeout
                    synchronized (mDrawing) {
                        final int interval = Preferences.General.Transitions.getTransitionInterval(mContext);
                        if (mManualTransition || interval > 0) {
                            if (!mWorld.hasRunningTransition() || isTransitionTimeoutFired()) {
                                mDispatcher.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                                mManualTransition = false;

                                // Now start a delayed thread to generate the next effect
                                deselectCurrentTransition();
                                long diff = System.currentTimeMillis() - mLastTransition;
                                long delay = Math.max(200, interval - diff);
                                mHandler.postDelayed(mTransitionThread, delay);
                            }
                        } else {
                            // Just display the initial frames and never make transitions
                            if (!mWorld.hasRunningTransition() || isTransitionTimeoutFired()) {
                                mDispatcher.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                            }
                        }
                    }
                } else {
                    if (mWorld != null) {
                        // Just draw the world before notify GLView to goto sleep
                        mWorld.draw(mMVPMatrix, mMVPMatrixOffset);
                    }
                    mDispatcher.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                }

                // Draw the overlay
                drawOverlay();
            }
        }
    }

    /**
     * Check whether the transition has exceed the timeout
     *
     * @return boolean if the transition has exceed the timeout
     */
    private boolean isTransitionTimeoutFired() {
        long now = System.currentTimeMillis();
        long diff = now - mLastRunningTransition;
        return mLastRunningTransition != 0 && diff > Transition.MAX_TRANSTION_TIME;
    }

    /**
     * Method that draws the background of the wallpaper
     */
    private void drawBackground() {
        GLColor bg = Colors.getInstance(mContext).getBackground();
        GLES20.glClearColor(bg.r, bg.g, bg.b, bg.a);
        GLESUtil.glesCheckError("glClearColor");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLESUtil.glesCheckError("glClear");
    }

    /**
     * Method that draws the overlay of the wallpaper
     */
    private void drawOverlay() {
        if (mOverlay != null) {
            mOverlay.setAlpha(Preferences.General.getWallpaperDim(mContext) / 100.0f);
            mOverlay.draw(mMVPMatrix);
        }
    }

    /**
     * Method that draws the oops message
     */
    private void drawOops() {
        if (mOopsShape != null) {
            mOopsShape.draw(mMVPMatrix);
        }
    }

    private void handleCastStatusChanged() {
        boolean hasConnectivity = CastUtils.hasValidCastNetwork(mContext);
        boolean castEnabled = PreferencesProvider.Preferences.Cast.isEnabled(mContext);
        if (castEnabled && hasConnectivity) {
            bindToCastService();
        } else if (castEnabled) {
            unbindFromCastService();
        }
    }
}
