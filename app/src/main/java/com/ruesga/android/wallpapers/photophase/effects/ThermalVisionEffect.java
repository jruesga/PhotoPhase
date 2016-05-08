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
 * A thermal vision effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class ThermalVisionEffect extends PhotoPhaseEffect {

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() \n" +
            "{ \n" +
            "    vec2 uv = v_texcoord;\n" +
            "    vec3 pixcol = texture2D(tex_sampler, uv).rgb;\n" +
            "    vec3 colors[3];\n" +
            "    colors[0] = vec3(0.,0.,1.);\n" +
            "    colors[1] = vec3(1.,1.,0.);\n" +
            "    colors[2] = vec3(1.,0.,0.);\n" +
            "    float lum = (pixcol.r+pixcol.g+pixcol.b)/3.;\n" +
            "    int ix = (lum < 0.5)? 0:1;\n" +
            "    vec3 tc = mix(colors[ix],colors[ix+1],(lum-float(ix)*0.3)/0.3);\n" +
            "    gl_FragColor = vec4(tc, 1.0);\n" +
            "}";

    /**
     * Constructor of <code>ThermalVisionEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public ThermalVisionEffect(EffectContext ctx, String name) {
        super(ctx, ThermalVisionEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);
    }

}
