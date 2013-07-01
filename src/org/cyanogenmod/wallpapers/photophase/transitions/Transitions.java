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

package org.cyanogenmod.wallpapers.photophase.transitions;

import android.content.Context;

import org.cyanogenmod.wallpapers.photophase.PhotoFrame;
import org.cyanogenmod.wallpapers.photophase.TextureManager;
import org.cyanogenmod.wallpapers.photophase.preferences.PreferencesProvider.Preferences;


/**
 * A class that manages all the supported transitions
 */
public class Transitions {

    /**
     * Enumeration of the supported transitions
     */
    public enum TRANSITIONS {
        /**
         * A random combination of all supported transitions
         */
        RANDOM,
        /**
         * @see NullTransition
         */
        NO_TRANSITION,
        /**
         * @see SwapTransition
         */
        SWAP,
        /**
         * @see FadeTransition
         */
        FADE,
        /**
         * @see TranslateTransition
         */
        TRANSLATION;
    }

    /**
     * Method that return the next type of transition to apply the picture.
     *
     * @param frame The frame which the effect will be applied to
     * @return TRANSITIONS The next type of  transition to apply
     */
    public static TRANSITIONS getNextTypeOfTransition(PhotoFrame frame) {
        int transition = Preferences.General.Transitions.getTransitionTypes();
        if (transition == TRANSITIONS.RANDOM.ordinal()) {
            int low = TRANSITIONS.SWAP.ordinal();
            int hight = TRANSITIONS.values().length - 1;
            transition = low + (int)(Math.random() * ((hight - low) + 1));
        }
        if (transition == TRANSITIONS.SWAP.ordinal()) {
            return TRANSITIONS.SWAP;
        }
        if (transition == TRANSITIONS.FADE.ordinal()) {
            return TRANSITIONS.FADE;
        }
        if (transition == TRANSITIONS.TRANSLATION.ordinal()) {
            return TRANSITIONS.TRANSLATION;
        }
        return TRANSITIONS.NO_TRANSITION;
    }

    /**
     * Method that creates a new transition.
     *
     * @param ctx The current context
     * @param tm The texture manager
     * @param type The type of transition
     * @param frame The frame which the effect will be applied to
     * @return Transition The next transition to apply
     */
    public static Transition createTransition(
            Context ctx, TextureManager tm, TRANSITIONS type, PhotoFrame frame) {
        if (type.compareTo(TRANSITIONS.SWAP) == 0) {
            return new SwapTransition(ctx, tm);
        }
        if (type.compareTo(TRANSITIONS.FADE) == 0) {
            return new FadeTransition(ctx, tm);
        }
        if (type.compareTo(TRANSITIONS.TRANSLATION) == 0) {
            return new TranslateTransition(ctx, tm);
        }
        return new NullTransition(ctx, tm);
    }
}
