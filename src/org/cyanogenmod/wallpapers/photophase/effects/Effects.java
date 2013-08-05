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

import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

import org.cyanogenmod.wallpapers.photophase.preferences.PreferencesProvider.Preferences;

/**
 * A class that manages all the supported effects
 */
public class Effects {

    /**
     * Enumeration of the supported effects
     */
    public enum EFFECTS {
        /**
         * A random combination of all supported effects
         */
        RANDOM,
        /**
         * No effect
         */
        NO_EFFECT,
        /**
         * @see EffectFactory#EFFECT_GRAYSCALE
         */
        GRAYSCALE,
        /**
         * @see EffectFactory#EFFECT_SEPIA
         */
        SEPIA;
    }

    /**
     * Method that return the next effect to use with the picture.
     *
     * @param effectContext The android media effects context
     * @return Effect The next effect to use or null if no need to apply any effect
     */
    public static Effect getNextEffect(EffectContext effectContext) {
        // Get a new instance of a effect factory
        EffectFactory effectFactory = effectContext.getFactory();

        // Get an effect based on the user preference
        int effect = Preferences.General.Effects.getEffectTypes();
        if (effect == EFFECTS.RANDOM.ordinal()) {
            int low = EFFECTS.NO_EFFECT.ordinal();
            int hight = EFFECTS.values().length - 1;
            effect = low + (int)(Math.random() * ((hight - low) + 1));
        }

        // Select the effect if is available
        if (effect == EFFECTS.GRAYSCALE.ordinal()) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAYSCALE)) {
                return effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
            }
        }
        if (effect == EFFECTS.SEPIA.ordinal()) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_SEPIA)) {
                return effectFactory.createEffect(EffectFactory.EFFECT_SEPIA);
            }
        }
        return null;
    }
}
