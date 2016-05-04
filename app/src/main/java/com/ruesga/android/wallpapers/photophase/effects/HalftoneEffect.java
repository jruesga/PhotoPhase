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
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "const float step_w = 0.0015625;\n" +
            "const float step_h = 0.0027778;\n" +
            "uniform float strength;\n" +
            "void main(void)\n" +
            "{\n" +
            "    float offx = floor(v_texcoord.s  / (strength * step_w));\n" +
            "    float offy = floor(v_texcoord.t  / (strength * step_h));\n" +
            "    vec3 res = texture2D(tex_sampler, vec2(offx * strength * step_w , offy * strength * step_h)).bgr;\n" +
            "    vec2 prc = fract(v_texcoord.st / vec2(strength * step_w, strength * step_h));\n" +
            "    vec2 pw = pow(abs(prc - 0.5), vec2(2.0));\n" +
            "    float  rs = pow(0.45, 2.0);\n" +
            "    float gr = smoothstep(rs - 0.1, rs + 0.1, pw.x + pw.y);\n" +
            "    float y = (res.r + res.g + res.b) / 3.0; \n" +
            "    vec3 ra = res / y;\n" +
            "    float ls = 0.3;\n" +
            "    float lb = ceil(y / ls);\n" +
            "    float lf = ls * lb + 0.3;\n" +
            "    res = lf * res;\n" +
            "    gl_FragColor.a = 1.0;\n" +
            "    gl_FragColor.rgb = mix(res, vec3(0.1, 0.1, 0.1), gr);\n" +
            "}";

    private float mStrength = 16.0f;
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
