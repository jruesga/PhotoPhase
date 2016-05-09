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
import android.opengl.GLSurfaceView;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

/**
 * An abstract class for using a {@link GLSurfaceView} inside a {@link WallpaperService}.
 */
public abstract class EGLWallpaperService extends WallpaperService {

    /**
     * A listener interface for the {@link GLESWallpaperService.GLESEngine} engine class.
     */
    public interface EGLEngineListener {
        // No methods
    }

    /**
     * An EGL implementation of {@link android.service.wallpaper.WallpaperService.Engine} that
     * uses {@link GLSurfaceView}.
     */
    public class EGLEngine extends Engine {
        /**
         * The internal {@link GLSurfaceView}.
         */
        class WallpaperGLSurfaceView extends GLSurfaceView {

            /**
             * @see GLSurfaceView
             */
            WallpaperGLSurfaceView(Context context) {
                super(context);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }

            /**
             * Should be called when the {@link GLSurfaceView} is not needed anymore.
             */
            public void onDestroy() {
                super.onDetachedFromWindow();
            }
        }

        private WallpaperGLSurfaceView mGlSurfaceView;

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            mGlSurfaceView = createWallpaperGLSurfaceView();
        }

        /**
         * Method that returns the internal {@link GLSurfaceView}
         *
         * @return GLSurfaceView The internal {@link GLSurfaceView}.
         */
        protected GLSurfaceView getGlSurfaceView() {
            return mGlSurfaceView;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDestroy() {
            super.onDestroy();
            mGlSurfaceView.onDestroy();
        }

        /**
         * Override this method if a {@link GLSurfaceView} wrapper is needed, for example
         * if you need implements some eve
         *
         * @return WallpaperGLSurfaceView The specialized EGL {@link GLSurfaceView}.
         */
        public WallpaperGLSurfaceView createWallpaperGLSurfaceView() {
            return new WallpaperGLSurfaceView(EGLWallpaperService.this);
        }
    }
}
