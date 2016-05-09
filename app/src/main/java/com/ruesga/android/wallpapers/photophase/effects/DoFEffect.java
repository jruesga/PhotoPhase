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
 * A Depth of field effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * <tr>
 *   <td><code>strength</code></td>
 *   <td>The dof strength.</td>
 *   <td>Positive float (>0). Higher numbers produce more dof.</td>
 * </tr>
 * </table>
 */
public class DoFEffect extends PhotoPhaseEffect {

    private static final String TAG = "DoFEffect";

    public static final String STRENGTH_PARAMETER = "strength";

    private static final String H_VERTEX_SHADER =
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "varying vec2 v_blurTexCoords[14];\n" +
            "uniform float strength;\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = a_position;\n" +
            "    v_texcoord = a_texcoord;\n" +
            "    v_blurTexCoords[ 0] = v_texcoord + vec2(-0.028 * strength, 0.0);\n" +
            "    v_blurTexCoords[ 1] = v_texcoord + vec2(-0.024 * strength, 0.0);\n" +
            "    v_blurTexCoords[ 2] = v_texcoord + vec2(-0.020 * strength, 0.0);\n" +
            "    v_blurTexCoords[ 3] = v_texcoord + vec2(-0.016 * strength, 0.0);\n" +
            "    v_blurTexCoords[ 4] = v_texcoord + vec2(-0.012 * strength, 0.0);\n" +
            "    v_blurTexCoords[ 5] = v_texcoord + vec2(-0.008 * strength, 0.0);\n" +
            "    v_blurTexCoords[ 6] = v_texcoord + vec2(-0.004 * strength, 0.0);\n" +
            "    v_blurTexCoords[ 7] = v_texcoord + vec2( 0.004 * strength, 0.0);\n" +
            "    v_blurTexCoords[ 8] = v_texcoord + vec2( 0.008 * strength, 0.0);\n" +
            "    v_blurTexCoords[ 9] = v_texcoord + vec2( 0.012 * strength, 0.0);\n" +
            "    v_blurTexCoords[10] = v_texcoord + vec2( 0.016 * strength, 0.0);\n" +
            "    v_blurTexCoords[11] = v_texcoord + vec2( 0.020 * strength, 0.0);\n" +
            "    v_blurTexCoords[12] = v_texcoord + vec2( 0.024 * strength, 0.0);\n" +
            "    v_blurTexCoords[13] = v_texcoord + vec2( 0.028 * strength, 0.0);\n" +
            "}\n";

    private static final String V_VERTEX_SHADER =
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "varying vec2 v_blurTexCoords[14];\n" +
            "uniform float strength;\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = a_position;\n" +
            "    v_texcoord = a_texcoord;\n" +
            "    v_blurTexCoords[ 0] = v_texcoord + vec2(0.0, -0.028 * strength);\n" +
            "    v_blurTexCoords[ 1] = v_texcoord + vec2(0.0, -0.024 * strength);\n" +
            "    v_blurTexCoords[ 2] = v_texcoord + vec2(0.0, -0.020 * strength);\n" +
            "    v_blurTexCoords[ 3] = v_texcoord + vec2(0.0, -0.016 * strength);\n" +
            "    v_blurTexCoords[ 4] = v_texcoord + vec2(0.0, -0.012 * strength);\n" +
            "    v_blurTexCoords[ 5] = v_texcoord + vec2(0.0, -0.008 * strength);\n" +
            "    v_blurTexCoords[ 6] = v_texcoord + vec2(0.0, -0.004 * strength);\n" +
            "    v_blurTexCoords[ 7] = v_texcoord + vec2(0.0,  0.004 * strength);\n" +
            "    v_blurTexCoords[ 8] = v_texcoord + vec2(0.0,  0.008 * strength);\n" +
            "    v_blurTexCoords[ 9] = v_texcoord + vec2(0.0,  0.012 * strength);\n" +
            "    v_blurTexCoords[10] = v_texcoord + vec2(0.0,  0.016 * strength);\n" +
            "    v_blurTexCoords[11] = v_texcoord + vec2(0.0,  0.020 * strength);\n" +
            "    v_blurTexCoords[12] = v_texcoord + vec2(0.0,  0.024 * strength);\n" +
            "    v_blurTexCoords[13] = v_texcoord + vec2(0.0,  0.028 * strength);\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "varying vec2 v_blurTexCoords[14];\n" +
            "const float cx = 0.5;\n" +
            "const float cy = 0.5;\n" +
            "const float r = 0.3;\n" +
            "bool focus(vec2 p, vec2 c, float r) {\n" +
            "  float dx = (c.x - p.x);\n" +
            "  float dy = (c.y - p.y);\n" +
            "  dx *= dx;\n" +
            "  dy *= dy;\n" +
            "  return (dx + dy) <= (r * r);\n" +
            "}\n" +
            "void main()\n" +
            "{\n" +
            "  vec2 uv = v_texcoord.xy;\n" +
            "  vec2 c = vec2(cx, cy);\n" +
            "  if (focus(uv, c, r)) {\n" +
            "    gl_FragColor = texture2D(tex_sampler, v_texcoord);\n" +
            "  } else {\n" +
            "    gl_FragColor = vec4(0.0);\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[ 0]) * 0.0044299121055113265;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[ 1]) * 0.00895781211794;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[ 2]) * 0.0215963866053;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[ 3]) * 0.0443683338718;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[ 4]) * 0.0776744219933;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[ 5]) * 0.115876621105;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[ 6]) * 0.147308056121;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_texcoord         ) * 0.159576912161;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[ 7]) * 0.147308056121;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[ 8]) * 0.115876621105;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[ 9]) * 0.0776744219933;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[10]) * 0.0443683338718;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[11]) * 0.0215963866053;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[12]) * 0.00895781211794;\n" +
            "    gl_FragColor += texture2D(tex_sampler, v_blurTexCoords[13]) * 0.0044299121055113265;\n" +
            "  }\n" +
            "}";

    private float mStrength = 1.0f;
    private final int mStrengthHandle;

    /**
     * Constructor of <code>BlurEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public DoFEffect(EffectContext ctx, String name) {
        super(ctx, DoFEffect.class.getName());
        init(new String[]{H_VERTEX_SHADER, V_VERTEX_SHADER},
                new String[]{FRAGMENT_SHADER, FRAGMENT_SHADER});

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
