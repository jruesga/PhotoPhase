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

/**
 * A crosshatching effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class CrosshatchingEffect extends PhotoPhaseEffect {

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() \n" +
            "{\n" +
            "  float hatch_y_offset = 5.0;\n" +
            "  float lum_threshold_1 = 1.0;\n" +
            "  float lum_threshold_2 = 0.7;\n" +
            "  float lum_threshold_3 =0.5;\n" +
            "  float lum_threshold_4 = 0.3;\n" +
            "  vec2 uv = v_texcoord;\n" +
            "  float lum = length(texture2D(tex_sampler, uv).rgb);\n" +
            "  vec3 tc = vec3(1.0, 1.0, 1.0);\n" +
            "  if (lum < lum_threshold_1) \n" +
            "  {\n" +
            "    if (mod(gl_FragCoord.x + gl_FragCoord.y, 10.0) == 0.0) \n" +
            "      tc = vec3(0.0, 0.0, 0.0);\n" +
            "  }  \n" +
            "  if (lum < lum_threshold_2) \n" +
            "  {\n" +
            "    if (mod(gl_FragCoord.x - gl_FragCoord.y, 10.0) == 0.0) \n" +
            "      tc = vec3(0.0, 0.0, 0.0);\n" +
            "  }  \n" +
            "  if (lum < lum_threshold_3) \n" +
            "  {\n" +
            "    if (mod(gl_FragCoord.x + gl_FragCoord.y - hatch_y_offset, 10.0) == 0.0) \n" +
            "      tc = vec3(0.0, 0.0, 0.0);\n" +
            "  }  \n" +
            "  if (lum < lum_threshold_4) \n" +
            "  {\n" +
            "    if (mod(gl_FragCoord.x - gl_FragCoord.y - hatch_y_offset, 10.0) == 0.0) \n" +
            "      tc = vec3(0.0, 0.0, 0.0);\n" +
            "  }\n" +
            "  gl_FragColor = vec4(tc, 1.0);\n" +
            "}";

    /**
     * Constructor of <code>CrossHatchingEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public CrosshatchingEffect(EffectContext ctx, String name) {
        super(ctx, CrosshatchingEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);
    }

}
