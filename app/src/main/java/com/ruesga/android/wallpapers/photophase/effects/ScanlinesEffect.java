/*
 * Copyright (C) 2015 Jorge Ruesga
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
// Based on the shaders of Max Maischein of App-VideoMixer:
//   http://cpansearch.perl.org/src/CORION/App-VideoMixer-0.02/filters/scanlines.glsl
//

package com.ruesga.android.wallpapers.photophase.effects;

import android.media.effect.EffectContext;
import android.opengl.GLES20;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;

/**
 * A TV scanline effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class ScanlinesEffect extends PhotoPhaseEffect {

    private static final String TAG = "ScanlinesEffect";

    public static final String STRENGTH_PARAMETER = "strength";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "uniform float offset;\n" +
            "uniform float frequency;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main(void)\n" +
            "{\n" +
            "    float global_pos = (v_texcoord.y + offset) * frequency;\n" +
            "    float wave_pos = cos((fract(global_pos) - 0.5)*3.14);\n" +
            "    vec4 pel = texture2D(tex_sampler, v_texcoord);\n" +
            "    gl_FragColor = mix(vec4(0,0,0,0), pel, wave_pos);\n" +
            "}";

    private final int mFrequencyHandle;
    private final int mOffsetHandle;

    private float mStrength = 6f;

    /**
     * Constructor of <code>ScanlinesEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public ScanlinesEffect(EffectContext ctx, String name) {
        super(ctx, ScanlinesEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);

        // Parameters
        mFrequencyHandle = GLES20.glGetUniformLocation(mProgram[0], "frequency");
        GLESUtil.glesCheckError("glGetUniformLocation");
        mOffsetHandle = GLES20.glGetUniformLocation(mProgram[0], "offset");
        GLESUtil.glesCheckError("glGetUniformLocation");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void applyParameters(int width, int height) {
        float mFrequency = height / mStrength;
        float mOffset = 0f;

        // Set parameters
        GLES20.glUniform1f(mFrequencyHandle, mFrequency);
        GLESUtil.glesCheckError("glUniform1f");
        GLES20.glUniform1f(mOffsetHandle, mOffset);
        GLESUtil.glesCheckError("glUniform1f");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParameter(String parameterKey, Object value) {
        if (parameterKey.compareTo(STRENGTH_PARAMETER) == 0) {
            try {
                float strength = Float.parseFloat(value.toString());
                if (strength < 0) {
                    Log.w(TAG, "strength parameter must be > 0");
                    return;
                }
                mStrength = strength;
            } catch (NumberFormatException ex) {
                // Ignore
            }
        }
    }
}
