/*
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
// Based on the shaders of kodemongki:
//   http://kodemongki.blogspot.com.es/2011/06/kameraku-custom-shader-effects-example.html
//

package org.cyanogenmod.wallpapers.photophase.effects;

import android.media.effect.EffectContext;
import android.opengl.GLES20;
import android.util.Log;

import org.cyanogenmod.wallpapers.photophase.utils.GLESUtil;

/**
 * A pixelate effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * <tr>
 *   <td><code>strength</code></td>
 *   <td>The pixelate strength.</td>
 *   <td>Positive float (>0). Higher numbers produce more pixelation.</td>
 * </tr>
 * </table>
 */
public class PixelateEffect extends PhotoPhaseEffect {

    private static final String TAG = "PixelateEffect";

    private static final String STRENGTH_PARAMETER = "strength";

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
            "    gl_FragColor.a = 1.0;\n" +
            "    gl_FragColor.rgb = res;\n" +
            "}";

    private float mStrength = 8.0f;
    private int mStepsHandle;

    /**
     * Constructor of <code>PixelateEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public PixelateEffect(EffectContext ctx, String name) {
        super(ctx, PixelateEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void init(String vertexShader, String fragmentShader) {
        super.init(vertexShader, fragmentShader);

        // Parameters
        mStepsHandle = GLES20.glGetUniformLocation(mProgram, "strength");
        GLESUtil.glesCheckError("glGetUniformLocation");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void applyParameters() {
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
