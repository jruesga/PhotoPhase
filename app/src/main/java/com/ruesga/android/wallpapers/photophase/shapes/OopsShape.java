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

package com.ruesga.android.wallpapers.photophase.shapes;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.AndroidHelper;
import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLColor;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLESTextureInfo;

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

    private static Typeface sFont;

    private final Context mContext;

    private FloatBuffer mPositionBuffer;
    private FloatBuffer mTextureBuffer;

    private final int[] mProgramHandlers;
    private final int[] mTextureHandlers;
    private final int[] mPositionHandlers;
    private final int[] mTextureCoordHandlers;
    private final int[] mMVPMatrixHandlers;

    private GLESTextureInfo mOopsImageTexture;
    private GLESTextureInfo mOopsTextTexture;
    private GLESTextureInfo mNoPermissionTextTexture;

    /**
     * Constructor of <code>OopsShape</code>
     *
     * @param ctx The current context
     */
    public OopsShape(Context ctx) {
        mContext = ctx;

        if (sFont == null) {
            sFont = Typeface.createFromAsset(ctx.getAssets(), "fonts/NotoSans-Bold.ttf");
        }

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
                    GLES20.glGetUniformLocation(mProgramHandlers[i], "sTexture");
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

        // Load the textures
        mOopsImageTexture = GLESUtil.loadTexture(ctx, R.drawable.bg_oops, null, null, null, false);
        Bitmap textBitmap = text2Bitmap(ctx.getString(R.string.no_pictures_oops_msg));
        mOopsTextTexture = GLESUtil.loadTexture(mContext, textBitmap, null, null, null);
        Bitmap noPermissionTextBitmap =
                text2Bitmap(ctx.getString(R.string.no_pictures_permission_required_msg));
        mNoPermissionTextTexture = GLESUtil.loadTexture(
                mContext, noPermissionTextBitmap, null, null, null);

        // Recycle
        mOopsImageTexture.bitmap.recycle();
        mOopsImageTexture.bitmap = null;
        textBitmap.recycle();
        noPermissionTextBitmap.recycle();
        mOopsTextTexture.bitmap.recycle();
        mOopsTextTexture.bitmap = null;
        mNoPermissionTextTexture.bitmap.recycle();
        mNoPermissionTextTexture.bitmap = null;
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
        GLColor bg = PreferencesProvider.Preferences.General.DEFAULT_BACKGROUND_COLOR;
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
        if (AndroidHelper.hasReadExternalStoragePermissionGranted(mContext)) {
            drawTexture(matrix, 1, mOopsTextTexture.handle);
        } else {
            drawTexture(matrix, 1, mNoPermissionTextTexture.handle);
        }

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
            if (GLESUtil.DEBUG_GL_MEMOBJS) {
                Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteTextures: ["
                        + mOopsImageTexture.handle + "]");
            }
            int[] textures = new int[]{mOopsImageTexture.handle};
            GLES20.glDeleteTextures(1, textures, 0);
            GLESUtil.glesCheckError("glDeleteTextures");
        }
        mOopsImageTexture = null;
        if (mOopsTextTexture != null && mOopsTextTexture.handle != 0) {
            int[] textures = new int[]{mOopsTextTexture.handle};
            if (GLESUtil.DEBUG_GL_MEMOBJS) {
                Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteTextures: ["
                        + mOopsTextTexture.handle + "]");
            }
            GLES20.glDeleteTextures(1, textures, 0);
            GLESUtil.glesCheckError("glDeleteTextures");
        }
        mOopsTextTexture = null;
        if (mNoPermissionTextTexture != null && mNoPermissionTextTexture.handle != 0) {
            int[] textures = new int[]{mNoPermissionTextTexture.handle};
            if (GLESUtil.DEBUG_GL_MEMOBJS) {
                Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteTextures: ["
                        + mNoPermissionTextTexture.handle + "]");
            }
            GLES20.glDeleteTextures(1, textures, 0);
            GLESUtil.glesCheckError("glDeleteTextures");
        }
        mNoPermissionTextTexture = null;

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
                if (GLESUtil.DEBUG_GL_MEMOBJS) {
                    Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteProgram: "
                            + mProgramHandlers[i]);
                }
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
     * @param text The text to draw to the bitmap
     * @return Bitmap The bitmap with the text
     */
    private Bitmap text2Bitmap(String text) {
        Paint paint = new Paint();
        paint.setTypeface(sFont);
        paint.setColor(Color.WHITE);
        paint.setTextSize(mContext.getResources().getDimensionPixelSize(R.dimen.oops_shape_text_size));
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        Bitmap src = mOopsImageTexture.bitmap;
        Bitmap image = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, src.getWidth() / 2, src.getHeight() - (src.getHeight() * 0.33f), paint);
        return image;
    }
}
