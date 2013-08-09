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

package org.cyanogenmod.wallpapers.photophase;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;

/**
 * A helper class with useful methods for deal with android.
 */
public final class AndroidHelper {

    /**
     * Method that returns if the device is a tablet
     *
     * @param ctx The current context
     * @return boolean If device is a table
     */
    public static boolean isTablet(Context ctx) {
        Configuration configuration = ctx.getResources().getConfiguration();
        return (configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                    >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
    * Method that returns if an option menu has to be displayed
    *
    * @param ctx The current context
    * @return boolean If an option menu has to be displayed
    */
    public static boolean showOptionsMenu(Context ctx) {
        // Show overflow button?
        return !ViewConfiguration.get(ctx).hasPermanentMenuKey();
    }

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
     * This method converts device specific pixels to device independent pixels.
     *
     * @param ctx The current context
     * @param px A value in px (pixels) unit
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(Context ctx, float px) {
        Resources resources = ctx.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);
    }

    /**
     * Calculate the dimension of the status bar
     *
     * @param context The current context
     * @return The height of the status bar
     */
    public static int calculateStatusBarHeight(Context context) {
        // CyanogenMod specific featured (DO NOT RELAY IN INTERNAL VARS)
        boolean hiddenStatusBar =
                Settings.System.getInt(context.getContentResolver(), "expanded_desktop_state", 0) == 1 &&
                Settings.System.getInt(context.getContentResolver(), "expanded_desktop_style", 0) == 2;
        int result = 0;
        if (!hiddenStatusBar && !(context instanceof Activity)) {
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = context.getResources().getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }
}
