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
 * A fisheye effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class FisheyeEffect extends PhotoPhaseEffect {

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "const float PI = 3.1415926535;\n" +
            "void main()\n" +
            "{\n" +
            "  float aperture = 178.0;\n" +
            "  float apertureHalf = 0.5 * aperture * (PI / 180.0);\n" +
            "  float maxFactor = sin(apertureHalf);\n" +
            "  vec2 uv;\n" +
            "  vec2 xy = 2.0 * v_texcoord.xy - 1.0;\n" +
            "  float d = length(xy);\n" +
            "  if (d < (2.0-maxFactor))\n" +
            "  {\n" +
            "    d = length(xy * maxFactor);\n" +
            "    float z = sqrt(1.0 - d * d);\n" +
            "    float r = atan(d, z) / PI;\n" +
            "    float phi = atan(xy.y, xy.x);\n" +
            "    uv.x = r * cos(phi) + 0.5;\n" +
            "    uv.y = r * sin(phi) + 0.5;\n" +
            "  }\n" +
            "  else\n" +
            "  {\n" +
            "    uv = v_texcoord.xy;\n" +
            "  }\n" +
            "  vec4 c = texture2D(tex_sampler, uv);\n" +
            "  gl_FragColor = c;\n" +
            "}";

    /**
     * Constructor of <code>FisheyeEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public FisheyeEffect(EffectContext ctx, String name) {
        super(ctx, FisheyeEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);
    }

}
