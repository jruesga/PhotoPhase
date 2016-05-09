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
public class PixelateEffect extends PhotoPhaseEffect {

    private static final String TAG = "PixelateEffect";

    public static final String STRENGTH_PARAMETER = "strength";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "uniform float w;\n" +
            "uniform float h;\n" +
            "uniform float pixel_w;\n" +
            "uniform float pixel_h;\n" +
            "void main() \n" +
            "{\n" +
            "    vec2 uv = v_texcoord;\n" +
            "    float dx = pixel_w*(1./w);\n" +
            "    float dy = pixel_h*(1./h);\n" +
            "    vec2 coord = vec2(dx*floor(uv.x/dx),\n" +
            "                      dy*floor(uv.y/dy));\n" +
            "    vec3 tc = texture2D(tex_sampler, coord).rgb;\n" +
            "    gl_FragColor = vec4(tc, 1.0);\n" +
            "}";

    private float mPixelW = 15.0f;
    private float mPixelH = 10.0f;

    private int mWidthHandle;
    private int mHeightHandle;
    private int mPixelWidthHandle;
    private int mPixelHeightHandle;

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
        mWidthHandle = GLES20.glGetUniformLocation(mProgram[0], "w");
        GLESUtil.glesCheckError("glGetUniformLocation");
        mHeightHandle = GLES20.glGetUniformLocation(mProgram[0], "h");
        GLESUtil.glesCheckError("glGetUniformLocation");
        mPixelWidthHandle = GLES20.glGetUniformLocation(mProgram[0], "pixel_w");
        GLESUtil.glesCheckError("glGetUniformLocation");
        mPixelHeightHandle = GLES20.glGetUniformLocation(mProgram[0], "pixel_h");
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
        GLES20.glUniform1f(mPixelWidthHandle, mPixelW);
        GLESUtil.glesCheckError("glUniform1f");
        GLES20.glUniform1f(mPixelHeightHandle, mPixelH);
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
                mPixelW = 15.0f * strength;
                mPixelH = 10.0f * strength;
            } catch (NumberFormatException ex) {
                // Ignore
            }
        }
    }
}
