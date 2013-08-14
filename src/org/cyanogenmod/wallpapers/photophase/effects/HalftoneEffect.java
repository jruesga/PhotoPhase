/*
 * Copyright (C) 2008 Max Maischein
 * Copyright (C) 2013 The CyanogenMod Project
 *
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
//
// Based in the shaders of Max Maischein of App-VideoMixer:
//   http://cpansearch.perl.org/src/CORION/App-VideoMixer-0.02/filters/halftone.glsl
//

package org.cyanogenmod.wallpapers.photophase.effects;

import android.media.effect.EffectContext;
import android.opengl.GLES20;
import android.util.Log;

import org.cyanogenmod.wallpapers.photophase.utils.GLESUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * A halftone effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * <tr>
 *   <td><code>strength</code></td>
 *   <td>The halftone steps multiplier.</td>
 *   <td>Positive float (>0). Higher numbers produce smallest points;</td>
 * </tr>
 * </table>
 */
public class HalftoneEffect extends PhotoPhaseEffect {

    private static final String TAG = "HalftoneEffect";

    /**
     * The halftone steps multiplier parameter key
     */
    public static final String STRENGTH_PARAMETER = "strength";

    private static final int FLOAT_SIZE_BYTES = 4;

    private static final String VERTEX_SHADER =
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            "  gl_Position = vec4(a_position.xy, 0.0, 1.0);\n" +
            "  gl_Position = sign(gl_Position);\n" +
            "  v_texcoord = a_texcoord;\n" +
            "}\n";
    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "uniform float steps\n;" +
            "float dotsize = 1.0 / steps ;\n" +
            "float half_step = dotsize / 2.0;\n" +
            "void main() {\n" +
            "    vec2 center = v_texcoord - vec2(mod(v_texcoord.x, dotsize),mod(v_texcoord.y, dotsize)) + half_step;\n" +
            "    vec4 pel = texture2D( tex_sampler, center);\n" +
            "    float size = length(pel);\n" +
            "    if (distance(v_texcoord,center) <= dotsize*size/4.0) {\n" +
            "       gl_FragColor = pel;\n" +
            "    } else {\n" +
            "       gl_FragColor = vec4(0.0,0.0,0.0,0.0);\n" +
            "    };\n" +
            "}\n";

    private static final float[] TEX_VERTICES = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] POS_VERTICES = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};

    private float mStrength = 32.0f;

    private int mProgram;
    private int mTexSamplerHandle;
    private int mTexCoordHandle;
    private int mPosCoordHandle;
    private int mStepsHandle;

    private FloatBuffer mTexVertices;
    private FloatBuffer mPosVertices;

    /**
     * An abstract contructor of <code>Effect</code> to follow the rules
     * defined by {@see EffectFactory}.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public HalftoneEffect(EffectContext ctx, String name) {
        super(ctx, HalftoneEffect.class.getName());
        init();
    }

    /**
     * Method that initializes the effect
     */
    private void init() {
        // Create program
        mProgram = GLESUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);

        // Bind attributes and uniforms
        mTexSamplerHandle = GLES20.glGetUniformLocation(mProgram, "tex_sampler");
        GLESUtil.glesCheckError("glGetUniformLocation");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texcoord");
        GLESUtil.glesCheckError("glGetAttribLocation");
        mPosCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_position");
        GLESUtil.glesCheckError("glGetAttribLocation");
        mStepsHandle = GLES20.glGetUniformLocation(mProgram, "steps");
        GLESUtil.glesCheckError("glGetUniformLocation");

        // Setup coordinate buffers
        mTexVertices = ByteBuffer.allocateDirect(
                TEX_VERTICES.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexVertices.put(TEX_VERTICES).position(0);
        mPosVertices = ByteBuffer.allocateDirect(
                POS_VERTICES.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mPosVertices.put(POS_VERTICES).position(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void apply(int inputTexId) {
        // Use our shader program
        GLES20.glUseProgram(mProgram);
        GLESUtil.glesCheckError("glUseProgram");

        // Disable blending
        GLES20.glDisable(GLES20.GL_BLEND);
        GLESUtil.glesCheckError("glDisable");

        // Set the vertex attributes
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexVertices);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");
        GLES20.glVertexAttribPointer(mPosCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mPosVertices);
        GLESUtil.glesCheckError("glVertexAttribPointer");
        GLES20.glEnableVertexAttribArray(mPosCoordHandle);
        GLESUtil.glesCheckError("glEnableVertexAttribArray");

        // Set parameters
        GLES20.glUniform1f(mStepsHandle, mStrength);
        GLESUtil.glesCheckError("glUniform1f");

        // Set the input texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexId);
        GLESUtil.glesCheckError("glBindTexture");
        GLES20.glUniform1i(mTexSamplerHandle, 0);
        GLESUtil.glesCheckError("glUniform1i");

        // Draw
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLESUtil.glesCheckError("glClearColor");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLESUtil.glesCheckError("glClear");
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtil.glesCheckError("glDrawArrays");

        // Disable attributes
        GLES20.glDisableVertexAttribArray(mTexCoordHandle);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mPosCoordHandle);
        GLESUtil.glesCheckError("glDisableVertexAttribArray");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParameter(String parameterKey, Object value) {
        if (parameterKey.compareTo(STRENGTH_PARAMETER) == 0) {
            try {
                float strength = Float.parseFloat(value.toString());
                if (strength <= 0) {
                    Log.w(TAG, "strength parameter must be >= 0");
                    return;
                }
                mStrength = strength;
            } catch (NumberFormatException ex) {
                // Ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        if (GLES20.glIsProgram(mProgram)) {
            GLES20.glDeleteProgram(mProgram);
            GLESUtil.glesCheckError("glDeleteProgram");
        }
        mTexVertices = null;
        mPosVertices = null;
    }

}
