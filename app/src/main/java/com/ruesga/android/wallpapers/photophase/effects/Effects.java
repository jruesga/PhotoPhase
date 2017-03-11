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

import android.content.Context;
import android.graphics.Color;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A class that manages all the supported effects
 */
public class Effects {

    public static class Settings {
        public final int mMin;
        public final int mMax;
        public final int mDef;
        public Settings(int min, int max, int def) {
            mMin = min;
            mMax = max;
            mDef = def;
        }
    }

    /**
     * Enumeration of the supported effects
     */
    public enum EFFECTS {
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_NULL
         */
        NO_EFFECT(0, null),
        /**
         * @see EffectFactory#EFFECT_AUTOFIX
         */
        AUTOFIX(1, new Settings(0, 20, 5)),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_BLUR
         */
        BLUR(2, new Settings(0, 20, 5)),
        /**
         * @see EffectFactory#EFFECT_CROSSPROCESS
         */
        CROSSPROCESS(3, null),
        /**
         * @see EffectFactory#EFFECT_DOCUMENTARY
         */
        DOCUMENTARY(4, null),
        /**
         * @see EffectFactory#EFFECT_DUOTONE
         */
        DUOTONE(5, new Settings(0, 8, 0)),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_EMBOSS
         */
        EMBOSS(6, null),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_FISHEYE
         */
        FISHEYE(7, null),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_GLOW
         */
        GLOW(8, null),
        /**
         * @see EffectFactory#EFFECT_GRAIN
         */
        GRAIN(9, new Settings(0, 20, 13)),
        /**
         * @see EffectFactory#EFFECT_GRAYSCALE
         */
        GRAYSCALE(10, null),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_HALFTONE
         */
        HALFTONE(11, new Settings(0, 20, 10)),
        /**
         * @see EffectFactory#EFFECT_LOMOISH
         */
        LOMOISH(12, null),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_MIRROR
         */
        MIRROR(13, null),
        /**
         * @see EffectFactory#EFFECT_NEGATIVE
         */
        NEGATIVE(14, null),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_OUTLINE
         */
        OUTLINE(15, null),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_PIXELATE
         */
        PIXELATE(16, new Settings(0, 20, 8)),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_POPART
         */
        POPART(17, null),
        /**
         * @see EffectFactory#EFFECT_POSTERIZE
         */
        POSTERIZE(18, null),
        /**
         * @see EffectFactory#EFFECT_SATURATE
         */
        SATURATE(19, new Settings(0, 20, 8)),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_SCANLINES
         */
        SCANLINES(20, new Settings(0, 20, 10)),
        /**
         * @see EffectFactory#EFFECT_SEPIA
         */
        SEPIA(21, null),
        /**
         * @see EffectFactory#EFFECT_TEMPERATURE
         */
        TEMPERATURE(22, new Settings(0, 20, 15)),
        /**
         * @see EffectFactory#EFFECT_TINT
         */
        TINT(23, new Settings(0, 12, 0)),
        /**
         * @see EffectFactory#EFFECT_VIGNETTE
         */
        VIGNETTE(24, new Settings(0, 20, 15)),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_NOISE
         */
        NOISE(25, new Settings(0, 20, 9)),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_FROSTED
         */
        FROSTED(26, new Settings(0, 20, 9)),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_CROSSHATCHING
         */
        CROSSHATCHING(27, null),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_THERMALVISION
         */
        THERMALVISION(28, null),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_SWIRL
         */
        SWIRL(29, new Settings(0, 20, 4)),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_DOF
         */
        DOF(30, new Settings(0, 20, 15)),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_WARHOL
         */
        WARHOL(31, null),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_TOON
         */
        TOON(32, null),
        /**
         * @see PhotoPhaseEffectFactory#EFFECT_SOBEL
         */
        SOBEL(33, null);

