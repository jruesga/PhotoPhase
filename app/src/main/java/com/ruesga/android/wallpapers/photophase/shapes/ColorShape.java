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
import android.opengl.GLES20;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLColor;
import com.ruesga.android.wallpapers.photophase.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * A shape plus color.
 */
public class ColorShape implements DrawableShape {

    private int mProgramHandler;
    private int mPositionHandler;
    private int mColorHandler;
    private int mMatrixHandler;
    private FloatBuffer mVertexBuffer;

    private final GLColor mColor;

    /**
     * Constructor of <code>ColorShape</code>.
     *
     * @param ctx The current context
     * @param vertex The vertext data
     * @param color The color
     */
    public ColorShape(Context ctx, float[] vertex, GLColor color) {
        super();
        mColor = color;

        mProgramHandler = GLESUtil.createProgram(
                                                ctx.getResources(),
                                                R.raw.color_vertex_shader,
                                                R.raw.color_fragment_shader);
        mPositionHandler = GLES20.glGetAttribLocation(mProgramHandler, "aPosition");
        GLESUtil.glesCheckError("glGetAttribLocation");
        mColorHandler = GLES20.glGetAttribLocation(mProgramHandler, "aColor");
        GLESUtil.glesCheckError("glGetAttribLocation");
        mMatrixHandler = GLES20.glGetUniformLocation(mProgramHandler, "uMVPMatrix");
        GLESUtil.glesCheckError("glGetUniformLocation");

        // Initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(vertex.length * 4); // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(vertex);
        mVertexBuffer.position(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(float[] matrix) {
        // Bind default FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLESUtil.glesCheckError("glBindFramebuffer");

        if (mColor.a == 0f) {
            return;
        }

        // Enable properties
        GLES20.glEnable(GLES20.GL_BLEND);
        GLESUtil.glesCheckError("glEnable");
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLESUtil.glesCheckError("glBlendFunc");

        // Set the program and its attributes
        GLES20.glUseProgram(mProgramHandler);
        GLESUtil.glesCheckError("glUseProgram");

        // Position
        mVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandler, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mPositionHandler);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Color
        GLES20.glVertexAttrib4f(mColorHandler, mColor.r, mColor.g, mColor.b, mColor.a);
        GLESUtil.glesCheckError("glVertexAttrib4f");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMatrixHandler, 1, false, matrix, 0);
        GLESUtil.glesCheckError("glUniformMatrix4fv");

        // Draw the photo frame
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtil.glesCheckError("glDrawElements");

        // Disable attributes
        GLES20.glDisableVertexAttribArray(mPositionHandler);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mColorHandler);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");

        // Disable properties
        GLES20.glDisable(GLES20.GL_BLEND);
        GLESUtil.glesCheckError("glDisable");
    }

    /**
     * Method that sets the alpha color of the shape
     *
     * @param value The new alpha color of the shape
     */
    public void setAlpha(float value) {
        mColor.a = value;
    }

    /**
     * Method that destroy all the internal references
     */
    public void recycle() {
        if (GLES20.glIsProgram(mProgramHandler)) {
            if (GLESUtil.DEBUG_GL_MEMOBJS) {
                Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteProgram: " + mProgramHandler);
            }
            GLES20.glDeleteProgram(mProgramHandler);
            GLESUtil.glesCheckError("glDeleteProgram");
        }
        mProgramHandler = 0;
        mPositionHandler = 0;
        mColorHandler = 0;
        mMatrixHandler = 0;
        mVertexBuffer.clear();
        mVertexBuffer = null;
    }
}
