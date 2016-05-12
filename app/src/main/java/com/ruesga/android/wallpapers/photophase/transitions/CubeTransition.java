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

package com.ruesga.android.wallpapers.photophase.transitions;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.ruesga.android.wallpapers.photophase.PhotoFrame;
import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.textures.TextureManager;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions.TRANSITIONS;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;
import com.ruesga.android.wallpapers.photophase.utils.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A transition that applies a cube effect transition to the picture.
 */
public class CubeTransition extends Transition {

    /**
     * The enumeration of all possibles window movements
     */
    public enum WINDOW_MODES {
        /**
         * Open the picture from left to right
         */
        LEFT_TO_RIGHT,
        /**
         * Open the picture from right to left
         */
        RIGHT_TO_LEFT
    }

    private static final int[] VERTEX_SHADER = {R.raw.default_vertex_shader, R.raw.default_vertex_shader};
    private static final int[] FRAGMENT_SHADER = {R.raw.default_fragment_shader, R.raw.default_fragment_shader};

    private static final float TRANSITION_TIME = 1500.0f;

    private static final float SCALE_AMOUNT = 0.2f;

    private WINDOW_MODES mMode;

    private FloatBuffer mPositionBuffer;
    private final float[] mTranslationMatrix;
    private float[] mVertex;

    private float mAmount;

    public CubeTransition(Context ctx, TextureManager tm) {
        super(ctx, tm, VERTEX_SHADER, FRAGMENT_SHADER);

        // Initialized
        mTranslationMatrix = new float[16];
        reset();
    }

    @Override
    public TRANSITIONS getType() {
        return TRANSITIONS.CUBE;
    }

    @Override
    public float getTransitionTime() {
        return TRANSITION_TIME;
    }

    @Override
    public boolean hasTransitionTarget() {
        return true;
    }

    @Override
    public void select(PhotoFrame target) {
        super.select(target);
        mAmount = getAmount();

        // Create the interal buffer
        float[] vertex = target.getFrameVertex();
        if (mPositionBuffer == null) {
            // (# of coordinate values * 4 bytes per float)
            ByteBuffer bb = ByteBuffer.allocateDirect(vertex.length * 4);
            bb.order(ByteOrder.nativeOrder());
            mPositionBuffer = bb.asFloatBuffer();
        }
        if (mVertex == null) {
            mVertex = new float[vertex.length];
        }

        // choose a random mode
        chooseMode();
    }

    @Override
    public void chooseMode() {
        List<WINDOW_MODES> modes = new ArrayList<>(Arrays.asList(WINDOW_MODES.values()));
        int low = 0;
        int high = modes.size() - 1;
        mMode = modes.get(Utils.getNextRandom(low, high));
    }

    @Override
    public void applyTransition(float delta, float[] matrix) {
        if (delta < 1) {
            applyDstTransition(delta, matrix);
            applySrcTransition(delta, matrix);
        } else {
            applyFinalTransition(matrix);
        }
    }

    private void applySrcTransition(float delta, float[] matrix) {
        // Bind default FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLESUtil.glesCheckError("glBindFramebuffer");

        // Set the program
        useProgram(0);

        // Disable blending
        GLES20.glDisable(GLES20.GL_BLEND);
        GLESUtil.glesCheckError("glDisable");

        // Set the input texture
        int textureHandle = mTarget.getTextureHandle();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLESUtil.glesCheckError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
        GLESUtil.glesCheckError("glBindTexture");
        GLES20.glUniform1i(mTextureHandlers[0], 0);
        GLESUtil.glesCheckError("glBindTexture");

        // Texture
        FloatBuffer textureBuffer = mTarget.getTextureBuffer();
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandlers[0], 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandlers[0]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Position
        setInternalVertex();
        float interpolation = delta > 0.5f
                        ? 1 - delta
                        : delta;
        float w = Math.abs(mVertex[6] - mVertex[4]);
        switch (mMode) {
            case RIGHT_TO_LEFT:
                mVertex[1] -= interpolation * mAmount;
                mVertex[5] += interpolation * mAmount;
                mVertex[4] += w * delta;
                mVertex[0] = mVertex[4];
                break;
            case LEFT_TO_RIGHT:
                mVertex[3] -= interpolation * mAmount;
                mVertex[7] += interpolation * mAmount;
                mVertex[6] -= w * delta;
                mVertex[2] = mVertex[6];
                break;
            default:
                break;
        }
        mPositionBuffer.position(0);
        mPositionBuffer.put(mVertex);
        mPositionBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandlers[0], 2, GLES20.GL_FLOAT, false, 0, mPositionBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mPositionHandlers[0]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Calculate the delta angle and the translation and rotate parameters
        float angle = 0.0f;
        float translateX = 0.0f;
        float rotateY = 0.0f;
        switch (mMode) {
            case RIGHT_TO_LEFT:
                angle = delta * 90;
                rotateY = -1.0f;
                translateX = mVertex[2] * -1;
                break;
            case LEFT_TO_RIGHT:
                angle = delta * -90;
                rotateY = -1.0f;
                translateX = mVertex[0] * -1;
                break;

            default:
                break;
        }

        // Apply the projection and view transformation
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(mTranslationMatrix, 0, matrix, 0, -translateX, 0.0f, 0.0f);
        Matrix.rotateM(mTranslationMatrix, 0, mTranslationMatrix, 0, angle, 0.0f, rotateY, 0.0f);
        Matrix.translateM(mTranslationMatrix, 0, mTranslationMatrix, 0, translateX, 0.0f, 0.0f);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandlers[0], 1, false, mTranslationMatrix, 0);
        GLESUtil.glesCheckError("glUniformMatrix4fv");

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtil.glesCheckError("glDrawElements");

