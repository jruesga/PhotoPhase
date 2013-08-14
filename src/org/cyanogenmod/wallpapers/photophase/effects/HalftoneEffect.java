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
import android.media.effect.EffectFactory;
import android.opengl.GLES20;
import android.util.Log;

import org.cyanogenmod.wallpapers.photophase.utils.GLESUtil;

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

    private static final String STRENGTH_PARAMETER = "strength";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "uniform float steps\n;" +
            "float dotsize = 1.0 / steps ;\n" +
            "float half_step = dotsize / 2.0;\n" +
            "void main() {\n" +
            "    vec2 center = v_texcoord - vec2(mod(v_texcoord.x, dotsize),mod(v_texcoord.y, dotsize)) + half_step;\n" +
            "    vec4 pel = texture2D(tex_sampler, center);\n" +
            "    float size = length(pel);\n" +
            "    if (distance(v_texcoord,center) <= dotsize*size/4.0) {\n" +
            "       gl_FragColor = pel;\n" +
            "    } else {\n" +
            "       gl_FragColor = vec4(0.0,0.0,0.0,0.0);\n" +
            "    };\n" +
            "}\n";

    private float mStrength = 32.0f;
    private int mStepsHandle;

    /**
     * An abstract constructor of <code>Effect</code> to follow the rules
     * defined by {@link EffectFactory}.
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
        mStepsHandle = GLES20.glGetUniformLocation(mProgram, "steps");
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
