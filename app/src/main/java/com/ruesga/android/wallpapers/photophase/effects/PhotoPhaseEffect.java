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

package com.ruesga.android.wallpapers.photophase.effects;

import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * An abstract class definition for all the PhotoPhase custom effects
 */
public abstract class PhotoPhaseEffect extends Effect {

    private static final int FLOAT_SIZE_BYTES = 4;

    private static final String MCA_IDENTITY_EFFECT = "IdentityEffect";

    static final String VERTEX_SHADER =
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            "  gl_Position = vec4(a_position.xy, 0.0, 1.0);\n" +
            "  gl_Position = sign(gl_Position);\n" +
            "  v_texcoord = a_texcoord;\n" +
            "}\n";

    private static final float[] TEX_VERTICES = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] POS_VERTICES = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};

    private final int GL_STATE_FBO          = 0;
    private final int GL_STATE_PROGRAM      = 1;
    private final int GL_STATE_ARRAYBUFFER  = 2;
    private final int GL_STATE_COUNT        = GL_STATE_ARRAYBUFFER + 1;

    private final int[] mOldState = new int[GL_STATE_COUNT];

    private final EffectContext mEffectContext;
    private final String mName;

    private final Effect mIdentityEffect;

    protected int[] mProgram;
    private int[] mTexSamplerHandle;
    private int[] mTexCoordHandle;
    private int[] mPosCoordHandle;

    private FloatBuffer[] mTexVertices;
    private FloatBuffer[] mPosVertices;

    /**
     * An abstract constructor of <code>Effect</code> to follow the rules
     * defined by {@link EffectFactory}.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public PhotoPhaseEffect(EffectContext ctx, String name) {
        super();
        mEffectContext = ctx;
        mName = name;

        // Stand on MCA identity effect for the initialization work
        EffectFactory effectFactory = mEffectContext.getFactory();
        mIdentityEffect = effectFactory.createEffect(MCA_IDENTITY_EFFECT);
    }

    void init(String vertexShader, String fragmentShader) {
        init(new String[]{vertexShader}, new String[]{fragmentShader});
    }

    /**
     * Method that initializes the effect
     */
    void init(String[] vertexShaders, String fragmentShaders[]) {
        // Create program
        int count  = vertexShaders.length;
        mProgram = new int[count];
        mTexSamplerHandle = new int[count];
        mTexCoordHandle = new int[count];
        mPosCoordHandle = new int[count];
        mTexVertices = new FloatBuffer[count];
        mPosVertices = new FloatBuffer[count];

        for (int i = 0; i < count; i++) {
            mProgram[i] = GLESUtil.createProgram(vertexShaders[i], fragmentShaders[i]);

            // Bind attributes and uniforms
            mTexSamplerHandle[i] = GLES20.glGetUniformLocation(mProgram[i], "tex_sampler");
            GLESUtil.glesCheckError("glGetUniformLocation");
            mTexCoordHandle[i] = GLES20.glGetAttribLocation(mProgram[i], "a_texcoord");
            GLESUtil.glesCheckError("glGetAttribLocation");
            mPosCoordHandle[i] = GLES20.glGetAttribLocation(mProgram[i], "a_position");
            GLESUtil.glesCheckError("glGetAttribLocation");

            // Setup coordinate buffers
            mTexVertices[i] = ByteBuffer.allocateDirect(
                    TEX_VERTICES.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTexVertices[i].put(TEX_VERTICES).position(0);
            mPosVertices[i] = ByteBuffer.allocateDirect(
                    POS_VERTICES.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mPosVertices[i].put(POS_VERTICES).position(0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return mName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized void apply(int inputTexId, int width, int height, int outputTexId) {
        // Save the GLES state
        saveGLState();

        int[] fb = new int[1];
        try {
            // Create a framebuffer object and call the effect apply method to draw the effect
            GLES20.glGenFramebuffers(1, fb, 0);
            GLESUtil.glesCheckError("glGenFramebuffers");
            if (GLESUtil.DEBUG_GL_MEMOBJS) {
                Log.d(GLESUtil.DEBUG_GL_MEMOBJS_NEW_TAG, "glGenFramebuffers: " + fb[0]);
            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
            GLESUtil.glesCheckError("glBindFramebuffer");

            // Render on the whole framebuffer
            GLES20.glViewport(0, 0, width, height);
            GLESUtil.glesCheckError("glViewport");

            // Create a new output texture (Use the MCA identity to clone the input to the output)
            mIdentityEffect.apply(inputTexId, width, height, outputTexId);

            // Create the framebuffer
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20. GL_TEXTURE_2D, outputTexId, 0);
            GLESUtil.glesCheckError("glFramebufferTexture2D");

            // Check if the buffer was built successfully
            final int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                // Something was wrong. Throw an exception
                GLESUtil.glesCheckError("glCheckFramebufferStatus");
                int error = GLES20.glGetError();
                throw new android.opengl.GLException(error, GLUtils.getEGLErrorString(error));
            }

            // Apply the effect
            int count = mProgram.length;
            for (int i = 0; i < count; i++) {
                applyProgram(i, inputTexId, width, height);
            }

        } finally {
            // Restore the GLES state
            restoreGLState();

            // Clean framebuffer memory
            if (GLESUtil.DEBUG_GL_MEMOBJS) {
                Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteFramebuffers: " + fb[0]);
            }
            GLES20.glDeleteFramebuffers(1, fb, 0);
            GLESUtil.glesCheckError("glDeleteFramebuffers");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParameter(String parameterKey, Object value) {
        // Ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        for (int program : mProgram) {
            if (GLES20.glIsProgram(program)) {
                if (GLESUtil.DEBUG_GL_MEMOBJS) {
                    Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteProgram: " + program);
                }
                GLES20.glDeleteProgram(program);
                GLESUtil.glesCheckError("glDeleteProgram");
            }
        }
        mTexVertices = null;
        mPosVertices = null;
    }

    /**
     * Method that applies the effect.
     *
     * @param index The index of the program to apply
     * @param inputTexId The input texture
     */
    void applyProgram(int index, int inputTexId, int width, int height) {
        // Use our shader program
        GLES20.glUseProgram(mProgram[index]);
        GLESUtil.glesCheckError("glUseProgram");

        // Disable blending
        GLES20.glDisable(GLES20.GL_BLEND);
        GLESUtil.glesCheckError("glDisable");

        // Set the vertex attributes
        GLES20.glVertexAttribPointer(mTexCoordHandle[index], 2, GLES20.GL_FLOAT, false, 0, mTexVertices[index]);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mTexCoordHandle[index]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");
        GLES20.glVertexAttribPointer(mPosCoordHandle[index], 2, GLES20.GL_FLOAT, false, 0, mPosVertices[index]);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mPosCoordHandle[index]);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Set parameters
        applyParameters(width, height);

        // Set the input texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLESUtil.glesCheckError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexId);
        GLESUtil.glesCheckError("glBindTexture");
        GLES20.glUniform1i(mTexSamplerHandle[index], 0);
        GLESUtil.glesCheckError("glUniform1i");

        // Draw
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLESUtil.glesCheckError("glClearColor");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLESUtil.glesCheckError("glClear");
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtil.glesCheckError("glDrawArrays");

        // Disable attributes
        GLES20.glDisableVertexAttribArray(mTexCoordHandle[index]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mPosCoordHandle[index]);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
    }

    /**
     * Method that applies the parameters of the effect.
     */
    void applyParameters(int width, int height) {
        // Do nothing
    }

    /**
     * Save GL state
     */
    private void saveGLState() {
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, mOldState, GL_STATE_FBO);
        GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM, mOldState, GL_STATE_PROGRAM);
        GLES20.glGetIntegerv(GLES20.GL_ARRAY_BUFFER_BINDING, mOldState, GL_STATE_ARRAYBUFFER);
    }

    /**
     * Restore GL state
     */
    private void restoreGLState() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mOldState[GL_STATE_FBO]);
        GLES20.glUseProgram(mOldState[GL_STATE_PROGRAM]);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mOldState[GL_STATE_ARRAYBUFFER]);
    }
}