        // Disable attributes
        GLES20.glDisableVertexAttribArray(mPositionHandlers[0]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mTextureCoordHandlers[0]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
    }

    private void applyDstTransition(float delta, float[] matrix) {
        // Bind default FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLESUtil.glesCheckError("glBindFramebuffer");

        // Set the program
        useProgram(1);

        // Disable blending
        GLES20.glDisable(GLES20.GL_BLEND);
        GLESUtil.glesCheckError("glDisable");

        // Set the input texture
        int textureHandle = mTransitionTarget.getTextureHandle();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLESUtil.glesCheckError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
        GLESUtil.glesCheckError("glBindTexture");
        GLES20.glUniform1i(mTextureHandlers[1], 0);
        GLESUtil.glesCheckError("glBindTexture");

        // Texture
        FloatBuffer textureBuffer = mTransitionTarget.getTextureBuffer();
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandlers[1], 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandlers[1]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Position
        setInternalVertex();
        float interpolation = delta > 0.5f
                ? 1 - delta
                : delta;
        float w = Math.abs(mVertex[6] - mVertex[4]);
        switch (mMode) {
            case LEFT_TO_RIGHT:
                mVertex[1] -= interpolation * mAmount;
                mVertex[5] += interpolation * mAmount;
                mVertex[4] += w * (1 - delta);
                mVertex[0] = mVertex[4];
                break;
            case RIGHT_TO_LEFT:
                mVertex[3] -= interpolation * mAmount;
                mVertex[7] += interpolation * mAmount;
                mVertex[6] -= w * (1 - delta);
                mVertex[2] = mVertex[6];
                break;
            default:
                break;
        }
        mPositionBuffer.position(0);
        mPositionBuffer.put(mVertex);
        mPositionBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandlers[1], 2, GLES20.GL_FLOAT, false, 0, mPositionBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mPositionHandlers[1]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Calculate the delta angle and the translation and rotate parameters
        float angle = 0.0f;
        float translateX = 0.0f;
        float rotateY = 0.0f;
        switch (mMode) {
            case LEFT_TO_RIGHT:
                angle = 90 - (delta * 90);
                rotateY = -1.0f;
                translateX = mVertex[2] * -1;
                break;
            case RIGHT_TO_LEFT:
                angle = -90 + (delta * 90);
                rotateY = -1.0f;
                translateX = mVertex[0] * -1;
                break;

            default:
                break;
        }

        // Apply the projection and view transformation
        Matrix.setIdentityM(matrix, 0);
        Matrix.translateM(mTranslationMatrix, 0, matrix, 0, -translateX, 0.0f, 0.0f);
        Matrix.rotateM(mTranslationMatrix, 0, mTranslationMatrix, 0, angle, 0.0f, rotateY, 0.0f);
        Matrix.translateM(mTranslationMatrix, 0, mTranslationMatrix, 0, translateX, 0.0f, 0.0f);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandlers[1], 1, false, mTranslationMatrix, 0);
        GLESUtil.glesCheckError("glUniformMatrix4fv");

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtil.glesCheckError("glDrawElements");

        // Disable attributes
        GLES20.glDisableVertexAttribArray(mPositionHandlers[1]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mTextureCoordHandlers[1]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
    }

    private void applyFinalTransition(float[] matrix) {
        // Bind default FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLESUtil.glesCheckError("glBindFramebuffer");

        // Use our shader program
        useProgram(1);

        // Disable blending
        GLES20.glDisable(GLES20.GL_BLEND);
        GLESUtil.glesCheckError("glDisable");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandlers[1], 1, false, matrix, 0);
        GLESUtil.glesCheckError("glUniformMatrix4fv");

        // Texture
        FloatBuffer textureBuffer = mTransitionTarget.getTextureBuffer();
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandlers[1], 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandlers[1]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Position
        FloatBuffer positionBuffer = mTransitionTarget.getPositionBuffer();
        positionBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandlers[1], 2, GLES20.GL_FLOAT, false, 0, positionBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mPositionHandlers[1]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Set the input texture
        int textureHandle = mTransitionTarget.getTextureHandle();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLESUtil.glesCheckError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
        GLESUtil.glesCheckError("glBindTexture");
        GLES20.glUniform1i(mTextureHandlers[1], 0);
        GLESUtil.glesCheckError("glUniform1i");

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtil.glesCheckError("glDrawElements");

        // Disable attributes
        GLES20.glDisableVertexAttribArray(mPositionHandlers[1]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mTextureCoordHandlers[1]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
    }

    private void setInternalVertex() {
        float[] originalVertex = mTarget.getFrameVertex();
        System.arraycopy(originalVertex, 0, mVertex, 0, originalVertex.length);
    }

    private float getAmount() {
        return ((mTarget.getFrameWidth() * SCALE_AMOUNT) / 2);
    }
}
