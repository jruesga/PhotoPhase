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
 * An emboss effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class EmbossEffect extends PhotoPhaseEffect {

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "const float step_w = 0.0015625;\n" +
            "const float step_h = 0.0027778;\n" +
            "void main(void)\n" +
            "{\n" +
            "    vec3 t1 = texture2D(tex_sampler, vec2(v_texcoord.x - step_w, v_texcoord.y - step_h)).bgr;\n" +
            "    vec3 t2 = texture2D(tex_sampler, vec2(v_texcoord.x, v_texcoord.y - step_h)).bgr;\n" +
            "    vec3 t3 = texture2D(tex_sampler, vec2(v_texcoord.x + step_w, v_texcoord.y - step_h)).bgr;\n" +
            "    vec3 t4 = texture2D(tex_sampler, vec2(v_texcoord.x - step_w, v_texcoord.y)).bgr;\n" +
            "    vec3 t5 = texture2D(tex_sampler, v_texcoord).bgr;\n" +
            "    vec3 t6 = texture2D(tex_sampler, vec2(v_texcoord.x + step_w, v_texcoord.y)).bgr;\n" +
            "    vec3 t7 = texture2D(tex_sampler, vec2(v_texcoord.x - step_w, v_texcoord.y + step_h)).bgr;\n" +
            "    vec3 t8 = texture2D(tex_sampler, vec2(v_texcoord.x, v_texcoord.y + step_h)).bgr;\n" +
            "    vec3 t9 = texture2D(tex_sampler, vec2(v_texcoord.x + step_w, v_texcoord.y + step_h)).bgr;\n" +
            "    vec3 rr = -4.0 * t1 - 4.0 * t2 - 4.0 * t4 + 12.0 * t5;\n" +
            "    float y = (rr.r + rr.g + rr.b) / 3.0;\n" +
            "    gl_FragColor.a = 1.0;\n" +
            "    gl_FragColor.rgb = vec3(y, y, y) + 0.3;\n" +
            "}";

    /**
     * Constructor of <code>EmbossEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public EmbossEffect(EffectContext ctx, String name) {
        super(ctx, EmbossEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);
    }

}
