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
// Based on the shaders library of Geeks3d:
//   http://www.geeks3d.com/shader-library
//

package com.ruesga.android.wallpapers.photophase.effects;

import android.media.effect.EffectContext;
import android.opengl.GLES20;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;

/**
 * A crosshatching effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class FrostedEffect extends PhotoPhaseEffect {

    private static final String TAG = "FrostedEffect";

    public static final String STRENGTH_PARAMETER = "strength";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "uniform float strength;\n" +
            "varying vec2 v_texcoord;\n" +
            "const float rnd_scale = 5.1;\n" +
            "const vec2 v1 = vec2(92.,80.);\n" +
            "const vec2 v2 = vec2(41.,62.);\n" +
            "float rand(vec2 co)\n" +
            "{\n" +
            "  return fract(sin(dot(co.xy ,v1)) + cos(dot(co.xy ,v2)) * rnd_scale);\n" +
            "}\n" +
            "void main() \n" +
            "{\n" +
            "  vec2 uv = v_texcoord;\n" +
            "  vec2 rnd = vec2(rand(uv.xy),rand(uv.yx));  \n" +
            "  vec3 tc = texture2D(tex_sampler, uv+rnd*strength).rgb;  \n" +
            "  gl_FragColor = vec4(tc, 1.0);\n" +
            "}";

    private float mStrength = 0.10f;
    private final int mStrengthHandle;

    /**
     * Constructor of <code>CrossHatchingEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public FrostedEffect(EffectContext ctx, String name) {
        super(ctx, FrostedEffect.class.getName());
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
