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




/**
 * An abstract implementation of {@link EGLWallpaperService} based on <code>GLES</code>.
 */
public abstract class GLES20WallpaperService extends GLESWallpaperService {

    /**
     * {@inheritDoc}
     */
    @Override
    public Engine onCreateEngine() {
        return new GLES20Engine();
    }

    /**
     * A class that extends the {@link GLESWallpaperService.GLESEngine} to add support for
     * <code>GLES20</code>.
     */
    class GLES20Engine extends GLESWallpaperService.GLESEngine {
        /**
         * {@inheritDoc}
         */
        @Override
        void initialize() {
            // Request an OpenGL ES 2.x compatible context.
            getGlSurfaceView().setEGLContextClientVersion(2);
            getGlSurfaceView().setEGLConfigChooser(false);
        }
    }
}
