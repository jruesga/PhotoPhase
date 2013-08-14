/*
 * Copyright (C) 2008 Max Maischein
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
// Based in the shaders of Max Maischein of App-VideoMixer:
//   http://cpansearch.perl.org/src/CORION/App-VideoMixer-0.02/filters/scanlines.glsl
//

package org.cyanogenmod.wallpapers.photophase.effects;

import android.media.effect.EffectContext;

/**
 * A TV scanline effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class ScanlinesEffect extends PhotoPhaseEffect {

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "uniform float offset;\n" +
            "float frequency = 83.0;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main(void)\n" +
            "{\n" +
            "    float global_pos = (v_texcoord.y + offset) * frequency;\n" +
            "    float wave_pos = cos((fract( global_pos ) - 0.5)*3.14);\n" +
            "    vec4 pel = texture2D( tex_sampler, v_texcoord );\n" +
            "    gl_FragColor = mix(vec4(0,0,0,0), pel, wave_pos);\n" +
            "}";

    /**
     * An abstract contructor of <code>Effect</code> to follow the rules
     * defined by {@see EffectFactory}.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public ScanlinesEffect(EffectContext ctx, String name) {
        super(ctx, ScanlinesEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);
    }

}
