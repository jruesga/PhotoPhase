//
//   https://github.com/spite/Wagner/blob/master/fragment-shaders/toon-fs.glsl
//

package com.ruesga.android.wallpapers.photophase.effects;

import android.media.effect.EffectContext;
import android.opengl.GLES20;

import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;

/**
 * A cartoon effect<br/>
 * <table>
 * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
 * </table>
 */
public class ToonEffect extends PhotoPhaseEffect {

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform float w;\n" +
            "uniform float h;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "\n" +
            "#define HueLevCount 6\n" +
            "#define SatLevCount 7\n" +
            "#define ValLevCount 4\n" +
            "float HueLevels[HueLevCount];\n" +
            "float SatLevels[SatLevCount];\n" +
            "float ValLevels[ValLevCount];\n" +
            "\n" +
            "vec3 RGBtoHSV(float r, float g, float b) {\n" +
            "   float minv, maxv, delta;\n" +
            "   vec3 res;\n" +
            "\n" +
            "   minv = min(min(r, g), b);\n" +
            "   maxv = max(max(r, g), b);\n" +
            "   res.z = maxv;\n" +
            "\n" +
            "   delta = maxv - minv;\n" +
            "\n" +
            "   if( maxv != 0.0 )\n" +
            "      res.y = delta / maxv;\n" +
            "   else {\n" +
            "      res.y = 0.0;\n" +
            "      res.x = -1.0;\n" +
            "      return res;\n" +
            "   }\n" +
            "\n" +
            "   if (r == maxv)\n" +
            "      res.x = ( g - b ) / delta;\n" +
            "   else if (g == maxv)\n" +
            "      res.x = 2.0 + (b - r) / delta;\n" +
            "   else\n" +
            "      res.x = 4.0 + (r - g) / delta;\n" +
            "\n" +
            "   res.x = res.x * 60.0;\n" +
            "   if (res.x < 0.0)\n" +
            "      res.x = res.x + 360.0;\n" +
            "\n" +
            "   return res;\n" +
            "}\n" +
            "\n" +
            "vec3 HSVtoRGB(float h, float s, float v) {\n" +
            "   int i;\n" +
            "   float f, p, q, t;\n" +
            "   vec3 res;\n" +
            "\n" +
            "   if (s == 0.0) {\n" +
            "      // achromatic (grey)\n" +
            "      res.x = v;\n" +
            "      res.y = v;\n" +
            "      res.z = v;\n" +
            "      return res;\n" +
            "   }\n" +
            "\n" +
            "   h /= 60.0;\n" +
            "   i = int(floor(h));\n" +
            "   f = h - float(i);\n" +
            "   p = v * ( 1.0 - s );\n" +
            "   q = v * ( 1.0 - s * f );\n" +
            "   t = v * ( 1.0 - s * ( 1.0 - f ) );\n" +
            "\n" +
            "   if (i==0) {\n" +
            "        res.x = v;\n" +
            "        res.y = t;\n" +
            "        res.z = p;\n" +
            "    } else if (i==1) {\n" +
            "         res.x = q;\n" +
            "         res.y = v;\n" +
            "         res.z = p;\n" +
            "    } else if (i==2) {\n" +
            "         res.x = p;\n" +
            "         res.y = v;\n" +
            "         res.z = t;\n" +
            "    } else if (i==3) {\n" +
            "         res.x = p;\n" +
            "         res.y = q;\n" +
            "         res.z = v;\n" +
            "    } else if (i==4) {\n" +
            "         res.x = t;\n" +
            "         res.y = p;\n" +
            "         res.z = v;\n" +
            "    } else if (i==5) {\n" +
            "         res.x = v;\n" +
            "         res.y = p;\n" +
            "         res.z = q;\n" +
            "   }\n" +
            "\n" +
            "   return res;\n" +
            "}\n" +
            "\n" +
            "float nearestLevel(float col, int mode) {\n" +
            "   if (mode==0) {\n" +
            "        for (int i =0; i<HueLevCount-1; i++ ) {\n" +
            "            if (col >= HueLevels[i] && col <= HueLevels[i+1]) {\n" +
            "              return HueLevels[i+1];\n" +
            "            }\n" +
            "        }\n" +
            "     }\n" +
            "\n" +
            "    if (mode==1) {\n" +
            "        for (int i =0; i<SatLevCount-1; i++ ) {\n" +
            "            if (col >= SatLevels[i] && col <= SatLevels[i+1]) {\n" +
            "              return SatLevels[i+1];\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    if (mode==2) {\n" +
            "        for (int i =0; i<ValLevCount-1; i++ ) {\n" +
            "            if (col >= ValLevels[i] && col <= ValLevels[i+1]) {\n" +
            "              return ValLevels[i+1];\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "float avg_intensity(vec4 pix) {\n" +
            "    return (pix.r + pix.g + pix.b) / 3.;\n" +
            "}\n" +
            "\n" +
            "vec4 get_pixel(vec2 coords, float dx, float dy) {\n" +
            "    return texture2D(tex_sampler, coords + vec2(dx, dy));\n" +
            "}\n" +
            "\n" +
            "float IsEdge(in vec2 coords){\n" +
            "    float dxtex = 1.0 / w;\n" +
            "    float dytex = 1.0 / h;\n" +
            "\n" +
            "    float pix[9];\n" +
            "\n" +
            "    int k = -1;\n" +
            "    float delta;\n" +
            "\n" +
            "    // read neighboring pixel intensities\n" +
            "    float pix0 = avg_intensity(get_pixel(coords, -1.0 * dxtex, -1.0 * dytex));\n" +
            "    float pix1 = avg_intensity(get_pixel(coords, -1.0 * dxtex,  0.0 * dytex));\n" +
            "    float pix2 = avg_intensity(get_pixel(coords, -1.0 * dxtex,  1.0 * dytex));\n" +
            "    float pix3 = avg_intensity(get_pixel(coords,  0.0 * dxtex, -1.0 * dytex));\n" +
            "    float pix4 = avg_intensity(get_pixel(coords,  0.0 * dxtex,  0.0 * dytex));\n" +
            "    float pix5 = avg_intensity(get_pixel(coords,  0.0 * dxtex,  1.0 * dytex));\n" +
            "    float pix6 = avg_intensity(get_pixel(coords,  1.0 * dxtex, -1.0 * dytex));\n" +
            "    float pix7 = avg_intensity(get_pixel(coords,  1.0 * dxtex,  0.0 * dytex));\n" +
            "    float pix8 = avg_intensity(get_pixel(coords,  1.0 * dxtex,  1.0 * dytex));\n" +
            "    delta = (abs(pix1 - pix7) + abs(pix5 - pix3) + abs(pix0 - pix8) + abs(pix2 - pix6)) / 4.;\n" +
            "\n" +
            "    return clamp(5.5*delta,0.0,1.0);\n" +
            "}\n" +
            "\n" +
            "void main(void)\n" +
            "{\n" +
            "    HueLevels[0] = 0.0;\n" +
            "    HueLevels[1] = 80.0;\n" +
            "    HueLevels[2] = 160.0;\n" +
            "    HueLevels[3] = 240.0;\n" +
            "    HueLevels[4] = 320.0;\n" +
            "    HueLevels[5] = 360.0;\n" +
            "\n" +
            "    SatLevels[0] = 0.0;\n" +
            "    SatLevels[1] = 0.1;\n" +
            "    SatLevels[2] = 0.3;\n" +
            "    SatLevels[3] = 0.5;\n" +
            "    SatLevels[4] = 0.6;\n" +
            "    SatLevels[5] = 0.8;\n" +
            "    SatLevels[6] = 1.0;\n" +
            "\n" +
            "    ValLevels[0] = 0.0;\n" +
            "    ValLevels[1] = 0.3;\n" +
            "    ValLevels[2] = 0.6;\n" +
            "    ValLevels[3] = 1.0;\n" +
            "\n" +
            "    vec4 colorOrg = texture2D(tex_sampler, v_texcoord);\n" +
            "    vec3 vHSV =  RGBtoHSV(colorOrg.r, colorOrg.g, colorOrg.b);\n" +
            "    vHSV.x = nearestLevel(vHSV.x, 0);\n" +
            "    vHSV.y = nearestLevel(vHSV.y, 1);\n" +
            "    vHSV.z = nearestLevel(vHSV.z, 2);\n" +
            "    float edg = IsEdge(v_texcoord);\n" +
            "    vec3 vRGB = (edg >= 0.3)? vec3(0.0, 0.0, 0.0) : HSVtoRGB(vHSV.x, vHSV.y, vHSV.z);\n" +
            "    gl_FragColor = vec4(vRGB.x, vRGB.y, vRGB.z, 1.0);\n" +
            "}\n";

    private final int mWidthHandle;
    private final int mHeightHandle;

    /**
     * Constructor of <code>ToonEffect</code>.
     *
     * @param ctx The effect context
     * @param name The effect name
     */
    public ToonEffect(EffectContext ctx, String name) {
        super(ctx, ToonEffect.class.getName());
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
