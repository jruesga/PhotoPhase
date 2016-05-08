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
// Based on:
//   http://www.derivative.ca/forum/viewtopic.php?f=27&t=4245
//

package com.ruesga.android.wallpapers.photophase.effects;

import android.media.effect.EffectContext;
import android.opengl.GLES20;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;

/**
 * A halftone effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * <tr>
 *   <td><code>strength</code></td>
 *   <td>The halftone strength.</td>
 *   <td>Positive float (>0). Higher numbers produce smallest points.</td>
 * </tr>
 * </table>
 */
public class HalftoneEffect extends PhotoPhaseEffect {

    private static final String TAG = "HalftoneEffect";

    public static final String STRENGTH_PARAMETER = "strength";

    private static final String FRAGMENT_SHADER =
            "#ifdef GL_OES_standard_derivatives\n" +
            "#extension GL_OES_standard_derivatives : enable\n" +
            "#endif\n" +
            "precision highp float;\n" +
            "uniform float strength;\n" +
            "const float uScale = 1.0;\n" +
            "const float uYrot = 0.0;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "float aastep(float threshold, float value) {\n" +
            "  float afwidth = strength * (1.0/200.0) / uScale / cos(uYrot);\n" +
            "  return smoothstep(threshold-afwidth, threshold+afwidth, value);\n" +
            "}\n" +
            "void main() {\n" +
            "    vec2 st2 = mat2(0.707, -0.707, 0.707, 0.707) * v_texcoord;\n" +
            "    vec2 nearest = 2.0*fract(strength * st2) - 1.0;\n" +
            "    float dist = length(nearest);\n" +
            "    // Use a texture to modulate the size of the dots\n" +
            "    vec3 texcolor = texture2D(tex_sampler, v_texcoord).rgb;\n" +
            "    float radius = sqrt(1.0-texcolor.g);\n" +
            "    vec3 white = vec3(1.0, 1.0, 1.0);\n" +
            "    vec3 black = vec3(0.0, 0.0, 0.0);\n" +
            "    vec3 fragcolor = mix(black, white, aastep(radius, dist));\n" +
            "    gl_FragColor = vec4(fragcolor, 1.0);\n" +
            "}";

    private float mStrength = 80.0f;
    private int mStepsHandle;

    /**
     * Constructor of <code>HalftoneEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public HalftoneEffect(EffectContext ctx, String name) {
        super(ctx, HalftoneEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void init(String vertexShader, String fragmentShader) {
        super.init(vertexShader, fragmentShader);

        // Parameters
        mStepsHandle = GLES20.glGetUniformLocation(mProgram[0], "strength");
        GLESUtil.glesCheckError("glGetUniformLocation");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void applyParameters(int width, int height) {
        // Set parameters
        GLES20.glUniform1f(mStepsHandle, mStrength);
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

}
