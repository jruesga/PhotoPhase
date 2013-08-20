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

package org.cyanogenmod.wallpapers.photophase.shapes;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;

import org.cyanogenmod.wallpapers.photophase.Colors;
import org.cyanogenmod.wallpapers.photophase.utils.GLESUtil;
import org.cyanogenmod.wallpapers.photophase.utils.GLESUtil.GLColor;
import org.cyanogenmod.wallpapers.photophase.utils.GLESUtil.GLESTextureInfo;
import org.cyanogenmod.wallpapers.photophase.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * A shape to draw an oops message
 */
public class OopsShape implements DrawableShape {

    private static final int VERTEX_SHADER = R.raw.default_vertex_shader;
    private static final int FRAGMENT_SHADER = R.raw.default_fragment_shader;

    // The texture coordinates
    private static final float[] TEXTURE_COORDS = {
                                                    0.0f, 1.0f,
                                                    1.0f, 1.0f,
                                                    0.0f, 0.0f,
                                                    1.0f, 0.0f
                                                  };

    // The vertex position coordinates
    private static final float[] VERTEX_COORDS_PORTRAIT = {
                                                           -0.75f, -0.5f,
                                                            0.75f, -0.5f,
                                                           -0.75f,  0.5f,
                                                            0.75f,  0.5f
                                                         };
    private static final float[] VERTEX_COORDS_LANDSCAPE = {
                                                            -0.5f, -0.75f,
                                                             0.5f, -0.75f,
                                                            -0.5f,  0.75f,
                                                             0.5f,  0.75f
                                                          };

    private FloatBuffer mPositionBuffer;
    private FloatBuffer mTextureBuffer;

    private int[] mProgramHandlers;
    private int[] mTextureHandlers;
    private int[] mPositionHandlers;
    private int[] mTextureCoordHandlers;
    private int[] mMVPMatrixHandlers;

    private String mMessage;

    private GLESTextureInfo mOopsImageTexture;
    private GLESTextureInfo mOopsTextTexture;

    /**
     * Constructor of <code>OopsShape</code>
     *
     * @param ctx The current context
     * @param resourceMessageId The resource identifier with the message
     */
    public OopsShape(Context ctx, int resourceMessageId) {
        super();

        int orientation = ctx.getResources().getConfiguration().orientation;
        float[] vertex = VERTEX_COORDS_PORTRAIT;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            vertex = VERTEX_COORDS_LANDSCAPE;
        }

        // Load the buffers
        ByteBuffer bb1 = ByteBuffer.allocateDirect(vertex.length * 4); // (# of coordinate values * 4 bytes per float)
        bb1.order(ByteOrder.nativeOrder());
        mPositionBuffer = bb1.asFloatBuffer();
        mPositionBuffer.put(vertex);
        mPositionBuffer.position(0);
        // -
        ByteBuffer bb2 = ByteBuffer.allocateDirect(TEXTURE_COORDS.length * 4); // (# of coordinate values * 4 bytes per float)
        bb2.order(ByteOrder.nativeOrder());
        mTextureBuffer = bb2.asFloatBuffer();
        mTextureBuffer.put(TEXTURE_COORDS);
        mTextureBuffer.position(0);

        // Initialize the structures
        mProgramHandlers = new int[2];
        mTextureHandlers = new int[2];
        mPositionHandlers = new int[2];
        mTextureCoordHandlers = new int[2];
        mMVPMatrixHandlers = new int[2];

        // Create all the params
        for (int i = 0; i < 2; i++) {
            mProgramHandlers[i] =
                    GLESUtil.createProgram(
                            ctx.getResources(), VERTEX_SHADER, FRAGMENT_SHADER);
            mTextureHandlers[i] =
                    GLES20.glGetAttribLocation(mProgramHandlers[i], "sTexture");
            mPositionHandlers[i] =
                    GLES20.glGetAttribLocation(mProgramHandlers[i], "aPosition");
            GLESUtil.glesCheckError("glGetAttribLocation");
            mTextureCoordHandlers[i] =
                    GLES20.glGetAttribLocation(mProgramHandlers[i], "aTextureCoord");
            GLESUtil.glesCheckError("glGetAttribLocation");
            mMVPMatrixHandlers[i] =
                    GLES20.glGetUniformLocation(mProgramHandlers[i], "uMVPMatrix");
            GLESUtil.glesCheckError("glGetUniformLocation");
        }

        // Get the localized message
        mMessage = ctx.getString(resourceMessageId);

        // Load the textures
        mOopsImageTexture = GLESUtil.loadTexture(ctx, R.drawable.bg_cid_oops, null, null, false);
        Bitmap textBitmap = text2Bitmap(ctx, mMessage);
        mOopsTextTexture = GLESUtil.loadTexture(textBitmap, null, null);

