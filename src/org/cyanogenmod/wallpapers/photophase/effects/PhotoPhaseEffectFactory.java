/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package org.cyanogenmod.wallpapers.photophase.effects;

/**
 * A class that defines the own PhotoPhase's effects implementation. This class follows the
 * rules of the MCA aosp library.
 */
public class PhotoPhaseEffectFactory {

    /**
     * <p>Applies halftone effect to the image.</p>
     * <p>Available parameters:</p>
     * <table>
     * <tr><td>Parameter name</td><td>Meaning</td><td>Valid values</td></tr>
     * <tr>
     *   <td><code>strength</code></td>
     *   <td>The halftone steps multiplier.</td>
     *   <td>Positive float (>0). Higher numbers produce smallest points;</td>
     * </tr>
     * </table>
     */
    public static final String EFFECT_HALFTONE = "org.cyanogenmod.wallpapers.photophase.effects.HalftoneEffect";
}
