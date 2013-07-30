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

package org.cyanogenmod.wallpapers.photophase.preferences;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import org.cyanogenmod.wallpapers.photophase.model.Disposition;

import java.util.List;

/**
 * A fragment class that allow to choose the layout disposition of the wallpaper for landscape
 * screen. 
 */
public class LandscapeDispositionFragment extends DispositionFragment {

    /**
     * Constructor of <code>LandscapeDispositionFragment</code>
     */
    public LandscapeDispositionFragment() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Disposition> getUserDispositions() {
        return PreferencesProvider.Preferences.Layout.getLandscapeDisposition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRows() {
        // inverted
        return PreferencesProvider.Preferences.Layout.getCols();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCols() {
        // inverted
        return PreferencesProvider.Preferences.Layout.getRows();
    }
}
