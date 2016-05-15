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

package com.ruesga.android.wallpapers.photophase.effects;

import android.media.effect.EffectContext;

/**
 * A Warhol effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class WarholEffect extends PhotoPhaseEffect {

    private final String VERTEX_SHADER =
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_position;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            "  gl_Position = vec4(a_position.xy, 0.0, 1.0);\n" +
            "  gl_Position = sign(gl_Position);\n" +
            "  v_position = gl_Position.xy;\n" +
            "  v_texcoord = a_texcoord;\n" +
            "}\n";
    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_position;\n" +
            "varying vec2 v_texcoord;\n" +
            "const float steps = 2.0;\n" +
            "const float dotsize = 1.0 / steps;\n" +
            "const float half_step = dotsize / 2.0;\n" +
            "void main(void)\n" +
            "{\n" +
            "    vec2 av;\n" +
            "    if (v_position.x < 0.0 && v_position.y < 0.0) {\n" +
            "        av = vec2(v_position.x + 1.0, v_position.y + 1.0);\n" +
            "    } else if (v_position.x >= 0.0 && v_position.y < 0.0) {\n" +
            "        av = vec2(v_position.x, v_position.y + 1.0);\n" +
            "    } else if (v_position.x < 0.0 && v_position.y >= 0.0) {\n" +
            "        av = vec2(v_position.x + 1.0, v_position.y);\n" +
            "    } else if (v_position.x >= 0.0 && v_position.y >= 0.0) {\n" +
            "        av = vec2(v_position.x, v_position.y);\n" +
            "    }\n" +
            "    vec4 tex = texture2D(tex_sampler, av);\n" +
            "    vec4 tint;\n" +
            "    int ofs = int(v_texcoord.x * steps) + int(v_texcoord.y * steps) * 2;\n" +
            "    if (0 == ofs) {\n" +
            "        tint = vec4(0.9811764705882353, 0.2941176470588235, 0.2784313725490196, 0.0);\n" +
            "    } else if (1 == ofs) {\n" +
            "        tint = vec4(0.2627450980392157, 0.9698039215686275, 0.6784313725490196, 0.0);\n" +
            "    } else if (2 == ofs) {\n" +
            "        tint = vec4(0.9411764705882353, 0.796078431372549, 0.2901960784313725, 0.0);\n" +
            "    } else {\n" +
            "        tint = vec4(0.3843137254901961, 0.6117647058823529, 0.9627450980392157, 0.0);\n" +
            "    }\n" +
            "    float gray  = dot(tex.rgb, vec3(0.3, 0.59, 0.11));\n" +
            "    gl_FragColor = mix(tex, tint, gray);\n" +
            "}";

    /**
     * Constructor of <code>WarholEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public WarholEffect(EffectContext ctx, String name) {
        super(ctx, WarholEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);
    }

}
