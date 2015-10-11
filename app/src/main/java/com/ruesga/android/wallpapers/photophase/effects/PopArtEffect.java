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

/**
 * A pop art (Warhol) effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class PopArtEffect extends PhotoPhaseEffect {

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main(void)\n" +
            "{\n" +
            "    vec3 col = texture2D(tex_sampler, v_texcoord).bgr;\n" +
            "    float y = 0.3 *col.r + 0.59 * col.g + 0.11 * col.b;\n" +
            "    y = y < 0.3 ? 0.0 : (y < 0.6 ? 0.5 : 1.0);\n" +
            "    if (y == 0.5)\n" +
            "        col = vec3(0.8, 0.0, 0.0);\n" +
            "    else if (y == 1.0)\n" +
            "        col = vec3(0.9, 0.9, 0.0);\n" +
            "    else\n" +
            "        col = vec3(0.0, 0.0, 0.0);\n" +
            "    gl_FragColor.a = 1.0;\n" +
            "    gl_FragColor.rgb = col;\n" +
            "}";

    /**
     * Constructor of <code>PopArtEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public PopArtEffect(EffectContext ctx, String name) {
        super(ctx, PopArtEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);
    }

}
