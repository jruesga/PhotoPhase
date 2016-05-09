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

import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.view.SurfaceHolder;


/**
 * An abstract implementation of {@link EGLWallpaperService} based on <code>GLES</code>.
 */
public abstract class GLESWallpaperService extends EGLWallpaperService {

    /**
     * A listener interface for the {@link GLESWallpaperService.GLESEngine} engine class.
     */
    public interface GLESEngineListener extends EGLEngineListener {
        /**
         * Method invoked when the EGL surface is starting to initialize.
         *
         * @param view GLSurfaceView The EGL view
         */
        void onInitializeEGLView(GLSurfaceView view);

        /**
         * Method invoked when the EGL surface is recycled.
         *
         * @param view GLSurfaceView The EGL view
         * @param renderer The renderer associated
         */
        void onDestroyEGLView(GLSurfaceView view, Renderer renderer);

        /**
         * Method invoked when the EGL surface was initialized.
         *
         * @param view GLSurfaceView The EGL view
         */
        void onEGLViewInitialized(GLSurfaceView view);

        /**
         * Method invoked when the EGL context is paused
         *
         * @param renderer The renderer associated
         */
        void onPause(Renderer renderer);

        /**
         * Method invoked when the EGL context is resumed
         *
         * @param renderer The renderer associated
         */
        void onResume(Renderer renderer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Engine onCreateEngine() {
        return new GLESEngine();
    }

    /**
     * A class that extends the {@link EGLWallpaperService.EGLEngine} to add support for
     * <code>GLES</code>.
     */
    class GLESEngine extends EGLWallpaperService.EGLEngine {

        private GLESEngineListener mListener = null;
        private WallpaperGLSurfaceView mWallpaperGLSurfaceView = null;
        private Renderer mRenderer = null;

        private boolean mRendererHasBeenSet;
        private boolean mPauseOnPreview;

        /**
         * Method that sets the EGL engine listener
         *
         * @param listener The EGL engine listener
         */
        public void setGLESEngineListener(GLESEngineListener listener) {
            mListener = listener;
        }

        /**
         * Method that sets the {@link GLSurfaceView} to use.
         *
         * @param wallpaperGLSurfaceView A {@link GLSurfaceView}
         */
        public void setWallpaperGLSurfaceView(WallpaperGLSurfaceView wallpaperGLSurfaceView) {
            mWallpaperGLSurfaceView = wallpaperGLSurfaceView;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            // Notify initialization
            if (mListener != null) {
                mListener.onInitializeEGLView(getGlSurfaceView());
            }

            // Initialize the GLES context
            initialize();

            // Set the renderer to our user-defined renderer.
            mRenderer = getNewRenderer(getGlSurfaceView());
            getGlSurfaceView().setRenderer(mRenderer);
            mRendererHasBeenSet = true;

            // Notify that the EGL is initialized
            if (mListener != null) {
                mListener.onEGLViewInitialized(getGlSurfaceView());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDestroy() {
            // Notify initialization
            getGlSurfaceView().queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onDestroyEGLView(getGlSurfaceView(), mRenderer);
                    }
                    mRenderer = null;
                }
            });

            super.onDestroy();
        }

        /**
         * Method that initializes
         */
        void initialize() {
            // Request an OpenGL ES 1.x compatible context.
            getGlSurfaceView().setEGLContextClientVersion(1);
        }

        /**
         * Returns the renderer used by the engine
         *
         * @return Renderer The renderer
         */
        protected Renderer getRenderer() {
            return mRenderer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public WallpaperGLSurfaceView createWallpaperGLSurfaceView() {
            // Check whether to use a proprietary GLSurfaceView reference or an internal one
            if (mWallpaperGLSurfaceView != null) {
                return mWallpaperGLSurfaceView;
            }
            return super.createWallpaperGLSurfaceView();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (mRendererHasBeenSet) {
                if (visible) {
                    getGlSurfaceView().onResume();
                    getGlSurfaceView().setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                    mListener.onResume(mRenderer);
                } else {
                    // Check that the user is not previewing the live wallpaper; if they are, then
                    // if they open up a settings dialog that appears over the preview, we don’t
                    // want to pause rendering
                    boolean preview = isPreview();
                    if (!preview || mPauseOnPreview) {
                        getGlSurfaceView().setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                        getGlSurfaceView().onPause();
                        mListener.onPause(mRenderer);
                    }
                }
            }
        }

        /**
         * Method that determines if the surface view should be paused on preview mode
         *
         * @return boolean whether the surface view should be paused on preview mode
         */
        public boolean isPauseOnPreview() {
            return mPauseOnPreview;
        }

        /**
         * Method that sets if the surface view should be paused on preview mode
         *
         * @param pauseOnPreview whether the surface view should be paused on preview mode
         */
        public void setPauseOnPreview(boolean pauseOnPreview) {
            this.mPauseOnPreview = pauseOnPreview;
        }
    }

    /**
     * Method that return a new EGL renderer.
     *
     * @param view The view that will be associated to the renderer
     * @return Renderer The new EGL renderer.
     */
    public abstract Renderer getNewRenderer(GLSurfaceView view);
}
