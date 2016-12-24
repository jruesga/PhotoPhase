/*
 * Copyright (C) 2015 Jorge Ruesga
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

/**
 * A class that defines the own PhotoPhase's effects implementation. This class follows the
 * rules of the MCA aosp library.
 */
public class PhotoPhaseEffectFactory {

    /**
     * <p>Applies a blur effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_BLUR = "com.ruesga.android.wallpapers.photophase.effects.BlurEffect";

    /**
     * <p>Applies an emboss effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_EMBOSS = "com.ruesga.android.wallpapers.photophase.effects.EmbossEffect";

    /**
     * <p>Applies a fixeye effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_FISHEYE = "com.ruesga.android.wallpapers.photophase.effects.FisheyeEffect";

    /**
     * <p>Applies a glow effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_GLOW = "com.ruesga.android.wallpapers.photophase.effects.GlowEffect";

    /**
     * <p>Applies a halftone effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * <tr>
     *   <td><code>strength</code></td>
     *   <td>The halftone steps multiplier.</td>
     *   <td>Positive float (>0). Higher numbers produce smallest points</td>
     * </tr>
     * </table>
     */
    public static final String EFFECT_HALFTONE = "com.ruesga.android.wallpapers.photophase.effects.HalftoneEffect";

    /**
     * <p>Applies a mirror effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_MIRROR = "com.ruesga.android.wallpapers.photophase.effects.MirrorEffect";

    /**
     * <p>Doesn't apply any effect.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_NULL = "com.ruesga.android.wallpapers.photophase.effects.NullEffect";

    /**
     * <p>Applies an outline effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_OUTLINE = "com.ruesga.android.wallpapers.photophase.effects.OutlineEffect";

    /**
     * <p>Applies a pixelate effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * <tr>
     *   <td><code>strength</code></td>
     *   <td>The pixelate steps multiplier.</td>
     *   <td>Positive float (>0). Higher numbers produce more pixelation.</td>
     * </tr>
     * </table>
     */
    public static final String EFFECT_PIXELATE = "com.ruesga.android.wallpapers.photophase.effects.PixelateEffect";

    /**
     * <p>Applies a pop art (Warhol) effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_POPART = "com.ruesga.android.wallpapers.photophase.effects.PopArtEffect";

    /**
     * <p>Applies a TV scan line effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_SCANLINES = "com.ruesga.android.wallpapers.photophase.effects.ScanlinesEffect";

    /**
     * <p>Applies a noise effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_NOISE = "com.ruesga.android.wallpapers.photophase.effects.NoiseEffect";

    /**
     * <p>Applies a crosshatching effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_FROSTED = "com.ruesga.android.wallpapers.photophase.effects.FrostedEffect";

    /**
     * <p>Applies a crosshatching effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_CROSSHATCHING = "com.ruesga.android.wallpapers.photophase.effects.CrosshatchingEffect";

    /**
     * <p>Applies a thermal vision effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_THERMALVISION = "com.ruesga.android.wallpapers.photophase.effects.ThermalVisionEffect";

    /**
     * <p>Applies a swirl effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_SWIRL = "com.ruesga.android.wallpapers.photophase.effects.SwirlEffect";

    /**
     * <p>Applies a deep of field effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_DOF = "com.ruesga.android.wallpapers.photophase.effects.DoFEffect";

    /**
     * <p>Applies a warhol effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_WARHOL = "com.ruesga.android.wallpapers.photophase.effects.WarholEffect";

    /**
     * <p>Applies a cartoon effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_TOON = "com.ruesga.android.wallpapers.photophase.effects.ToonEffect";

    /**
     * <p>Applies a sobel edge detector effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * </table>
     */
    public static final String EFFECT_SOBEL = "com.ruesga.android.wallpapers.photophase.effects.SobelEffect";
}
