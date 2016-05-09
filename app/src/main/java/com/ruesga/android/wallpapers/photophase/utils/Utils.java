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

package com.ruesga.android.wallpapers.photophase.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.RectF;
import android.util.DisplayMetrics;

import java.util.Random;

/**
 * A helper class with utilities
 */
public class Utils {

    private static final Random sRandom = new Random();

    /**
     * This method converts dp unit to equivalent device specific value in pixels.
     *
     * @param ctx The current context
     * @param dp A value in dp (Device independent pixels) unit
     * @return float A float value to represent Pixels equivalent to dp according to device
     */
    public static float convertDpToPixel(Context ctx, float dp) {
        Resources resources = ctx.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    /**
     * Used to determine if the device is a tablet or not
     *
     * @param context The {@link Context} to use.
     * @return True if the device is a tablet, false otherwise.
     */
    public static boolean isTablet(final Context context) {
        final int layout = context.getResources().getConfiguration().screenLayout;
        return (layout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Method that converts a rect from a vertex data
     *
     * @param vertex The vertex array
     * @return RectF The rect data
     */
    public static RectF rectFromVertex(float[] vertex) {
        RectF rect = new RectF();
        rect.left = vertex[0];
        rect.top = vertex[7];
        rect.right = vertex[6];
        rect.bottom = vertex[1];
        return rect;
    }

    /**
     * Method that returns a random number between two numbers.
     *
     * @param low The low number
     * @param high The high number
     * @return int The random number
     */
    public static int getNextRandom(int low, int high) {
        return low + (int)(sRandom.nextDouble() * ((high - low) + 1));
    }
}