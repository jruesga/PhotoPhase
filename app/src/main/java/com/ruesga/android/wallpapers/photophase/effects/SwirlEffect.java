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
 * A pixelate effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * <tr>
 *   <td><code>pixel_w</code></td>
 *   <td>The pixel width strength.</td>
 *   <td>Positive float (>0). Higher numbers produce more pixelation.</td>
 * </tr>
 * <tr>
 *   <td><code>pixel_h</code></td>
 *   <td>The pixel height strength.</td>
 *   <td>Positive float (>0). Higher numbers produce more pixelation.</td>
 * </tr>
 * </table>
 */
public class SwirlEffect extends PhotoPhaseEffect {

    private static final String TAG = "SwirlEffect";

    public static final String STRENGTH_PARAMETER = "strength";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "uniform float strength;\n" +
            "uniform float w;\n" +
            "uniform float h;\n" +
            "const float angle = 0.8;\n" +
            "void main (void)\n" +
            "{\n" +
            "  vec2 uv = v_texcoord;\n" +
            "  vec2 texSize = vec2(w, h);\n" +
            "  vec2 center = vec2(w/2.0, h/2.0);\n" +
            "  float radius = (min(w,h) / 2.0) / strength;\n" +
            "  vec2 tc = uv * texSize;\n" +
            "  tc -= center;\n" +
            "  float dist = length(tc);\n" +
            "  if (dist < radius) \n" +
            "  {\n" +
            "    float percent = (radius - dist) / radius;\n" +
            "    float theta = percent * percent * angle * 8.0;\n" +
            "    float s = sin(theta);\n" +
            "    float c = cos(theta);\n" +
            "    tc = vec2(dot(tc, vec2(c, -s)), dot(tc, vec2(s, c)));\n" +
            "  }\n" +
            "  tc += center;\n" +
            "  vec3 color = texture2D(tex_sampler, tc / texSize).rgb;\n" +
            "  gl_FragColor = vec4(color, 1.0);\n" +
            "}";

    private float mStrength = 1.5f;

    private final int mStrengthHandle;
    private final int mWidthHandle;
    private final int mHeightHandle;

    /**
     * Constructor of <code>PixelateEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public SwirlEffect(EffectContext ctx, String name) {
        super(ctx, SwirlEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);

        // Parameters
        mStrengthHandle = GLES20.glGetUniformLocation(mProgram[0], "strength");
        GLESUtil.glesCheckError("glGetUniformLocation");
        mWidthHandle = GLES20.glGetUniformLocation(mProgram[0], "w");
        GLESUtil.glesCheckError("glGetUniformLocation");
        mHeightHandle = GLES20.glGetUniformLocation(mProgram[0], "h");
        GLESUtil.glesCheckError("glGetUniformLocation");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void applyParameters(int width, int height) {
        // Set parameters
        GLES20.glUniform1f(mWidthHandle, (float) width);
        GLESUtil.glesCheckError("glUniform1f");
        GLES20.glUniform1f(mHeightHandle, (float) height);
        GLESUtil.glesCheckError("glUniform1f");
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
