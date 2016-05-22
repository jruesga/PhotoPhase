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

import android.graphics.RectF;
import android.opengl.GLES20;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.model.Disposition;
import com.ruesga.android.wallpapers.photophase.textures.TextureManager;
import com.ruesga.android.wallpapers.photophase.textures.TextureRequestor;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLColor;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLESTextureInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


/**
 * A GLES square geometry that represents one photo frame for show in the wallpaper.
 */
public class PhotoFrame implements TextureRequestor {

    // The default texture coordinates (fit to frame)
    private static final float[] DEFAULT_TEXTURE_COORDS = {
                                                            0.0f, 1.0f,
                                                            1.0f, 1.0f,
                                                            0.0f, 0.0f,
                                                            1.0f, 0.0f
                                                          };


    private final Disposition mDisposition;
    private final TextureManager mTextureManager;

    private final float[] mFrameVertex, mPhotoVertex;
    private final float mFrameWidth, mFrameHeight;
    private final float mPhotoWidth, mPhotoHeight;


    private FloatBuffer mPositionBuffer;
    private FloatBuffer mTextureBuffer;

    private GLESTextureInfo mTextureInfo;

    private final GLColor mBackgroundColor;

    private boolean mLoaded;

    private final Object mSync = new Object();

    /**
     * Constructor of <code>PhotoFrame</code>.
     *
     * @param disposition The associated disposition
     * @param textureManager The texture manager
     * @param frameVertex A 4 dimension array with the coordinates per vertex plus padding
     * @param photoVertex A 4 dimension array with the coordinates per vertex without padding
     * @param color Background color
     */
    public PhotoFrame(Disposition disposition, TextureManager textureManager, float[] frameVertex,
            float[] photoVertex, GLColor color) {
        super();
        mDisposition = disposition;
        mLoaded = false;
        mBackgroundColor = color;
        mTextureManager = textureManager;

        // Save dimensions
        mFrameVertex = frameVertex;
        mFrameWidth = frameVertex[6] - frameVertex[4];
        mFrameHeight = frameVertex[1] - frameVertex[5];
        mPhotoVertex = photoVertex;
        mPhotoWidth = photoVertex[6] - photoVertex[4];
        mPhotoHeight = photoVertex[5] - photoVertex[1];

        // Initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(photoVertex.length * 4); // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder());
        mPositionBuffer = bb.asFloatBuffer();
        mPositionBuffer.put(photoVertex);
        mPositionBuffer.position(0);

        // Load the texture
        mTextureInfo = null;

        // Request a new image for this frame
        requestTexture();
    }

    public Disposition getDisposition() {
        return mDisposition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RectF getRequestorDimensions() {
        return new RectF(0, 0, mPhotoWidth, mPhotoHeight);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTextureHandle(GLESTextureInfo ti) {
        // If the picture is invalid request a new texture
        if (ti == null || ti.handle <= 0) {
            requestTexture();
            return;
        }

        // Full frame picture
        setTextureHandle(ti, DEFAULT_TEXTURE_COORDS);
        mLoaded = true;
    }

    public void requestTexture() {
        mTextureManager.request(this);
    }

    /**
     * Internal method that expose the texture coordinates to set
     *
     * @param ti The texture info
     * @param textureCoords The texture coordinates
     */
    private void setTextureHandle(GLESTextureInfo ti, final float[] textureCoords) {
        // Recycle the previous handle
        if (mTextureInfo != null) {
            if (GLES20.glIsTexture(mTextureInfo.handle)) {
                int[] textures = new int[]{mTextureInfo.handle};
                if (GLESUtil.DEBUG_GL_MEMOBJS) {
                    Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteTextures: ["
                            + mTextureInfo.handle + "]");
                }
                GLES20.glDeleteTextures(1, textures, 0);
                GLESUtil.glesCheckError("glDeleteTextures");
            }
            if (mTextureInfo.bitmap != null) {
                mTextureInfo.bitmap.recycle();
                mTextureInfo.bitmap = null;
            }
        }

        // Initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(textureCoords.length * 4); // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder());
        synchronized (mSync) {
            // Swap buffers
            mTextureBuffer = bb.asFloatBuffer();
            mTextureBuffer.put(textureCoords);
            mTextureBuffer.position(0);
        }
        mTextureInfo = ti;
    }

    /**
     * Method that returns the frame vertex
     *
     * @return float[] The frame vertex
     */
    public float[] getFrameVertex() {
        return mFrameVertex;
    }

    /**
     * Method that returns the frame width
     *
     * @return float The frame width
     */
    public float getFrameWidth() {
        return mFrameWidth;
    }

    /**
     * Method that returns the frame height
     *
     * @return float The frame height
     */
    public float getFrameHeight() {
        return mFrameHeight;
    }

    /**
     * Method that returns the photo vertex
     *
     * @return float[] The photo vertex
     */
    public float[] getPhotoVertex() {
        return mPhotoVertex;
    }

    /**
     * Method that returns the position vertex buffer
     *
     * @return FloatBuffer The position vertex buffer
     */
    public FloatBuffer getPositionBuffer() {
        return mPositionBuffer;
    }

    /**
     * Method that returns the texture buffer
     *
     * @return FloatBuffer The texture buffer
     */
    public FloatBuffer getTextureBuffer() {
        synchronized (mSync) {
            return mTextureBuffer;
        }
    }

    /**
     * Method that returns the texture handle
     *
     * @return int The texture handle
     */
    public int getTextureHandle() {
        if (mTextureInfo != null) {
            return mTextureInfo.handle;
        }
        return -1;
    }

    /**
     * Method that returns the texture info
     *
     * @return GLESTextureInfo The texture info
     */
    public GLESTextureInfo getTextureInfo() {
        return mTextureInfo;
    }

    /**
     * Method that returns the background color of the frame
     *
     * @return GLColor The background color
     */
    public GLColor getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * Method that returns if the frame is loaded (has its picture loaded)
     *
     * @return boolean If the frame is loaded (has its picture loaded)
     */
    public boolean isLoaded() {
        return mLoaded;
    }

    /**
     * Request a recycle of the references of the object
     */
    public void recycle() {
        if (mTextureInfo != null && mTextureInfo.handle != 0) {
            int[] textures = new int[]{mTextureInfo.handle};
            if (GLESUtil.DEBUG_GL_MEMOBJS) {
                Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteTextures: ["
                        + mTextureInfo.handle + "]");
            }
            GLES20.glDeleteTextures(1, textures, 0);
            GLESUtil.glesCheckError("glDeleteTextures");
        }
        if (mTextureInfo != null && mTextureInfo.bitmap != null
                && !mTextureInfo.bitmap.isRecycled()) {
            mTextureInfo.bitmap.recycle();
        }
        mTextureInfo = null;

        if (mPositionBuffer != null) {
            mPositionBuffer.clear();
        }
        if (mTextureBuffer != null) {
            mTextureBuffer.clear();
        }
        mPositionBuffer = null;
        mTextureBuffer = null;
    }
}
