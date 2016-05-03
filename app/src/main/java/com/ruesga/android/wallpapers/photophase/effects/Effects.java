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

import android.graphics.Color;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.utils.Utils;

import java.util.HashMap;
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
        NO_EFFECT(0),
        /**
         * @see EffectFactory#EFFECT_AUTOFIX
         */
        AUTOFIX(1),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_BLUR
         */
        BLUR(2),
        /**
         * @see EffectFactory#EFFECT_CROSSPROCESS
         */
        CROSSPROCESS(3),
        /**
         * @see EffectFactory#EFFECT_DOCUMENTARY
         */
        DOCUMENTARY(4),
        /**
         * @see EffectFactory#EFFECT_DUOTONE
         */
        DUOTONE(5),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_EMBOSS
         */
        EMBOSS(6),
        /**
         * @see EffectFactory#EFFECT_FISHEYE
         */
        FISHEYE(7),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_GLOW
         */
        GLOW(8),
        /**
         * @see EffectFactory#EFFECT_GRAIN
         */
        GRAIN(9),
        /**
         * @see EffectFactory#EFFECT_GRAYSCALE
         */
        GRAYSCALE(10),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_HALFTONE
         */
        HALFTONE(11),
        /**
         * @see EffectFactory#EFFECT_LOMOISH
         */
        LOMOISH(12),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_MIRROR
         */
        MIRROR(13),
        /**
         * @see EffectFactory#EFFECT_NEGATIVE
         */
        NEGATIVE(14),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_OUTLINE
         */
        OUTLINE(15),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_PIXELATE
         */
        PIXELATE(16),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_POPART
         */
        POPART(17),
        /**
         * @see EffectFactory#EFFECT_POSTERIZE
         */
        POSTERIZE(18),
        /**
         * @see EffectFactory#EFFECT_SATURATE
         */
        SATURATE(19),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_SCANLINES
         */
        SCANLINES(20),
        /**
         * @see EffectFactory#EFFECT_SEPIA
         */
        SEPIA(21),
        /**
         * @see EffectFactory#EFFECT_TEMPERATURE
         */
        TEMPERATURE(22),
        /**
         * @see EffectFactory#EFFECT_TINT
         */
        TINT(23),
        /**
         * @see EffectFactory#EFFECT_VIGNETTE
         */
        VIGNETTE(24);

        public final int mId;
        EFFECTS(int id) {
            mId = id;
        }

        public static EFFECTS fromId(int id) {
            for (EFFECTS effect : EFFECTS.values()) {
                if (effect.mId == id) {
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
        mCachedEffects = new HashMap<>();
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
        // Get an effect based on the user preference
        EFFECTS[] effects = Preferences.General.Effects.toEFFECTS(
                Preferences.General.Effects.getSelectedEffects());
        EFFECTS nextEffect = null;
        if (effects.length > 0) {
            int low = 0;
            int high = effects.length - 1;
            int pos = Utils.getNextRandom(low, high);
            nextEffect = effects[pos];
        }
        return getEffect(nextEffect);
    }

    public Effect getEffect(EFFECTS nextEffect) {
        EffectFactory effectFactory = mEffectContext.getFactory();
        Effect effect = null;

        // Ensure we apply at least an effect (a null one)
        if (nextEffect == null) {
            nextEffect = EFFECTS.NO_EFFECT;
        }

        // The effect was cached previously?
        if (mCachedEffects.containsKey(nextEffect)) {
            return mCachedEffects.get(nextEffect);
        }

        // Select the effect if is available
        if (nextEffect.compareTo(EFFECTS.NO_EFFECT) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_NULL)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_NULL);
            }
        } else if (nextEffect.compareTo(EFFECTS.AUTOFIX) == 0) {
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
