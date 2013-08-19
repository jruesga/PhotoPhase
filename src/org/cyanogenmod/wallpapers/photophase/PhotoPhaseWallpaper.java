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

import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ViewConfiguration;

import org.cyanogenmod.wallpapers.photophase.GLESWallpaperService.GLESEngineListener;
import org.cyanogenmod.wallpapers.photophase.preferences.PreferencesProvider;

import java.util.ArrayList;
import java.util.List;


/**
 * The PhotoPhase Live Wallpaper service.
 */
public class PhotoPhaseWallpaper
    extends GLES20WallpaperService implements GLESEngineListener {

    private static final String TAG = "PhotoPhaseWallpaper";

    private static final boolean DEBUG = false;

    private List<PhotoPhaseRenderer> mRenderers;
    private PhotoPhaseWallpaperEngine mEngine;

    private boolean mPreserveEGLContext;

    // List of the current top activities. Tap should be ignored when this acitivities are
    // in the foreground
    static final String[] TOP_ACTIVITIES = {"com.android.internal.app.ChooserActivity"};

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "onCreate");
        super.onCreate();

        // Load the configuration
        mPreserveEGLContext = getResources().getBoolean(R.bool.config_preserve_egl_context);
        mRenderers = new ArrayList<PhotoPhaseRenderer>();

        // Instance the application
        PreferencesProvider.reload(this);
        Colors.register(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        super.onDestroy();
        for (PhotoPhaseRenderer renderer : mRenderers) {
            renderer.onDestroy();
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
        /*package*/ final ActivityManager mActivityManager;

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
            if (action.compareTo(WallpaperManager.COMMAND_TAP) == 0) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Only if the wallpaper is visible after a long press and
                        // not in preview mode
                        if (isVisible() && !isPreview()) {
                            List<ActivityManager.RunningTaskInfo> taskInfo =
                                                    mActivityManager.getRunningTasks(1);
                            String topActivity = taskInfo.get(0).topActivity.getClassName();
                            for (String activity : TOP_ACTIVITIES) {
                                if (activity.compareTo(topActivity) == 0) {
                                    // Ignore tap event
                                    return;
                                }
                            }

                            // Pass the x and y position to the renderer
                            ((PhotoPhaseRenderer)getRenderer()).onTouch(x, y);
                        }
                    }
                }, ViewConfiguration.getLongPressTimeout() + 100L);
            }
            return super.onCommand(action, x, y, z, extras, resultRequested);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(TAG, "onLowMemory");
        for (PhotoPhaseRenderer renderer : mRenderers) {
            // Pause the wallpaper and destroy the cached textures
            renderer.onPause();
            renderer.onLowMemory();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInitializeEGLView(GLSurfaceView view) {
        if (DEBUG) Log.d(TAG, "onInitializeEGLView");
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
        view.setPreserveEGLContextOnPause(mPreserveEGLContext);
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
        PhotoPhaseRenderer renderer = new PhotoPhaseRenderer(this, new GLESSurfaceDispatcher(view));
        renderer.onCreate();
        mRenderers.add(renderer);
        if (DEBUG) Log.d(TAG, "renderer" + renderer);
        return renderer;
    }
}
