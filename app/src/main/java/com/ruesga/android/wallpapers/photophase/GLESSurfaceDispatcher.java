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

/**
 * A class responsible of dispatch GLES commands inside the main GLThread.
 */
public class GLESSurfaceDispatcher {

    private final GLSurfaceView mSurface;

    /**
     * Constructor of <code>GLESSurfaceDispatcher</code>
     *
     * @param v The associated GLES surface view
     */
    public GLESSurfaceDispatcher(GLSurfaceView v) {
        super();
        mSurface = v;
    }

    /**
     * Check whether the surface has a valid context
     */
    public boolean hasValidSurface() {
        try {
            return mSurface.getHolder().getSurface().isValid();
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Method that dispatch a GLES commands inside the main GLThread.
     *
     * @param r The runnable that execute the GLES commands
     */
    public void dispatch(Runnable r) {
        this.mSurface.queueEvent(r);
    }

    /**
     * Method that set the render mode
     *
     * @param mode The GLES render mode
     */
    public void setRenderMode(int mode) {
        if (mSurface.getRenderMode() != mode) {
            mSurface.setRenderMode(mode);
        }
    }

    /**
     * Method that requests a render to the surface.
     */
    public void requestRender() {
        mSurface.requestRender();
    }
}
