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
import org.cyanogenmod.wallpapers.photophase.utils.Utils;

import java.util.Arrays;
import java.util.List;


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
        NO_TRANSITION,
        /**
         * @see CubeTransition
         */
        CUBE,
        /**
         * @see FadeTransition
         */
        FADE,
        /**
         * @see FlipTransition
         */
        FLIP,
        /**
         * @see SwapTransition
         */
        SWAP,
        /**
         * @see TranslateTransition
         */
        TRANSLATION,
        /**
         * @see WindowTransition
         */
        WINDOW;

        /**
         * Method that returns the transition from its ordinal position
         *
         * @param ordinal The ordinal position
         * @return TRANSITIONS The transition or null if wasn't found
         */
        public static TRANSITIONS fromOrdinal(int ordinal) {
            for (TRANSITIONS transition : TRANSITIONS.values()) {
                if (transition.ordinal() == ordinal) {
                    return transition;
                }
            }
            return null;
        }

        /**
         * Method that the returns an array with all the valid transitions (NO_TRANSITION is not
         * a valid one).
         *
         * @return TRANSITIONS[] The valid transitions
         */
        public static TRANSITIONS[] getValidTranstions() {
            TRANSITIONS[] src = TRANSITIONS.values();
            TRANSITIONS[] dst = new TRANSITIONS[src.length-1];
            System.arraycopy(src, 1, dst, 0, src.length-1);
            return dst;
        }
    }

    /**
     * Method that return the next type of transition to apply the picture.
     *
     * @param frame The frame which the translation will be applied to
     * @return TRANSITIONS The next type of  transition to apply
     */
    public static TRANSITIONS getNextTypeOfTransition(PhotoFrame frame) {
        // Get a transition based on the user preference
        List<TRANSITIONS> transitions =
                Arrays.asList(Preferences.General.Transitions.getTransitionTypes());
        TRANSITIONS nextTransition = null;
        if (transitions.size() > 0) {
            int low = 0;
            int high = transitions.size() - 1;
            int pos = Utils.getNextRandom(low, high);
            nextTransition = transitions.get(pos);
        }
        if (nextTransition == null) {
            return TRANSITIONS.NO_TRANSITION;
        }

        // Select the transition if is available
        if (nextTransition.compareTo(TRANSITIONS.SWAP) == 0) {
            return TRANSITIONS.SWAP;
        } else if (nextTransition.compareTo(TRANSITIONS.FADE) == 0) {
            return TRANSITIONS.FADE;
        } else if (nextTransition.compareTo(TRANSITIONS.TRANSLATION) == 0) {
            return TRANSITIONS.TRANSLATION;
        } else if (nextTransition.compareTo(TRANSITIONS.FLIP) == 0) {
            return TRANSITIONS.FLIP;
        } else if (nextTransition.compareTo(TRANSITIONS.WINDOW) == 0) {
            return TRANSITIONS.WINDOW;
        } else if (nextTransition.compareTo(TRANSITIONS.CUBE) == 0) {
            return TRANSITIONS.CUBE;
        }
        return TRANSITIONS.NO_TRANSITION;
    }

    /**
     * Method that creates a new transition.
     *
     * @param ctx The current context
     * @param tm The texture manager
     * @param type The type of transition
     * @param frame The frame which the translation will be applied to
     * @return Transition The next transition to apply
     */
    public static Transition createTransition(
            Context ctx, TextureManager tm, TRANSITIONS type, PhotoFrame frame) {
        if (type.compareTo(TRANSITIONS.SWAP) == 0) {
            return new SwapTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.FADE) == 0) {
            return new FadeTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.TRANSLATION) == 0) {
            return new TranslateTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.FLIP) == 0) {
            return new FlipTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.WINDOW) == 0) {
            return new WindowTransition(ctx, tm);
        } else if (type.compareTo(TRANSITIONS.CUBE) == 0) {
            return new CubeTransition(ctx, tm);
        }
        return new NullTransition(ctx, tm);
    }
}
