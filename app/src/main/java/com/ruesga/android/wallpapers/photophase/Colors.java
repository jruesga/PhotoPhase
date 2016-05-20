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

import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLColor;

/**
 * A class that defines some wallpaper GLColor colors.
 */
public class Colors {

    private GLColor mBackground = new GLColor(0);
    private GLColor mBorder = new GLColor(0);
    private GLColor mOverlay = new GLColor(0);

    private static Colors sInstance;

    public synchronized static Colors getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new Colors();
            sInstance.update(ctx);
        }
        return sInstance;
    }

    private void update(Context ctx) {
        mBackground = Preferences.General.getBackgroundColor(ctx);
        mBorder = Preferences.General.Borders.getBorderColor(ctx);
        mOverlay = new GLColor(ContextCompat.getColor(ctx, R.color.wallpaper_overlay_color));
    }

    public GLColor getBackground() {
        return mBackground;
    }

    public void setBackground(GLColor background) {
        mBackground = background;
    }

    public GLColor getBorder() {
        return mBorder;
    }

    public void setBorder(GLColor border) {
        mBorder = border;
    }

    public GLColor getOverlay() {
        return mOverlay;
    }
}
