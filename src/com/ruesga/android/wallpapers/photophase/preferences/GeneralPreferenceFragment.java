/*
 * Copyright (C) 2013 Jorge Ruesga
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

package com.ruesga.android.wallpapers.photophase.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.Colors;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLColor;
import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.preferences.SeekBarProgressPreference.OnDisplayProgress;
import com.ruesga.android.wallpapers.photophase.widgets.ColorPickerPreference;

/**
 * A fragment class with all the general settings
 */
public class GeneralPreferenceFragment extends PreferenceFragment {

    private static final String TAG = "GeneralPreferenceFragment";

    private static final boolean DEBUG = false;

    private SeekBarProgressPreference mWallpaperDim;
    private ColorPickerPreference mBackgroundColor;
    private ListPreference mTouchActions;
    private CheckBoxPreference mFixAspectRatio;
    private MultiSelectListPreference mTransitionsTypes;
    SeekBarProgressPreference mTransitionsInterval;
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
            } else if (key.compareTo("ui_fix_aspect_ratio") == 0) {
                mRedrawFlag = true;
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

        final String formatSeconds = getString(R.string.format_seconds);
        final String formatMinutes = getString(R.string.format_minutes);
        final String formatDim = getString(R.string.format_dim);

        // Change the preference manager
        getPreferenceManager().setSharedPreferencesName(PreferencesProvider.PREFERENCES_FILE);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        final Resources res = getActivity().getResources();

        // Add the preferences
        addPreferencesFromResource(R.xml.preferences_general);

        mWallpaperDim = (SeekBarProgressPreference)findPreference("ui_wallpaper_dim");
        mWallpaperDim.setFormat(formatDim);
        mWallpaperDim.setOnPreferenceChangeListener(mOnChangeListener);

        mBackgroundColor = (ColorPickerPreference)findPreference("ui_background_color");
        mBackgroundColor.setOnPreferenceChangeListener(mOnChangeListener);

        mTouchActions = (ListPreference)findPreference("ui_touch_action");
        mTouchActions.setOnPreferenceChangeListener(mOnChangeListener);

        mFixAspectRatio = (CheckBoxPreference)findPreference("ui_fix_aspect_ratio");
        mFixAspectRatio.setOnPreferenceChangeListener(mOnChangeListener);

        mTransitionsTypes = (MultiSelectListPreference)findPreference("ui_transition_types");
        mTransitionsTypes.setOnPreferenceChangeListener(mOnChangeListener);

        final int[] transitionsIntervals = res.getIntArray(R.array.transitions_intervals_values);
        mTransitionsInterval = (SeekBarProgressPreference)findPreference("ui_transition_interval");
        mTransitionsInterval.setFormat(getString(R.string.format_seconds));
        mTransitionsInterval.setMax(transitionsIntervals.length - 1);
        int transitionInterval = prefs.getInt("ui_transition_interval",
                Preferences.General.Transitions.DEFAULT_TRANSITION_INTERVAL_INDEX);
        if (transitionInterval > (transitionsIntervals.length - 1)) {
            mTransitionsInterval.setProgress(
                    Preferences.General.Transitions.DEFAULT_TRANSITION_INTERVAL_INDEX);
        }
        mTransitionsInterval.setOnDisplayProgress(new OnDisplayProgress() {
            @Override
            public String onDisplayProgress(int progress) {
                if (transitionsIntervals[progress] < 60000) {
                    // Seconds
                    mTransitionsInterval.setFormat(formatSeconds);
                    return String.valueOf(transitionsIntervals[progress] / 1000);
                }
                // Minutes
                mTransitionsInterval.setFormat(formatMinutes);
                return String.valueOf(transitionsIntervals[progress] / 1000 / 60);
            }
        });
        mTransitionsInterval.setOnPreferenceChangeListener(mOnChangeListener);

        mEffectsTypes = (MultiSelectListPreference)findPreference("ui_effect_types");
        mEffectsTypes.setOnPreferenceChangeListener(mOnChangeListener);
    }

}
