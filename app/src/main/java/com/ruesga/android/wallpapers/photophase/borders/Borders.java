/*
 * Copyright (C) 2016 Jorge Ruesga
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

package com.ruesga.android.wallpapers.photophase.borders;

import android.content.Context;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;

import com.ruesga.android.wallpapers.photophase.Colors;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that manages all the supported borders
 */
public class Borders {

    /**
     * Enumeration of the supported borders
     */
    public enum BORDERS {
        /**
         * @see BordersFactory#BORDER_NULL
         */
        NO_BORDER(0),
        /**
         * @see BordersFactory#BORDER_SIMPLE
         */
        SIMPLE(1),
        /**
         * @see BordersFactory#BORDER_ROUNDED
         */
        ROUNDED(2),
        /**
         * @see BordersFactory#BORDER_ROUNDED_SQUARES
         */
        ROUNDED_SQUARES(3),
        /**
         * @see BordersFactory#BORDER_HORIZONTAL_FILM
         */
        HORIZONTAL_FILM(4),
        /**
         * @see BordersFactory#BORDER_VERTICAL_FILM
         */
        VERTICAL_FILM(5),
        /**
         * @see BordersFactory#BORDER_ELEGANT
         */
        ELEGANT(6);

        public final int mId;
        BORDERS(int id) {
            mId = id;
        }

        public static BORDERS fromId(int id) {
            for (BORDERS border : BORDERS.values()) {
                if (border.mId == id) {
                    return border;
                }
            }
            return null;
        }
    }

    private final Map<BORDERS, Border> mCachedBorders;
    private final EffectContext mEffectContext;
    private final Context mContext;

    /**
     * Constructor of <code>Borders</code>
     *
     * @param effectContext The current effect context
     */
    public Borders(Context context, EffectContext effectContext) {
        super();
        mCachedBorders = new HashMap<>();
        mEffectContext = effectContext;
        mContext = context;
    }

    /**
     * Method that that release the cached data
     */
    public void release() {
        if (mCachedBorders != null) {
            for (Border border : mCachedBorders.values()) {
                border.release();
            }
            mCachedBorders.clear();
        }
    }

    /**
     * Method that return the next border to use with the picture.
     *
     * @return Border The next border to use or null if no need to apply any border
     */
    @SuppressWarnings("boxing")
    public Border getNextBorder() {
        // Get an effect based on the user preference
        BORDERS[] borders = Preferences.General.Borders.toBORDERS(
                Preferences.General.Borders.getSelectedBorders(mContext));
        BORDERS nextBorder = null;
        if (borders.length > 0) {
            int low = 0;
            int high = borders.length - 1;
            int pos = Utils.getNextRandom(low, high);
            nextBorder = borders[pos];
        }
        return getBorder(nextBorder);
    }

    public Border getBorder(BORDERS nextBorder) {
        EffectFactory effectFactory = mEffectContext.getFactory();
        Border border = null;

        // Ensure we apply at least an border (a null one)
        if (nextBorder == null) {
            nextBorder = BORDERS.NO_BORDER;
        }

        // The border was cached previously?
        if (mCachedBorders.containsKey(nextBorder)) {
            border = mCachedBorders.get(nextBorder);
            border.mColor = Colors.getBorder();
            border.mBgColor = Colors.getBackground();
            return border;
        }

        // Select the effect if is available
        if (nextBorder.compareTo(BORDERS.NO_BORDER) == 0) {
            if (EffectFactory.isEffectSupported(BordersFactory.BORDER_NULL)) {
                border = (Border) effectFactory.createEffect(BordersFactory.BORDER_NULL);
            }
        } else if (nextBorder.compareTo(BORDERS.SIMPLE) == 0) {
            if (EffectFactory.isEffectSupported(BordersFactory.BORDER_SIMPLE)) {
                border = (Border) effectFactory.createEffect(BordersFactory.BORDER_SIMPLE);
            }
        } else if (nextBorder.compareTo(BORDERS.ROUNDED) == 0) {
            if (EffectFactory.isEffectSupported(BordersFactory.BORDER_ROUNDED)) {
                border = (Border) effectFactory.createEffect(BordersFactory.BORDER_ROUNDED);
            }
        } else if (nextBorder.compareTo(BORDERS.ROUNDED_SQUARES) == 0) {
            if (EffectFactory.isEffectSupported(BordersFactory.BORDER_ROUNDED_SQUARES)) {
                border = (Border) effectFactory.createEffect(BordersFactory.BORDER_ROUNDED_SQUARES);
            }
        } else if (nextBorder.compareTo(BORDERS.HORIZONTAL_FILM) == 0) {
            if (EffectFactory.isEffectSupported(BordersFactory.BORDER_HORIZONTAL_FILM)) {
                border = (Border) effectFactory.createEffect(BordersFactory.BORDER_HORIZONTAL_FILM);
            }
        } else if (nextBorder.compareTo(BORDERS.VERTICAL_FILM) == 0) {
            if (EffectFactory.isEffectSupported(BordersFactory.BORDER_VERTICAL_FILM)) {
                border = (Border) effectFactory.createEffect(BordersFactory.BORDER_VERTICAL_FILM);
            }
        } else if (nextBorder.compareTo(BORDERS.ELEGANT) == 0) {
            if (EffectFactory.isEffectSupported(BordersFactory.BORDER_ELEGANT)) {
                border = (Border) effectFactory.createEffect(BordersFactory.BORDER_ELEGANT);
            }
        }

        // Instead of not to apply any border, just use one null border to follow the same
        // border model. This allow to use the same height when Border.apply is applied for all
        // the frames
        if (border == null && EffectFactory.isEffectSupported(BordersFactory.BORDER_NULL)) {
            border = (Border) effectFactory.createEffect(BordersFactory.BORDER_NULL);
            nextBorder = BORDERS.NO_BORDER;
        }

        // Set the color
        if (border != null) {
            border.mColor = Colors.getBorder();
            border.mBgColor = Colors.getBackground();

            // Cache the effects
            mCachedBorders.put(nextBorder, border);
        }
        return border;
    }
}
