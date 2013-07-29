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

package org.cyanogenmod.wallpapers.photophase.transitions;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLException;

import org.cyanogenmod.wallpapers.photophase.GLESUtil;
import org.cyanogenmod.wallpapers.photophase.PhotoFrame;
import org.cyanogenmod.wallpapers.photophase.R;
import org.cyanogenmod.wallpapers.photophase.TextureManager;
import org.cyanogenmod.wallpapers.photophase.transitions.Transitions.TRANSITIONS;

import java.nio.FloatBuffer;
/**
 * A special transition that does nothing other than draw the {@link PhotoFrame}
 * on the screen continually. No transition is done.
 */
public class NullTransition extends Transition {

    private static final int[] VERTEX_SHADER = {R.raw.default_vertex_shader};
    private static final int[] FRAGMENT_SHADER = {R.raw.default_fragment_shader};

    /**
     * Constructor of <code>NullTransition</code>
     *
     * @param ctx The current context
     * @param tm The texture manager
     */
    public NullTransition(Context ctx, TextureManager tm) {
        super(ctx, tm, VERTEX_SHADER, FRAGMENT_SHADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void select(PhotoFrame target) {
        super.select(target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TRANSITIONS getType() {
        return TRANSITIONS.NO_TRANSITION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasTransitionTarget() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunning() {
        return mTarget == null || !mTarget.isLoaded();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSelectable(PhotoFrame frame) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void apply(float[] matrix) throws GLException {
        // Check internal vars
        if (mTarget == null ||
            mTarget.getPictureVertexBuffer() == null ||
            mTarget.getTextureBuffer() == null ||
            mTarget.getVertexOrderBuffer() == null) {
            return;
        }

        // Draw the current target
        draw(mTarget, matrix);
    }

    /**
     * Method that draws the picture texture
     *
     * @param target
     * @param matrix The model-view-projection matrix
     */
    protected void draw(PhotoFrame target, float[] matrix) {
        // Set the program
        useProgram(0);

        // Bind the texture
        int textureHandle = target.getTextureHandle();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLESUtil.glesCheckError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
        GLESUtil.glesCheckError("glBindTexture");

        // Position
        FloatBuffer vertexBuffer = target.getPictureVertexBuffer();
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(
                mPositionHandlers[0],
                PhotoFrame.COORDS_PER_VERTER,
                GLES20.GL_FLOAT,
                false,
                PhotoFrame.COORDS_PER_VERTER * 4,
                vertexBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mPositionHandlers[0]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Texture
        FloatBuffer textureBuffer = target.getTextureBuffer();
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(
                mTextureCoordHandlers[0],
                2,
                GLES20.GL_FLOAT,
                false,
                2 * 4,
                textureBuffer);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandlers[0]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandlers[0], 1, false, matrix, 0);
        GLESUtil.glesCheckError("glUniformMatrix4fv");

        // Draw the photo frame
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        GLESUtil.glesCheckError("glDrawElements");

        // Disable attributes
        GLES20.glDisableVertexAttribArray(mPositionHandlers[0]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mTextureCoordHandlers[0]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
    }

}
