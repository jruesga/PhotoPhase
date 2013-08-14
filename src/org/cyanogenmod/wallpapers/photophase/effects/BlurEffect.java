/*
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
// Based on the shaders of kodemongki:
//   http://kodemongki.blogspot.com.es/2011/06/kameraku-custom-shader-effects-example.html
//

package org.cyanogenmod.wallpapers.photophase.effects;

import android.media.effect.EffectContext;

/**
 * A blur effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class BlurEffect extends PhotoPhaseEffect {

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main(void)\n" +
            "{\n" +
            "    float step = 0.02;\n" +
            "    vec3 c1 = texture2D(tex_sampler, vec2(v_texcoord.s - step, v_texcoord.t - step)).bgr;\n" +
            "    vec3 c2 = texture2D(tex_sampler, vec2(v_texcoord.s + step, v_texcoord.t + step)).bgr;\n" +
            "    vec3 c3 = texture2D(tex_sampler, vec2(v_texcoord.s - step, v_texcoord.t + step)).bgr;\n" +
            "    vec3 c4 = texture2D(tex_sampler, vec2(v_texcoord.s + step, v_texcoord.t - step)).bgr;\n" +
            "    gl_FragColor.a = 1.0;\n" +
            "    gl_FragColor.rgb = (c1 + c2 + c3 + c4) / 4.0;\n" +
            "}";

    /**
     * Constructor of <code>BlurEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public BlurEffect(EffectContext ctx, String name) {
        super(ctx, BlurEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);
    }

}
