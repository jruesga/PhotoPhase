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

package com.ruesga.android.wallpapers.photophase.transitions;

import android.content.Context;

import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.textures.TextureManager;
import com.ruesga.android.wallpapers.photophase.utils.Utils;

/**
 * A class that manages all the supported transitions
 */
public class Transitions {

    /**
     * Enumeration of the supported transitions
     */
    public enum TRANSITIONS {
        /**
         * @see NullTransition
         */
        NO_TRANSITION(0),
        /**
         * @see CubeTransition
         */
        CUBE(1),
        /**
         * @see FadeTransition
         */
        FADE(2),
        /**
         * @see FlipTransition
         */
        FLIP(3),
        /**
         * @see SwapTransition
         */
        SWAP(4),
        /**
         * @see TranslateTransition
         */
        TRANSLATE(5),
        /**
         * @see WindowTransition
         */
        WINDOW(6),
        /**
         * @see BlurTransition
         */
        BLUR(7),
        /**
         * @see VertigoTransition
         */
        VERTIGO(8),
        /**
         * @see MixTransition
         */
        MIX(9),
        /**
         * @see ApertureTransition
         */
        APERTURE(10);

        public final int mId;
        TRANSITIONS(int id) {
            mId = id;
        }

        public static TRANSITIONS fromId(int id) {
            for (TRANSITIONS transition : TRANSITIONS.values()) {
                if (transition.mId == id) {
                    return transition;
                }
            }
            return null;
        }
    }

    /**
     * Method that return the next type of transition to apply the picture.
     *
     * @return TRANSITIONS The next type of  transition to apply
     */
    public static TRANSITIONS getNextTypeOfTransition(Context context) {
        // Get a transition based on the user preference
        TRANSITIONS nextTransition;
        TRANSITIONS[] transitions = Preferences.General.Transitions.toTransitions(
                        Preferences.General.Transitions.getSelectedTransitions(context));
        if (transitions.length == 0) {
            // All the availables except the NO_TRANSITION
            TRANSITIONS[] values = TRANSITIONS.values();
            transitions = new TRANSITIONS[values.length - 1];
            System.arraycopy(values, 1, transitions, 0, transitions.length);
        }

        // Get a random transition between all the selected or availables
        int low = 0;
        int high = transitions.length - 1;
        int pos = Utils.getNextRandom(low, high);
        nextTransition = transitions[pos];

        // Select the transition if is available
        if (nextTransition.compareTo(TRANSITIONS.SWAP) == 0) {
            return TRANSITIONS.SWAP;
        } else if (nextTransition.compareTo(TRANSITIONS.FADE) == 0) {
            return TRANSITIONS.FADE;
        } else if (nextTransition.compareTo(TRANSITIONS.TRANSLATE) == 0) {
            return TRANSITIONS.TRANSLATE;
        } else if (nextTransition.compareTo(TRANSITIONS.FLIP) == 0) {
            return TRANSITIONS.FLIP;
        } else if (nextTransition.compareTo(TRANSITIONS.WINDOW) == 0) {
            return TRANSITIONS.WINDOW;
        } else if (nextTransition.compareTo(TRANSITIONS.CUBE) == 0) {
            return TRANSITIONS.CUBE;
        } else if (nextTransition.compareTo(TRANSITIONS.BLUR) == 0) {
            return TRANSITIONS.BLUR;
        } else if (nextTransition.compareTo(TRANSITIONS.VERTIGO) == 0) {
            return TRANSITIONS.VERTIGO;
        } else if (nextTransition.compareTo(TRANSITIONS.MIX) == 0) {
            return TRANSITIONS.MIX;
        } else if (nextTransition.compareTo(TRANSITIONS.APERTURE) == 0) {
            return TRANSITIONS.APERTURE;
        }
        return TRANSITIONS.NO_TRANSITION;
    }

    /**
     * Method that creates a new transition.
     *
     * @param ctx The current context
     * @param tm The texture manager
     * @param type The type of transition
     * @return Transition The next transition to apply
     */
    public static Transition createTransition(Context ctx, TextureManager tm, TRANSITIONS type) {
        if (type.compareTo(TRANSITIONS.SWAP) == 0) {
            return new SwapTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.FADE) == 0) {
            return new FadeTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.TRANSLATE) == 0) {
            return new TranslateTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.FLIP) == 0) {
            return new FlipTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.WINDOW) == 0) {
            return new WindowTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.CUBE) == 0) {
            return new CubeTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.BLUR) == 0) {
            return new BlurTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.VERTIGO) == 0) {
            return new VertigoTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.MIX) == 0) {
            return new MixTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.APERTURE) == 0) {
            return new ApertureTransition(ctx, tm);
        }
        return new NullTransition(ctx, tm);
    }
}
