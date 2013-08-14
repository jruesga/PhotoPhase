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

package org.cyanogenmod.wallpapers.photophase.effects;

import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import org.cyanogenmod.wallpapers.photophase.utils.GLESUtil;

import java.nio.IntBuffer;

/**
 * An abstract class definition for all the PhotoPhase custom effects
 */
public abstract class PhotoPhaseEffect extends Effect {

    private final EffectContext mEffectContext;
    private final String mName;

    /**
     * An abstract contructor of <code>Effect</code> to follow the rules
     * defined by {@see EffectFactory}.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public PhotoPhaseEffect(EffectContext ctx, String name) {
        super();
        mEffectContext = ctx;
        mName = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return mName;
    }

    /**
     * Method that returns the effect context
     *
     * @return EffectContext The effect context
     */
    public EffectContext getEffectContext() {
        return mEffectContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void apply(int inputTexId, int width, int height, int outputTexId) {
        // Create a framebuffer object and call the effect apply method to draw the effect
        int[] fb = new int[1];
        GLES20.glGenFramebuffers(1, fb, 0);
        GLESUtil.glesCheckError("glGenFramebuffers");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
        GLESUtil.glesCheckError("glBindFramebuffer");

        // Render on the whole framebuffer,
        GLES20.glViewport(0, 0, width, height);
        GLESUtil.glesCheckError("glViewport");

        // Create a new output texturure
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, outputTexId);
        GLESUtil.glesCheckError("glBindTexture");
        IntBuffer texBuffer = IntBuffer.wrap(new int[width * height]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, width, height, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, texBuffer);
        GLESUtil.glesCheckError("glTexImage2D");

        // Set the
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLESUtil.glesCheckError("glTexParameteri");
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLESUtil.glesCheckError("glTexParameteri");

        // Create the framebuffer
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20. GL_TEXTURE_2D, outputTexId, 0);
        GLESUtil.glesCheckError("glFramebufferTexture2D");

        // Check if the buffer was built successfully
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            // Something when wrong. Throw an exception
            GLESUtil.glesCheckError("glCheckFramebufferStatus");
            int error = GLES20.glGetError();
            throw new android.opengl.GLException(error, GLUtils.getEGLErrorString(error));
        }

        // Bind the framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);

        // Apply the effect
        apply(inputTexId);

        // Unbind the framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    /**
     * Method that applies the effect.
     *
     *  @param inputTexId The input texture
     */
    abstract void apply(int inputTexId);
}