        public final int mId;
        public final Settings mSettings;
        EFFECTS(int id, Settings settings) {
            mId = id;
            mSettings = settings;
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
    private final Context mContext;

    /**
     * Constructor of <code>Effects</code>
     *
     * @param effectContext The current effect context
     */
    public Effects(Context context, EffectContext effectContext) {
        super();
        mCachedEffects = new HashMap<>();
        mEffectContext = effectContext;
        mContext = context;
    }

    /**
     * Method that that release the cached data
     */
    public void release() {
        for (Effect effect : mCachedEffects.values()) {
            try {
                effect.release();
            } catch (NoSuchElementException ex) {
                // Catching a runtime exception is not ideally, but releasing
                // the effect causes a fc it the effect is not a valid state.
                // Since we are releasing the effect we can ignore it to avoid
                // crash the app
            }
        }
        mCachedEffects.clear();
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
                Preferences.General.Effects.getSelectedEffects(mContext));
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
            effect = mCachedEffects.get(nextEffect);
            updateParameters(nextEffect, effect);
            return effect;
        }

        // Select the effect if is available
        if (nextEffect.compareTo(EFFECTS.NO_EFFECT) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_NULL)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_NULL);
            }
        } else if (nextEffect.compareTo(EFFECTS.AUTOFIX) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_AUTOFIX)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_AUTOFIX);
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
            }
        } else if (nextEffect.compareTo(EFFECTS.EMBOSS) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_EMBOSS)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_EMBOSS);
            }
        } else if (nextEffect.compareTo(EFFECTS.FISHEYE) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_FISHEYE)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_FISHEYE);
            }
        } else if (nextEffect.compareTo(EFFECTS.GLOW) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_GLOW)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_GLOW);
            }
        } else if (nextEffect.compareTo(EFFECTS.GRAIN) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAIN)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAIN);
            }
        } else if (nextEffect.compareTo(EFFECTS.GRAYSCALE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_GRAYSCALE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
            }
        } else if (nextEffect.compareTo(EFFECTS.HALFTONE) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_HALFTONE)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_HALFTONE);
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
            }
        } else if (nextEffect.compareTo(EFFECTS.TINT) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_TINT)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_TINT);
            }
        } else if (nextEffect.compareTo(EFFECTS.VIGNETTE) == 0) {
            if (EffectFactory.isEffectSupported(EffectFactory.EFFECT_VIGNETTE)) {
                effect = effectFactory.createEffect(EffectFactory.EFFECT_VIGNETTE);
            }
        } else if (nextEffect.compareTo(EFFECTS.NOISE) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_NOISE)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_NOISE);
            }
        } else if (nextEffect.compareTo(EFFECTS.FROSTED) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_FROSTED)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_FROSTED);
            }
        } else if (nextEffect.compareTo(EFFECTS.CROSSHATCHING) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_CROSSHATCHING)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_CROSSHATCHING);
            }
        } else if (nextEffect.compareTo(EFFECTS.THERMALVISION) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_THERMALVISION)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_THERMALVISION);
            }
        } else if (nextEffect.compareTo(EFFECTS.SWIRL) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_SWIRL)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_SWIRL);
            }
        } else if (nextEffect.compareTo(EFFECTS.DOF) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_DOF)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_DOF);
            }
        } else if (nextEffect.compareTo(EFFECTS.WARHOL) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_WARHOL)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_WARHOL);
            }
        } else if (nextEffect.compareTo(EFFECTS.TOON) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_TOON)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_TOON);
            }
        } else if (nextEffect.compareTo(EFFECTS.SOBEL) == 0) {
            if (EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_SOBEL)) {
                effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_SOBEL);
            }
        }

        // Instead of not to apply any effect, just use one null effect to follow the same
        // effect model. This allow to use the same height when Effect.apply is applied for all
        // the frames
        if (effect == null && EffectFactory.isEffectSupported(PhotoPhaseEffectFactory.EFFECT_NULL)) {
            effect = effectFactory.createEffect(PhotoPhaseEffectFactory.EFFECT_NULL);
            nextEffect = EFFECTS.NO_EFFECT;
        }

        if (effect != null) {
            // Cache the effects
            mCachedEffects.put(nextEffect, effect);
        }

        updateParameters(nextEffect, effect);
        return effect;
    }

    private void updateParameters(EFFECTS type, Effect effect) {
        Settings settings = type.mSettings;
        if (settings == null) {
            return;
        }
        int val = Preferences.General.Effects.getEffectSettings(mContext, type.mId, settings.mDef);

        // Update the parameters
        if (type.compareTo(EFFECTS.AUTOFIX) == 0) {
            effect.setParameter("scale", (val * 0.05f));
        } else if (type.compareTo(EFFECTS.BLUR) == 0) {
            effect.setParameter(BlurEffect.STRENGTH_PARAMETER, (val * 0.2f) + 1.0f);
        } else if (type.compareTo(EFFECTS.FROSTED) == 0) {
            effect.setParameter(FrostedEffect.STRENGTH_PARAMETER, (val * 0.005f) + 0.005f);
        } else if (type.compareTo(EFFECTS.GRAIN) == 0) {
            effect.setParameter("strength", (val * 0.075f) + 0.075f);
        } else if (type.compareTo(EFFECTS.DOF) == 0) {
            effect.setParameter(DoFEffect.STRENGTH_PARAMETER, (val * 0.05f));
        } else if (type.compareTo(EFFECTS.SCANLINES) == 0) {
            effect.setParameter(ScanlinesEffect.STRENGTH_PARAMETER, (val * 0.3f) + 3f);
        } else if (type.compareTo(EFFECTS.HALFTONE) == 0) {
            effect.setParameter(HalftoneEffect.STRENGTH_PARAMETER, (val * 4f) + 40f);
        } else if (type.compareTo(EFFECTS.NOISE) == 0) {
            effect.setParameter(NoiseEffect.STRENGTH_PARAMETER, (val * 0.00175) + 0.00175);
        } else if (type.compareTo(EFFECTS.PIXELATE) == 0) {
            effect.setParameter(PixelateEffect.STRENGTH_PARAMETER, (val * 0.075) + 0.5);
        } else if (type.compareTo(EFFECTS.SATURATE) == 0) {
            effect.setParameter("scale", (val * 0.04f) + 0.1f);
        } else if (type.compareTo(EFFECTS.TEMPERATURE) == 0) {
            effect.setParameter("scale", (val * 0.04f) + 0.1f);
        } else if (type.compareTo(EFFECTS.VIGNETTE) == 0) {
            effect.setParameter("scale", (val * 0.04f) + 0.1f);
        } else if (type.compareTo(EFFECTS.SWIRL) == 0) {
            effect.setParameter(SwirlEffect.STRENGTH_PARAMETER, (val * 0.15f) + 1.0f);
        } else if (type.compareTo(EFFECTS.DUOTONE) == 0) {
            final String[] firstColors = {"#ff4a61c6", "#ffc64a50", "#ff4ac65b"};
            final int[] secondColors = {Color.WHITE, Color.BLACK, Color.LTGRAY};
            effect.setParameter("first_color", Color.parseColor(firstColors[val % firstColors.length]));
            effect.setParameter("second_color", secondColors[(val / secondColors.length)]);
        }  else if (type.compareTo(EFFECTS.TINT) == 0) {
            final int[] colors = {
                    Color.WHITE, Color.BLACK, Color.LTGRAY, Color.LTGRAY, Color.DKGRAY,
                    Color.parseColor("#ff4a61c6"), Color.parseColor("#ffc64a50"),
                    Color.parseColor("#ff4ac65b"), Color.parseColor("#ffc64ab6"),
                    Color.parseColor("#ffc6c54a"), Color.parseColor("#ffc69f4a"),
                    Color.parseColor("#ff934ac6"), Color.parseColor("#ffffa500")};
            effect.setParameter("tint", colors[val]);
        }
    }
}
