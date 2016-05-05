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

    public static final String PIXEL_W_PARAMETER = "pixel_w";
    public static final String PIXEL_H_PARAMETER = "pixel_h";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "uniform float w;\n" +
            "uniform float h;\n" +
            "uniform float pixel_w; //15.0;\n" +
            "uniform float pixel_h; //10.0;\n" +
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

    private float mPixelWidth = 15.0f;
    private float mPixelHeight = 10.0f;

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
        GLES20.glUniform1f(mPixelWidthHandle, mPixelWidth);
        GLESUtil.glesCheckError("glUniform1f");
        GLES20.glUniform1f(mPixelHeightHandle, mPixelHeight);
        GLESUtil.glesCheckError("glUniform1f");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setParameter(String parameterKey, Object value) {
        if (parameterKey.compareTo(PIXEL_W_PARAMETER) == 0) {
            try {
                float p = Float.parseFloat(value.toString());
                if (p <= 0) {
                    Log.w(TAG, "pixel width parameter must be >= 0");
                    return;
                }
                mPixelWidth = p;
            } catch (NumberFormatException ex) {
                // Ignore
            }
        } else if (parameterKey.compareTo(PIXEL_H_PARAMETER) == 0) {
            try {
                float p = Float.parseFloat(value.toString());
                if (p <= 0) {
                    Log.w(TAG, "pixel height parameter must be >= 0");
                    return;
                }
                mPixelHeight = p;
            } catch (NumberFormatException ex) {
                // Ignore
            }
        }
    }
}
