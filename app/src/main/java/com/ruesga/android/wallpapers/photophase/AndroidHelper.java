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

package com.ruesga.android.wallpapers.photophase;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;

/**
 * A helper class with useful methods for deal with android.
 */
public final class AndroidHelper {

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
     * Method that returns if the device is running jellybean or greater
     *
     * @return boolean true if is running jellybean or greater
     */
    public static boolean isJellyBeanOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Method that returns if the device is running kitkat or greater
     *
     * @return boolean true if is running kitkat or greater
     */
    public static boolean isKitKatOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * Method that returns if the device is running lollipop or greater
     *
     * @return boolean true if is running lollipop or greater
     */
    public static boolean isLollipopOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Method that returns if the device is running marshmallow or greater
     *
     * @return boolean true if is running marshmallow or greater
     */
    public static boolean isMarshmallowOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Calculate the dimension of the status bar
     *
     * @param context The current context
     * @return The height of the status bar
     */

    public static int calculateStatusBarHeight(Context context) {
        // CyanogenMod specific featured (DO NOT RELAY IN INTERNAL VARS)
        boolean hiddenStatusBar = Settings.System.getInt(context.getContentResolver(),
                "expanded_desktop_state", 0) == 1 && Settings.System.getInt(
                context.getContentResolver(), "expanded_desktop_style", 0) == 2;

        // On kitkat we can use the translucent bars to fill all the screen
        int result = 0;
        if (!isKitKatOrGreater() && !hiddenStatusBar && !(context instanceof Activity)) {
            int resourceId = context.getResources().getIdentifier(
                    "status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = context.getResources().getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }

    /**
     * Method that restart the wallpaper
     */
    public static void restartWallpaper() {
        // Restart the service
        Process.killProcess(Process.myPid());
    }

    @TargetApi(value=Build.VERSION_CODES.JELLY_BEAN)
    public static boolean hasReadExternalStoragePermissionGranted(Context context) {
        // We only are interested in MM permissions model. In other cases we explicit have
        // this permission granted by the system
        return !isMarshmallowOrGreater() || ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
    }

}
