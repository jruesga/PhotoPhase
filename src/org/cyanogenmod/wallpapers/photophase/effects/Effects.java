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

import android.graphics.Color;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

import org.cyanogenmod.wallpapers.photophase.preferences.PreferencesProvider.Preferences;

import java.util.Arrays;
import java.util.List;

/**
 * A class that manages all the supported effects
 */
public class Effects {

    /**
     * Enumeration of the supported effects
     */
    public enum EFFECTS {
        /**
         * No effect
         */
        NO_EFFECT,
        /**
         * @see EffectFactory#EFFECT_AUTOFIX
         */
        AUTOFIX,
        /**
         * @see EffectFactory#EFFECT_CROSSPROCESS
         */
        CROSSPROCESS,
        /**
         * @see EffectFactory#EFFECT_DOCUMENTARY
         */
        DOCUMENTARY,
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_HALFTONE
         */
        HALFTONE,
        /**
         * @see EffectFactory#EFFECT_DUOTONE
         */
        DUOTONE,
        /**
         * @see EffectFactory#EFFECT_FISHEYE
         */
        FISHEYE,
        /**
         * @see EffectFactory#EFFECT_GRAIN
         */
        GRAIN,
        /**
         * @see EffectFactory#EFFECT_GRAYSCALE
         */
        GRAYSCALE,
        /**
         * @see EffectFactory#EFFECT_LOMOISH
         */
        LOMOISH,
        /**
         * @see EffectFactory#EFFECT_NEGATIVE
         */
        NEGATIVE,
        /**
         * @see EffectFactory#EFFECT_POSTERIZE
         */
        POSTERIZE,
        /**
         * @see EffectFactory#EFFECT_SATURATE
         */
        SATURATE,
        /**
         * @see EffectFactory#EFFECT_SEPIA
         */
        SEPIA,
        /**
         * @see EffectFactory#EFFECT_TEMPERATURE
         */
        TEMPERATURE,
        /**
         * @see EffectFactory#EFFECT_TINT
         */
        TINT,
        /**
         * @see EffectFactory#EFFECT_VIGNETTE
         */
        VIGNETTE;

        /**
         * Method that returns the effect from its ordinal position
         *
         * @param ordinal The ordinal position
         * @return EFFECTS The effect or null if wasn't found
         */
        public static EFFECTS fromOrdinal(int ordinal) {
            for (EFFECTS effect : EFFECTS.values()) {
                if (effect.ordinal() == ordinal) {
                    return effect;
                }
            }
            return null;
        }
    }


    /**
     * Method that return the next effect to use with the picture.
     *
     * @param effectContext The android media effects context
     * @return Effect The next effect to use or null if no need to apply any effect
     */
    @SuppressWarnings("boxing")
    public static Effect getNextEffect(EffectContext effectContext) {
        // Get a new instance of a effect factory
        EffectFactory effectFactory = effectContext.getFactory();

        // Get an effect based on the user preference
        List<EFFECTS> effects = Arrays.asList(Preferences.General.Effects.getEffectTypes());
        EFFECTS nextEffect = null;
        if (effects.size() > 0) {
            int low = 0;
            int hight = effects.size() - 1;
            int pos = low + (int)(Math.random() * ((hight - low) + 1));
            nextEffect = effects.get(pos);
        }
        if (nextEffect == null) {
            return null;
        }

        // Select the effect if is available
        Effect effect = null;
        if (nextEffect.compareTo(EFFECTS.AUTOFIX) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_AUTOFIX)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_AUTOFIX);
                effect.setParameter("scale", 0.5f);
            }
        } else if (nextEffect.compareTo(EFFECTS.CROSSPROCESS) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_CROSSPROCESS)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_CROSSPROCESS);
            }
        } else if (nextEffect.compareTo(EFFECTS.DOCUMENTARY) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_DOCUMENTARY)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_DOCUMENTARY);
            }
        } else if (nextEffect.compareTo(EFFECTS.HALFTONE) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_HALFTONE)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_HALFTONE);
                effect.setParameter("strength", 30.0f);
            }
        } else if (nextEffect.compareTo(EFFECTS.DUOTONE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_DUOTONE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                effect.setParameter("first_color", Color.parseColor("#FF8CACFF"));
                effect.setParameter("second_color", Color.WHITE);
            }
        } else if (nextEffect.compareTo(EFFECTS.FISHEYE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_FISHEYE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_FISHEYE);
                effect.setParameter("scale", 1.0f);
            }
        } else if (nextEffect.compareTo(EFFECTS.GRAIN) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAIN)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAIN);
                effect.setParameter("strength", 1.0f);
            }
        } else if (nextEffect.compareTo(EFFECTS.GRAYSCALE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAYSCALE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
            }
        } else if (nextEffect.compareTo(EFFECTS.LOMOISH) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_LOMOISH)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_LOMOISH);
            }
        } else if (nextEffect.compareTo(EFFECTS.NEGATIVE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_NEGATIVE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_NEGATIVE);
            }
        } else if (nextEffect.compareTo(EFFECTS.POSTERIZE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_POSTERIZE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_POSTERIZE);
            }
        } else if (nextEffect.compareTo(EFFECTS.SATURATE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_SATURATE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_SATURATE);
                effect.setParameter("scale", .5f);
            }
        } else if (nextEffect.compareTo(EFFECTS.SEPIA) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_SEPIA)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_SEPIA);
            }
        } else if (nextEffect.compareTo(EFFECTS.TEMPERATURE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_TEMPERATURE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_TEMPERATURE);
                effect.setParameter("scale", .9f);
            }
        } else if (nextEffect.compareTo(EFFECTS.TINT) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_TINT)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_TINT);
            }
        } else if (nextEffect.compareTo(EFFECTS.VIGNETTE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_VIGNETTE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_VIGNETTE);
                effect.setParameter("scale", .5f);
            }
        }
        return effect;
    }
}
