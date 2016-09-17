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

import java.nio.FloatBuffer;

/**
 * A transition that applies a translation transition to the picture.
 */
public class FlipTransition extends Transition {

    /**
     * The enumeration of all possibles translations movements
     */
    public enum FLIP_MODES {
        /**
         * Flip the picture horizontally
         */
        HORIZONTAL,
        /**
         * Flip the picture vertically
         */
        VERTICAL
    }

    private static final int[] VERTEX_SHADER = {R.raw.default_vertex_shader, R.raw.default_vertex_shader};
    private static final int[] FRAGMENT_SHADER = {R.raw.default_fragment_shader, R.raw.default_fragment_shader};

    private static final float TRANSITION_TIME = 600.0f;

    private FLIP_MODES mMode;

    private final float[] mTranslationMatrix;

    public FlipTransition(Context ctx, TextureManager tm) {
        super(ctx, tm, VERTEX_SHADER, FRAGMENT_SHADER);

        // Initialized
        mTranslationMatrix = new float[16];
        reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TRANSITIONS getType() {
        return TRANSITIONS.FLIP;
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
        chooseMode();
    }

    @Override
    public void chooseMode() {
        FLIP_MODES[] modes = FLIP_MODES.values();
        int low = 0;
        int high = modes.length - 1;
        mMode = modes[Utils.getNextRandom(low, high)];
//        mMode = FLIP_MODES.VERTICAL;
    }

    @Override
    public void applyTransition(float delta, float[] matrix, float offset) {
        applyTransition(delta, matrix, offset, delta <= 0.5 ? mTarget : mTransitionTarget);
    }

    private void applyTransition(float delta, float[] matrix, float offset, PhotoFrame target) {
        // Retrieve the index of the structures
        int index = delta <= 0.5f ? 0 : 1;

        // Bind default FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLESUtil.glesCheckError("glBindFramebuffer");

        // Set the program
        useProgram(index);

        // Disable blending
        GLES20.glDisable(GLES20.GL_BLEND);
        GLESUtil.glesCheckError("glDisable");

        // Set the input texture
        int textureHandle = target.getTextureHandle();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLESUtil.glesCheckError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
        GLESUtil.glesCheckError("glBindTexture");
        GLES20.glUniform1i(mTextureHandlers[index], 0);
        GLESUtil.glesCheckError("glBindTexture");

        // Texture
        FloatBuffer textureBuffer = target.getTextureBuffer();
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandlers[index], 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandlers[index]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Position
        FloatBuffer positionBuffer = target.getPositionBuffer();
        positionBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandlers[index], 2, GLES20.GL_FLOAT, false, 0, positionBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mPositionHandlers[index]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Calculate the delta angle and the translation and rotate parameters
        float angle = (delta * 90) / 0.5f;
        if (index == 1) {
            angle = 90 - ((delta - 0.5f) * 90) / 0.5f;
        }
        float translateX = 0.0f;
        float translateY = 0.0f;
        float rotateX = 0.0f;
        float rotateY = 0.0f;
        switch (mMode) {
            case HORIZONTAL:
                rotateY = -1.0f;
                translateX = (mTarget.getFrameVertex()[2] -
                        ((mTarget.getFrameVertex()[2] - mTarget.getFrameVertex()[0]) / 2)) * -1;
                break;
            case VERTICAL:
                rotateX = -1.0f;
                translateY = (mTarget.getFrameVertex()[5] -
                        ((mTarget.getFrameVertex()[5] - mTarget.getFrameVertex()[1]) / 2)) * -1;
                break;

            default:
                break;
        }

        // Apply the projection and view transformation
        Matrix.setIdentityM(matrix, 0);
        if (offset != 0.0f) {
            Matrix.translateM(matrix, 0, offset, 0.0f, 0.0f);
        }
        Matrix.translateM(mTranslationMatrix, 0, matrix, 0, -translateX, -translateY, 0.0f);
        Matrix.rotateM(mTranslationMatrix, 0, mTranslationMatrix, 0, angle, rotateX, rotateY, 0.0f);
        Matrix.translateM(mTranslationMatrix, 0, mTranslationMatrix, 0, translateX, translateY, 0.0f);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandlers[index], 1, false, mTranslationMatrix, 0);
        GLESUtil.glesCheckError("glUniformMatrix4fv");

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtil.glesCheckError("glDrawElements");

        // Disable attributes
        GLES20.glDisableVertexAttribArray(mPositionHandlers[index]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mTextureCoordHandlers[index]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
    }

}
