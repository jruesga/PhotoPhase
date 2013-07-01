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
         * @see NullEffect
         */
        NO_EFFECT,
        /**
         * @see BlackAndWhiteEffect
         */
        BLACK_AND_WHITE,
        /**
         * @see SepiaEffect
         */
        SEPIA;
    }

    /**
     * Method that return the next effect to use with the picture.
     *
     * @return Effect The next effect to use
     */
    public static Effect getNextEffect() {
        int effect = Preferences.General.Effects.getEffectTypes();
        if (effect == EFFECTS.RANDOM.ordinal()) {
            int low = EFFECTS.NO_EFFECT.ordinal();
            int hight = EFFECTS.values().length - 1;
            effect = low + (int)(Math.random() * ((hight - low) + 1));
        }
        if (effect == EFFECTS.BLACK_AND_WHITE.ordinal()) {
            return new BlackAndWhiteEffect();
        }
        if (effect == EFFECTS.SEPIA.ordinal()) {
            return new SepiaEffect();
        }
        return new NullEffect();
    }
}
