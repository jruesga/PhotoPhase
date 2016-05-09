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
// Based on the shaders of kodemongki:
//   http://kodemongki.blogspot.com.es/2011/06/kameraku-custom-shader-effects-example.html
//

package com.ruesga.android.wallpapers.photophase.effects;

import android.media.effect.EffectContext;
import android.opengl.GLES20;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;

/**
 * A noise effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class NoiseEffect extends PhotoPhaseEffect {

    private static final String TAG = "NoiseEffect";

    public static final String STRENGTH_PARAMETER = "strength";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "uniform float strength;\n" +
            "void main(void)\n" +
            "{\n" +
            "  vec2 uv = v_texcoord;\n" +
            "  vec3 c1 = texture2D(tex_sampler, vec2(uv.s - strength, uv.t - strength)).bgr;\n" +
            "  vec3 c2 = texture2D(tex_sampler, vec2(uv.s + strength, uv.t + strength)).bgr;\n" +
            "  vec3 c3 = texture2D(tex_sampler, vec2(uv.s - strength, uv.t + strength)).bgr;\n" +
            "  vec3 c4 = texture2D(tex_sampler, vec2(uv.s + strength, uv.t - strength)).bgr;\n" +
            "  gl_FragColor.a = 1.0;\n" +
            "  gl_FragColor.rgb = (c1 + c2 + c3 + c4) / 4.0;\n" +
            "}";

    private float mStrength = 0.02f;
    private final int mStrengthHandle;

    /**
     * Constructor of <code>NoiseEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public NoiseEffect(EffectContext ctx, String name) {
        super(ctx, NoiseEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);

        // Parameters
        mStrengthHandle = GLES20.glGetUniformLocation(mProgram[0], "strength");
        GLESUtil.glesCheckError("glGetUniformLocation");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void applyParameters(int width, int height) {
        // Set parameters
        GLES20.glUniform1f(mStrengthHandle, mStrength);
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