        // Recycle
        mOopsImageTexture.bitmap.recycle();
        mOopsImageTexture.bitmap = null;
        textBitmap.recycle();
        textBitmap = null;
        mOopsTextTexture.bitmap.recycle();
        mOopsTextTexture.bitmap = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(float[] matrix) {
        // Bind default FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLESUtil.glesCheckError("glBindFramebuffer");

        // Clear background
        GLColor bg = Colors.getBackground();
        GLES20.glClearColor(bg.r, bg.g, bg.b, bg.a);
        GLESUtil.glesCheckError("glClearColor");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLESUtil.glesCheckError("glClear");

        // Enable blend
        GLES20.glEnable(GLES20.GL_BLEND);
        GLESUtil.glesCheckError("glEnable");
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_ONE_MINUS_SRC_COLOR);
        GLESUtil.glesCheckError("glBlendFunc");

        // Draw the textures
        drawTexture(matrix, 0, mOopsImageTexture.handle);
        drawTexture(matrix, 1, mOopsTextTexture.handle);

        // Disable blending
        GLES20.glDisable(GLES20.GL_BLEND);
        GLESUtil.glesCheckError("glDisable");
    }

    /**
     * Method that draws a texture
     *
     * @param matrix The model-view-projection matrix
     * @param index The index of the texture
     * @param texture The texture handler
     */
    private void drawTexture(float[] matrix, int index, int texture) {
        // Use our shader program
        GLES20.glUseProgram(mProgramHandlers[index]);
        GLESUtil.glesCheckError("glUseProgram()");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandlers[index], 1, false, matrix, 0);
        GLESUtil.glesCheckError("glUniformMatrix4fv");

        // Texture
        GLES20.glVertexAttribPointer(mTextureCoordHandlers[index], 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandlers[index]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Position
        GLES20.glVertexAttribPointer(mPositionHandlers[index], 2, GLES20.GL_FLOAT, false, 0, mPositionBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mPositionHandlers[index]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Set the input textures
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLESUtil.glesCheckError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLESUtil.glesCheckError("glBindTexture");
        GLES20.glUniform1i(mTextureHandlers[index], 0);
        GLESUtil.glesCheckError("glUniform1i");

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtil.glesCheckError("glDrawElements");

        // Disable attributes
        GLES20.glDisableVertexAttribArray(mPositionHandlers[index]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mTextureCoordHandlers[index]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
    }

    /**
     * Method that requests to remove the internal references and resources.
     */
    public void recycle() {
        // Remove textures
        if (mOopsImageTexture != null && mOopsImageTexture.handle != 0) {
            int[] textures = new int[]{mOopsImageTexture.handle};
            GLES20.glDeleteTextures(1, textures, 0);
            GLESUtil.glesCheckError("glDeleteTextures");
        }
        mOopsImageTexture = null;
        if (mOopsTextTexture != null && mOopsTextTexture.handle != 0) {
            int[] textures = new int[]{mOopsTextTexture.handle};
            GLES20.glDeleteTextures(1, textures, 0);
            GLESUtil.glesCheckError("glDeleteTextures");
        }
        mOopsTextTexture = null;

        // Remove buffers
        if (mPositionBuffer != null) {
            mPositionBuffer.clear();
        }
        if (mTextureBuffer != null) {
            mTextureBuffer.clear();
        }
        mPositionBuffer = null;
        mTextureBuffer = null;

        for (int i = 0; i < 2; i++) {
            if (GLES20.glIsProgram(mProgramHandlers[i])) {
                GLES20.glDeleteProgram(mProgramHandlers[i]);
                GLESUtil.glesCheckError("glDeleteProgram(" + i + ")");
            }
            mProgramHandlers[i] = 0;
            mTextureHandlers[i] = 0;
            mPositionHandlers[i] = 0;
            mTextureCoordHandlers[i] = 0;
            mMVPMatrixHandlers[i] = 0;
        }
    }

    /**
     * Method that converts a text to a bitmap
     *
     * @param ctx The current context
     * @param text The text to draw to the bitmap
     * @return Bitmap The bitmap with the text
     */
    public Bitmap text2Bitmap(Context ctx, String text) {
        Paint paint = new Paint();
        Typeface font = Typeface.createFromAsset(ctx.getAssets(), "fonts/Roboto-Bold.ttf");
        paint.setTypeface(font);
        paint.setColor(Color.WHITE);
        paint.setTextSize(24.0f);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        Bitmap src = mOopsImageTexture.bitmap;
        Bitmap image = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, src.getWidth()/2, src.getHeight() - (src.getHeight() * 0.33f), paint);
        return image;
    }
}
