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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewConfiguration;

import com.ruesga.android.wallpapers.photophase.GLESWallpaperService.GLESEngineListener;
import com.ruesga.android.wallpapers.photophase.preferences.ChoosePicturesFragment;
import com.ruesga.android.wallpapers.photophase.preferences.PhotoPhasePreferences;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider;

import java.util.ArrayList;
import java.util.List;


/**
 * The PhotoPhase Live Wallpaper service.
 */
public class PhotoPhaseWallpaper
    extends GLES20WallpaperService implements GLESEngineListener {

    private static final String TAG = "PhotoPhaseWallpaper";

    private static final boolean DEBUG = false;

    private List<Renderer> mRenderers;
    private PhotoPhaseWallpaperEngine mEngine;

    private boolean mPreserveEGLContext;

    // List of the current top activities. Tap should be ignored when this acitivities are
    // in the foreground
    private static final String[] TOP_ACTIVITIES = {"com.android.internal.app.ChooserActivity"};

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "onCreate");
        super.onCreate();

        // Load the configuration
        mPreserveEGLContext = getResources().getBoolean(R.bool.config_preserve_egl_context);
        mRenderers = new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        super.onDestroy();
        for (Renderer renderer : mRenderers) {
            ((PhotoPhaseRenderer) renderer).onDestroy();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Engine onCreateEngine() {
        mEngine = new PhotoPhaseWallpaperEngine(this);
        return mEngine;
    }

    /**
     * A wallpaper engine implementation using GLES.
     */
    class PhotoPhaseWallpaperEngine extends GLES20WallpaperService.GLES20Engine {

        private final Handler mHandler;
        final ActivityManager mActivityManager;

        private long mLastTouch = -1;
        private final Point mLastPoint = new Point();

        private static final int DOUBLE_TAP_MIN_TIME = 40;
        private final int mDoubleTapSlop;

        /**
         * Constructor of <code>PhotoPhaseWallpaperEngine<code>
         *
         * @param wallpaper The wallpaper service reference
         */
        PhotoPhaseWallpaperEngine(PhotoPhaseWallpaper wallpaper) {
            super();
            mHandler = new Handler();
            mActivityManager = (ActivityManager)getApplication().getSystemService(ACTIVITY_SERVICE);
            setOffsetNotificationsEnabled(false);
            setTouchEventsEnabled(false);
            setGLESEngineListener(wallpaper);
            setWallpaperGLSurfaceView(new PhotoPhaseWallpaperGLSurfaceView(wallpaper));
            setPauseOnPreview(true);

            mDoubleTapSlop = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        }

        /**
         * Out custom GLSurfaceView class to let us access all events stuff.
         */
        class PhotoPhaseWallpaperGLSurfaceView extends WallpaperGLSurfaceView {
            /**
             * The constructor of <code>PhotoPhaseWallpaperGLSurfaceView</code>.
             *
             * @param context The current context
             */
            public PhotoPhaseWallpaperGLSurfaceView(Context context) {
                super(context);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Bundle onCommand(final String action, final int x, final int y, final int z,
                final Bundle extras, final boolean resultRequested) {
            // Ignore commands in preview mode
            final Context ctx = PhotoPhaseWallpaper.this;
            if (action.compareTo(WallpaperManager.COMMAND_TAP) == 0) {
                if (!AndroidHelper.hasReadExternalStoragePermissionGranted(getApplicationContext())) {
                    // Open the album settings
                    Intent i = new Intent(getApplicationContext(), PhotoPhasePreferences.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                            ChoosePicturesFragment.class.getName());
                    i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                    startActivity(i);
                    return super.onCommand(action, x, y, z, extras, resultRequested);
                }

                if (isPreview()) {
                    return super.onCommand(action, x, y, z, extras, resultRequested);
                }

                if (isDoubleTap(ctx, x, y)) {
                    // Pass the x and y position to the renderer
                    ((PhotoPhaseRenderer)getRenderer()).onTouch(x, y, true);
                } else if (!PreferencesProvider.Preferences.General.Touch.getTouchMode(ctx)) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        @SuppressWarnings("deprecation")
                        public void run() {
                            // Only if the wallpaper is visible after a long press and
                            // not in preview mode
                            if (isVisible() && !isPreview()) {
                                // This is still valid, because we need the HOME task which is still
                                // part of the list after LOLLIPOP, and valid prior to this api
                                List<RunningTaskInfo> taskInfo = mActivityManager.getRunningTasks(1);
                                if (taskInfo.size() > 0 && taskInfo.get(0).topActivity != null) {
                                    String topActivity = taskInfo.get(0).topActivity.getClassName();
                                    for (String activity : TOP_ACTIVITIES) {
                                        if (activity.compareTo(topActivity) == 0) {
                                            // Ignore tap event
                                            return;
                                        }
                                    }
                                }

                                // Pass the x and y position to the renderer
                                ((PhotoPhaseRenderer) getRenderer()).onTouch(x, y, false);
                            }
                        }
                    }, ViewConfiguration.getLongPressTimeout() + 100L);
                }

                mLastTouch = System.currentTimeMillis();
                mLastPoint.set(x, y);
            }
            return super.onCommand(action, x, y, z, extras, resultRequested);
        }

        private boolean isDoubleTap(Context ctx, final int x, final int y) {
            if (PreferencesProvider.Preferences.General.Touch.getTouchMode(ctx)) {
                // User preference is double tap
                long diff = System.currentTimeMillis() - mLastTouch;
                if (diff > 0 && diff >= DOUBLE_TAP_MIN_TIME
                        && diff <= ViewConfiguration.getDoubleTapTimeout()) {
                    if (Math.abs(mLastPoint.x - x) < mDoubleTapSlop
                            && Math.abs(mLastPoint.y - y) < mDoubleTapSlop) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(TAG, "onLowMemory");
        for (Renderer renderer : mRenderers) {
            // Pause the wallpaper and destroy the cached textures
            ((PhotoPhaseRenderer) renderer).onPause();
            ((PhotoPhaseRenderer) renderer).onLowMemory();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInitializeEGLView(GLSurfaceView view) {
        if (DEBUG) Log.d(TAG, "onInitializeEGLView");
        view.getHolder().setFormat(PixelFormat.RGBA_8888);
        view.setPreserveEGLContextOnPause(mPreserveEGLContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroyEGLView(GLSurfaceView view, Renderer renderer) {
        if (DEBUG) Log.d(TAG, "onDestroyEGLView" + renderer);
        mRenderers.remove(renderer);
        ((PhotoPhaseRenderer)renderer).onPause();
        ((PhotoPhaseRenderer)renderer).onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEGLViewInitialized(GLSurfaceView view) {
        view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause(Renderer renderer) {
        if (DEBUG) Log.d(TAG, "onPause: " + renderer);
        ((PhotoPhaseRenderer)renderer).onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume(Renderer renderer) {
        if (DEBUG) Log.d(TAG, "onResume: " + renderer);
        ((PhotoPhaseRenderer)renderer).onResume();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Renderer getNewRenderer(GLSurfaceView view) {
        if (DEBUG) Log.d(TAG, "getNewRenderer()");
        PhotoPhaseRenderer renderer = new PhotoPhaseRenderer(this,
                new GLESSurfaceDispatcher(view), mEngine.isPreview());
        renderer.onCreate();
        mRenderers.add(renderer);
        if (DEBUG) Log.d(TAG, "renderer" + renderer);
        return renderer;
    }
}
