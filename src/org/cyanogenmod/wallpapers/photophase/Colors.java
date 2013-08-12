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

import android.content.Context;
import android.content.res.Resources;

import org.cyanogenmod.wallpapers.photophase.utils.GLESUtil.GLColor;
import org.cyanogenmod.wallpapers.photophase.preferences.PreferencesProvider.Preferences;

/**
 * A class that defines some wallpaper GLColor colors.
 */
public class Colors {

    private static GLColor sBackground = new GLColor(0);
    private static GLColor sOverlay = new GLColor(0);

    /**
     * This method should be called on initialization for load the preferences color
     */
    public static void register(Context ctx) {
        Resources res = ctx.getResources();
        sBackground = Preferences.General.getBackgroundColor();
        sOverlay = new GLColor(res.getColor(R.color.wallpaper_overlay_color));
    }

    public static GLColor getBackground() {
        return sBackground;
    }

    public static void setBackground(GLColor background) {
        Colors.sBackground = background;
    }

    public static GLColor getOverlay() {
        return sOverlay;
    }

    public static void setOverlay(GLColor overlay) {
        Colors.sOverlay = overlay;
    }

}
