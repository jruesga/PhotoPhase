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
import android.opengl.GLES20;

import org.cyanogenmod.wallpapers.photophase.GLESUtil.GLColor;
import org.cyanogenmod.wallpapers.photophase.GLESUtil.GLESTextureInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


/**
 * A GLES square geometry that represents one photo frame for show in the wallpaper.
 */
public class PhotoFrame implements TextureRequestor {

    /**
     * @hide
     */
    public static final int COORDS_PER_VERTER = 3;

    // The photo frame is always a rectangle, so here applies 2 triangle order
    private static final short[] VERTEX_ORDER = { 0, 1, 2, 0, 2, 3 };

    // The default texture coordinates (fit to frame)
    private static final float[] DEFAULT_TEXTURE_COORDS = {
                                                            0, 0, // top left
                                                            0, 1, // bottom left
                                                            1, 1, // bottom right
                                                            1, 0  // top right
                                                          };

    private final TextureManager mTextureManager;

    private final float mFrameWidth, mFrameHeight;
    private final float mPictureWidth, mPictureHeight;
    private final float[] mFrameVertex;
    private final float[] mPictureVertex;

    private FloatBuffer mPictureVertexBuffer;
    private ShortBuffer mVertexOrderBuffer;
    private FloatBuffer mTextureBuffer;

    private GLESTextureInfo mTextureInfo;

    private final GLColor mBackgroundColor;

    private boolean mLoaded;

    private final Object mSync = new Object();

    /**
     * Constructor of <code>PhotoFrame</code>.
     *
     * @param ctx The current context
     * @param textureManager The texture manager
     * @param frameVertex A 4 dimension array with the coordinates per vertex plus padding
     * @param pictureVertex A 4 dimension array with the coordinates per vertex
     * @param color Background color
     */
    public PhotoFrame(Context ctx, TextureManager textureManager,
            float[] frameVertex, float[] pictureVertex, GLColor color) {
        super();
        mLoaded = false;
        mBackgroundColor = color;
        mTextureManager = textureManager;

        // Save dimensions
        mFrameVertex = frameVertex;
        mFrameWidth = frameVertex[9] - frameVertex[0];
        mFrameHeight = frameVertex[4] - frameVertex[1];
        mPictureVertex = pictureVertex;
        mPictureWidth = pictureVertex[9] - pictureVertex[0];
        mPictureHeight = pictureVertex[4] - pictureVertex[1];

        // Initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(pictureVertex.length * 4); // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder());
        mPictureVertexBuffer = bb.asFloatBuffer();
        mPictureVertexBuffer.put(pictureVertex);
        mPictureVertexBuffer.position(0);

        // Initialize vertex byte buffer for shape coordinates order
        bb = ByteBuffer.allocateDirect(VERTEX_ORDER.length * 2); // (# of coordinate values * 2 bytes per short)
        bb.order(ByteOrder.nativeOrder());
        mVertexOrderBuffer = bb.asShortBuffer();
        mVertexOrderBuffer.put(VERTEX_ORDER);
        mVertexOrderBuffer.position(0);

        // Load the texture
        mTextureInfo = null;

        // Request a new image for this frame
        textureManager.request(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTextureHandle(GLESTextureInfo ti) {
        // If the picture is invalid request a new texture
        if (ti == null || ti.handle <= 0) {
            mTextureManager.request(this);
            return;
        }

        // Full frame picture
        setTextureHandle(ti, DEFAULT_TEXTURE_COORDS);
        mLoaded = true;
    }

    /**
     * Internal method that expose the texture coordinates to set
     *
     * @param ti The texture info
     * @param textureCoords The texture coordinates
     */
    private void setTextureHandle(GLESTextureInfo ti, final float[] textureCoords) {
        // Recycle the previous handle
        if (mTextureInfo != null && mTextureInfo.handle != 0) {
            int[] textures = new int[]{mTextureInfo.handle};
            GLES20.glDeleteTextures(1, textures, 0);
            GLESUtil.glesCheckError("glDeleteTextures");
        }

        // Initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(textureCoords.length * 4); // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder());
        synchronized (mSync) {
            // Synchronize buffer swap
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
     * Method that returns the picture vertex
     *
     * @return float[] The picture vertex
     */
    public float[] getPictureVertex() {
        return mPictureVertex;
    }

    /**
     * Method that returns the picture vertex buffer
     *
     * @return FloatBuffer The picture vertex buffer
     */
    public FloatBuffer getPictureVertexBuffer() {
        return mPictureVertexBuffer;
    }

    /**
     * Method that returns the vertex order buffer
     *
     * @return ShortBuffer The vertex order buffer
     */
    public ShortBuffer getVertexOrderBuffer() {
        return mVertexOrderBuffer;
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
     * Method that returns the picture width
     *
     * @return float The picture width
     */
    public float getPictureWidth() {
        return mPictureWidth;
    }

    /**
     * Method that returns the picture height
     *
     * @return float The picture height
     */
    public float getPictureHeight() {
        return mPictureHeight;
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
            GLES20.glDeleteTextures(1, textures, 0);
            GLESUtil.glesCheckError("glDeleteTextures");
        }
        mTextureInfo = null;

        if (mPictureVertexBuffer != null) {
            mPictureVertexBuffer.clear();
        }
        if (mVertexOrderBuffer != null) {
            mVertexOrderBuffer.clear();
        }
        if (mTextureBuffer != null) {
            mTextureBuffer.clear();
        }
        mPictureVertexBuffer = null;
        mVertexOrderBuffer = null;
        mTextureBuffer = null;
    }
}
