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
 * This effect only copies the source texture to the destination texture.<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class NullEffect extends PhotoPhaseEffect {

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main(void)\n" +
            "{\n" +
            "    gl_FragColor = texture2D(tex_sampler, v_texcoord);\n" +
            "}";

    /**
     * Constructor of <code>NullEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public NullEffect(EffectContext ctx, String name) {
        super(ctx, NullEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void applyProgram(int index, int inputTexId, int width, int height) {
        // Nothing to draw
    }
}
