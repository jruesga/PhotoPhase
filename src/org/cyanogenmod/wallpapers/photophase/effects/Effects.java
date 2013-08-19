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
import org.cyanogenmod.wallpapers.photophase.utils.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that manages all the supported effects
 */
public class Effects {

    /**
     * Enumeration of the supported effects
     */
    public enum EFFECTS {
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_NULL
         */
        NO_EFFECT,
        /**
         * @see EffectFactory#EFFECT_AUTOFIX
         */
        AUTOFIX,
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_BLUR
         */
        BLUR,
        /**
         * @see EffectFactory#EFFECT_CROSSPROCESS
         */
        CROSSPROCESS,
        /**
         * @see EffectFactory#EFFECT_DOCUMENTARY
         */
        DOCUMENTARY,
        /**
         * @see EffectFactory#EFFECT_DUOTONE
         */
        DUOTONE,
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_EMBOSS
         */
        EMBOSS,
        /**
         * @see EffectFactory#EFFECT_FISHEYE
         */
        FISHEYE,
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_GLOW
         */
        GLOW,
        /**
         * @see EffectFactory#EFFECT_GRAIN
         */
        GRAIN,
        /**
         * @see EffectFactory#EFFECT_GRAYSCALE
         */
        GRAYSCALE,
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_HALFTONE
         */
        HALFTONE,
        /**
         * @see EffectFactory#EFFECT_LOMOISH
         */
        LOMOISH,
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_MIRROR
         */
        MIRROR,
        /**
         * @see EffectFactory#EFFECT_NEGATIVE
         */
        NEGATIVE,
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_OUTLINE
         */
        OUTLINE,
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_PIXELATE
         */
        PIXELATE,
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_POPART
         */
        POPART,
        /**
         * @see EffectFactory#EFFECT_POSTERIZE
         */
        POSTERIZE,
        /**
         * @see EffectFactory#EFFECT_SATURATE
         */
        SATURATE,
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_SCANLINES
         */
        SCANLINES,
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

    private final Map<EFFECTS, Effect> mCachedEffects;
    private final EffectContext mEffectContext;

    /**
     * Constructor of <code>Effects</code>
     *
     * @param effectContext The current effect context
     */
    public Effects(EffectContext effectContext) {
        super();
        mCachedEffects = new HashMap<Effects.EFFECTS, Effect>();
        mEffectContext = effectContext;
    }

    /**
     * Method that that release the cached data
     */
    public void release() {
        if (mCachedEffects != null) {
            for (Effect effect : mCachedEffects.values()) {
                effect.release();
            }
            mCachedEffects.clear();
        }
    }

    /**
     * Method that return the next effect to use with the picture.
     *
     * @return Effect The next effect to use or null if no need to apply any effect
     */
    @SuppressWarnings("boxing")
    public Effect getNextEffect() {
        // Get a new instance of a effect factory
        EffectFactory effectFactory = mEffectContext.getFactory();
        Effect effect = null;

        // Get an effect based on the user preference
        List<EFFECTS> effects = Arrays.asList(Preferences.General.Effects.getEffectTypes());
        EFFECTS nextEffect = null;
        if (effects.size() > 0) {
            int low = 0;
            int high = effects.size() - 1;
            int pos = Utils.getNextRandom(low, high);
            nextEffect = effects.get(pos);
        }
        if (nextEffect == null) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_NULL)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_NULL);
                mCachedEffects.put(nextEffect, effect);
            }
            return effect;
        }

        // Has a cached effect?
        if (mCachedEffects.containsKey(nextEffect)) {
            return mCachedEffects.get(nextEffect);
        }

        // Select the effect if is available
        if (nextEffect.compareTo(EFFECTS.AUTOFIX) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_AUTOFIX)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_AUTOFIX);
                effect.setParameter("scale", 0.5f);
            }
        } else if (nextEffect.compareTo(EFFECTS.BLUR) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_BLUR)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_BLUR);
            }
        } else if (nextEffect.compareTo(EFFECTS.CROSSPROCESS) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_CROSSPROCESS)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_CROSSPROCESS);
            }
        } else if (nextEffect.compareTo(EFFECTS.DOCUMENTARY) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_DOCUMENTARY)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_DOCUMENTARY);
            }
        } else if (nextEffect.compareTo(EFFECTS.DUOTONE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_DUOTONE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                effect.setParameter("first_color", Color.parseColor("#FF8CACFF"));
                effect.setParameter("second_color", Color.WHITE);
            }
        } else if (nextEffect.compareTo(EFFECTS.EMBOSS) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_EMBOSS)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_EMBOSS);
            }
        } else if (nextEffect.compareTo(EFFECTS.FISHEYE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_FISHEYE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_FISHEYE);
                effect.setParameter("scale", 1.0f);
            }
        } else if (nextEffect.compareTo(EFFECTS.GLOW) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_GLOW)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_GLOW);
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
        } else if (nextEffect.compareTo(EFFECTS.HALFTONE) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_HALFTONE)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_HALFTONE);
                effect.setParameter("strength", 8.0f);
            }
        } else if (nextEffect.compareTo(EFFECTS.MIRROR) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_MIRROR)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_MIRROR);
            }
        } else if (nextEffect.compareTo(EFFECTS.LOMOISH) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_LOMOISH)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_LOMOISH);
            }
        } else if (nextEffect.compareTo(EFFECTS.NEGATIVE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_NEGATIVE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_NEGATIVE);
            }
        } else if (nextEffect.compareTo(EFFECTS.OUTLINE) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_OUTLINE)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_OUTLINE);
            }
        } else if (nextEffect.compareTo(EFFECTS.PIXELATE) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_PIXELATE)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_PIXELATE);
                effect.setParameter("strength", 8.0f);
            }
        } else if (nextEffect.compareTo(EFFECTS.POPART) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_POPART)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_POPART);
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
        } else if (nextEffect.compareTo(EFFECTS.SCANLINES) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_SCANLINES)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_SCANLINES);
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

        // Instead of not to apply any effect, just use one null effect to follow the same
        // effect model. This allow to use the same height when Effect.apply is applied for all
        // the frames
        if (effect == null && EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_NULL)) {
            effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_NULL);
            nextEffect = EFFECTS.NO_EFFECT;
        }

        // Cache the effects
        mCachedEffects.put(nextEffect, effect);
        return effect;
    }
}
