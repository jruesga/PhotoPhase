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
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;

import org.cyanogenmod.wallpapers.photophase.Colors;
import org.cyanogenmod.wallpapers.photophase.GLESUtil.GLColor;
import org.cyanogenmod.wallpapers.photophase.R;
import org.cyanogenmod.wallpapers.photophase.preferences.SeekBarProgressPreference.OnDisplayProgress;
import org.cyanogenmod.wallpapers.photophase.widgets.ColorPickerPreference;

import java.text.DecimalFormat;

/**
 * A fragment class with all the general settings
 */
public class GeneralPreferenceFragment extends PreferenceFragment {

    private static final String TAG = "GeneralPreferenceFragment";

    private static final boolean DEBUG = true;

    private SeekBarProgressPreference mWallpaperDim;
    private ColorPickerPreference mBackgroundColor;
    private ListPreference mTouchActions;
    private ListPreference mTransitionsTypes;
    private SeekBarProgressPreference mTransitionsInterval;
    private MultiSelectListPreference mEffectsTypes;

    boolean mRedrawFlag;
    boolean mEmptyTextureQueueFlag;

    private final OnPreferenceChangeListener mOnChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object newValue) {
            String key = preference.getKey();
            if (DEBUG) Log.d(TAG, "Preference changed: " + key + "=" + newValue);
            if (key.compareTo("ui_wallpaper_dim") == 0) {
                mRedrawFlag = true;
            } else if (key.compareTo("ui_background_color") == 0) {
                mRedrawFlag = true;
                int color = ((Integer)newValue).intValue();
                Colors.setBackground(new GLColor(color));
            } else if (key.compareTo("ui_transition_types") == 0) {
                mRedrawFlag = true;
            } else if (key.compareTo("ui_transition_interval") == 0) {
                mRedrawFlag = true;
            } else if (key.compareTo("ui_effect_types") == 0) {
                mRedrawFlag = true;
                mEmptyTextureQueueFlag = true;
            }
            return true;
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Reload the settings
        PreferencesProvider.reload(getActivity());

        // Notify that the settings was changed
        Intent intent = new Intent(PreferencesProvider.ACTION_SETTINGS_CHANGED);
        if (mRedrawFlag) {
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_REDRAW, Boolean.TRUE);
        }
        if (mEmptyTextureQueueFlag) {
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_EMPTY_TEXTURE_QUEUE, Boolean.TRUE);
        }
        getActivity().sendBroadcast(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Change the preference manager
        getPreferenceManager().setSharedPreferencesName(PreferencesProvider.PREFERENCES_FILE);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        final DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(0);
        df.setMaximumIntegerDigits(1);

        // Add the preferences
        addPreferencesFromResource(R.xml.preferences_general);

        mWallpaperDim = (SeekBarProgressPreference)findPreference("ui_wallpaper_dim");
        mWallpaperDim.setFormat(getString(R.string.pref_general_settings_wallpaper_dim_format));
        mWallpaperDim.setOnPreferenceChangeListener(mOnChangeListener);

        mBackgroundColor = (ColorPickerPreference)findPreference("ui_background_color");
        mBackgroundColor.setOnPreferenceChangeListener(mOnChangeListener);

        mTouchActions = (ListPreference)findPreference("ui_touch_action");
        mTouchActions.setOnPreferenceChangeListener(mOnChangeListener);

        mTransitionsTypes = (ListPreference)findPreference("ui_transition_types");
        mTransitionsTypes.setOnPreferenceChangeListener(mOnChangeListener);

        mTransitionsInterval = (SeekBarProgressPreference)findPreference("ui_transition_interval");
        mTransitionsInterval.setFormat(getString(R.string.pref_general_transitions_interval_format));
        int max = PreferencesProvider.Preferences.General.Transitions.MAX_TRANSITION_INTERVAL;
        int min = PreferencesProvider.Preferences.General.Transitions.MIN_TRANSITION_INTERVAL;
        final int MAX = ((max - min) / 1000) * 2;
        mTransitionsInterval.setMax(MAX);
        mTransitionsInterval.setOnDisplayProgress(new OnDisplayProgress() {
            @Override
            public String onDisplayProgress(int progress) {
                return df.format((progress * 0.5) + 1);
            }
        });
        mTransitionsInterval.setOnPreferenceChangeListener(mOnChangeListener);

        mEffectsTypes = (MultiSelectListPreference)findPreference("ui_effect_types");
        mEffectsTypes.setOnPreferenceChangeListener(mOnChangeListener);
    }

}
