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

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.cyanogenmod.wallpapers.photophase.R;
import org.cyanogenmod.wallpapers.photophase.model.Disposition;

import java.util.List;

/**
 * An abstract fragment class that allow to choose the layout disposition of the wallpaper 
 */
public abstract class DispositionFragment extends PreferenceFragment {

    /**
     * Available modes for the {@link DispositionFragment} class.
     */
    public static enum DispositionModes {
        /**
         * Portrait screen
         */
        PORTRAIT,
        /**
         * Landscape screen
         */
        LANDSCAPE;
    }

    private final DispositionModes mMode;

    private List<Disposition> mOldDispositions;

    /**
     * Constructor of <code>DispositionFragment</code>
     * 
     * @param mode The mode of the disposition layout
     */
    public DispositionFragment(DispositionModes mode) {
        super();
        mMode = mode;
    }

    /**
     * Method that returns the current user preference for the disposition
     *
     * @return List<Disposition> The current user preference dispositions
     */
    public abstract List<Disposition> getUserDispositions();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Change the preference manager
        getPreferenceManager().setSharedPreferencesName(PreferencesProvider.PREFERENCES_FILE);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup v = (ViewGroup)inflater.inflate(R.layout.choose_disposition_fragment, container, false);
//        v.addView(createFrame());
        return v;
    }

    /**
     * Method that recreate
     * @param r
     * @return
     */
    private View createFrame(Rect r) {
        // Create a view with all the 
        View v = new View(getActivity());
        v.setBackgroundColor(getResources().getColor(R.color.wallpaper_background_color));
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(r.width(), r.height());
        params.leftMargin = r.top;
        params.topMargin = r.left;
        v.setLayoutParams(params);
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        return v;
    }
}
