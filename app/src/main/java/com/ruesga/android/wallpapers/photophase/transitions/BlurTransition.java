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

import com.ruesga.android.wallpapers.photophase.PhotoFrame;
import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.textures.TextureManager;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions.TRANSITIONS;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;

import java.nio.FloatBuffer;

/**
 * A transition that applies a fade transition to the picture.
 */
public class BlurTransition extends Transition {

    private static final float TRANSITION_TIME = 1500.0f;

    private static final int[] VERTEX_SHADER = {R.raw.blur_vertex_shader};
    private static final int[] FRAGMENT_SHADER = {R.raw.blur_fragment_shader};

    private static final float MAX_BLUR_STRENGTH = 48.0f;

    private int mStrengthHandle;

    public BlurTransition(Context ctx, TextureManager tm) {
        super(ctx, tm, VERTEX_SHADER, FRAGMENT_SHADER);

        mStrengthHandle = GLES20.glGetUniformLocation(mProgramHandlers[0], "strength");
        GLESUtil.glesCheckError("glGetUniformLocation");
    }

    @Override
    public TRANSITIONS getType() {
        return TRANSITIONS.BLUR;
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
    public void applyTransition(float delta, float[] matrix, float offset) {
        if (delta <= 0.5) {
            // Draw the src target
            float strength = delta * 2.0f;
            draw(mTarget, matrix, MAX_BLUR_STRENGTH * strength);
        } else {
            // Draw the dst target
            float strength = (1 - delta) * 2.0f;
            draw(mTransitionTarget, matrix, MAX_BLUR_STRENGTH * strength);
        }
    }

    private void draw(PhotoFrame target, float[] matrix, float strength) {
        // Bind default FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLESUtil.glesCheckError("glBindFramebuffer");

        // Use our shader program
        useProgram(0);

        // Disable blending
        GLES20.glDisable(GLES20.GL_BLEND);
        GLESUtil.glesCheckError("glDisable");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandlers[0], 1, false, matrix, 0);
        GLESUtil.glesCheckError("glUniformMatrix4fv");

        // Strength
        GLES20.glUniform1f(mStrengthHandle, strength);
        GLESUtil.glesCheckError("glUniform1f");

        // Texture
        FloatBuffer textureBuffer = target.getTextureBuffer();
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandlers[0], 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandlers[0]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Position
        FloatBuffer positionBuffer = target.getPositionBuffer();
        positionBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandlers[0], 2, GLES20.GL_FLOAT, false, 0, positionBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mPositionHandlers[0]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Set the input texture
        int textureHandle = target.getTextureHandle();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLESUtil.glesCheckError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
        GLESUtil.glesCheckError("glBindTexture");
        GLES20.glUniform1i(mTextureHandlers[0], 0);
        GLESUtil.glesCheckError("glUniform1i");

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtil.glesCheckError("glDrawElements");

        // Disable attributes
        GLES20.glDisableVertexAttribArray(mPositionHandlers[0]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mTextureCoordHandlers[0]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
    }

    @Override
    public void recycle() {
        super.recycle();
        mStrengthHandle = -1;
    }
}
