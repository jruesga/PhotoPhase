//
//   https://github.com/spite/Wagner/blob/master/fragment-shaders/sobel2-fs.glsl
//

package com.ruesga.android.wallpapers.photophase.effects;

import android.media.effect.EffectContext;
import android.opengl.GLES20;

import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;

/**
 * A sobel effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class SobelEffect extends PhotoPhaseEffect {

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform float w;\n" +
            "uniform float h;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "\n" +
            "vec2 texel;\n" +
            "mat3 G[2];\n" +
            "\n" +
            "const mat3 g0 = mat3(1.0, 2.0, 1.0, 0.0, 0.0, 0.0, -1.0, -2.0, -1.0);\n" +
            "const mat3 g1 = mat3(1.0, 0.0, -1.0, 2.0, 0.0, -2.0, 1.0, 0.0, -1.0);\n" +
            "\n" +
            "void main(void) {\n" +
            "    mat3 I;\n" +
            "    float cnv[2];\n" +
            "    vec3 sample;\n" +
            "\n" +
            "    G[0] = g0;\n" +
            "    G[1] = g1;\n" +
            "    texel = vec2(1.0 / w, 1.0 / h);\n" +
            "\n" +
            "    for (float i=0.0; i<3.0; i++) {\n" +
            "        for (float j=0.0; j<3.0; j++) {\n" +
            "            sample = texture2D(tex_sampler, v_texcoord + texel * vec2(i-1.0,j-1.0)).rgb;\n" +
            "            I[int(i)][int(j)] = length(sample);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    for (int i=0; i<2; i++) {\n" +
            "        float dp3 = dot(G[i][0], I[0]) + dot(G[i][1], I[1]) + dot(G[i][2], I[2]);\n" +
            "        cnv[i] = dp3 * dp3;\n" +
            "    }\n" +
            "\n" +
            "    gl_FragColor = vec4(0.5 * sqrt(cnv[0] * cnv[0] + cnv[1] * cnv[1]));\n" +
            "}\n";

    private final int mWidthHandle;
    private final int mHeightHandle;

    /**
     * Constructor of <code>ToonEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public SobelEffect(EffectContext ctx, String name) {
        super(ctx, SobelEffect.class.getName());
        init(VERTEX_SHADER, FRAGMENT_SHADER);

        // Parameters
        mWidthHandle = GLES20.glGetUniformLocation(mProgram[0], "w");
        GLESUtil.glesCheckError("glGetUniformLocation");
        mHeightHandle = GLES20.glGetUniformLocation(mProgram[0], "h");
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
    }
}
