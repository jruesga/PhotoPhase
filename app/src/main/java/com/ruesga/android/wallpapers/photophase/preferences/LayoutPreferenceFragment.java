/*
 * Copyright (C) 2015 Jorge Ruesga
 * Copyright (C) 2014 The CyanogenMod Project
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
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.preferences.DiscreteSeekBarProgressPreference.OnDisplayProgress;

/**
 * A fragment class with the layout disposition
 */
public class LayoutPreferenceFragment extends PreferenceFragment {

    private static final String TAG = "LayoutPrefFragment";

    private static final boolean DEBUG = false;

    private DiscreteSeekBarProgressPreference mRandomDispositionsInterval;
    private Preference mPortraitDisposition;
    private Preference mLandscapeDisposition;

    private boolean mRedrawFlag;
    private boolean mDispositionIntervalFlag;

    private final OnPreferenceChangeListener mOnChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object newValue) {
            String key = preference.getKey();
            if (DEBUG) Log.d(TAG, "Preference changed: " + key + "=" + newValue);
            if (key.compareTo("ui_disposition_random") == 0) {
                boolean randomDispositions = (Boolean) newValue;
                mPortraitDisposition.setEnabled(!randomDispositions);
                mLandscapeDisposition.setEnabled(!randomDispositions);
                mRedrawFlag = true;
            } else if (key.compareTo("ui_disposition_random_interval") == 0) {
                mDispositionIntervalFlag = true;
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

        // Notify that the settings was changed
        Intent intent = new Intent(PreferencesProvider.ACTION_SETTINGS_CHANGED);
        if (mRedrawFlag) {
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_REDRAW, Boolean.TRUE);
        }
        if (mDispositionIntervalFlag) {
            int interval = Preferences.Layout.getRandomDispositionsInterval(getActivity());
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_DISPOSITION_INTERVAL_CHANGED, interval);
        }
        getActivity().sendBroadcast(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String formatOnRotation = getString(R.string.format_rotate_only);
        final String formatSeconds = getString(R.string.format_seconds);
        final String formatMinutes = getString(R.string.format_minutes);
        final String formatHours = getString(R.string.format_hours);
        final String formatDays = getString(R.string.format_days);

        // Change the preference manager
        getPreferenceManager().setSharedPreferencesName(PreferencesProvider.PREFERENCES_FILE);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        final Resources res = getActivity().getResources();

        // Add the preferences
        addPreferencesFromResource(R.xml.preferences_layout);

        // -- Random dispositions
        CheckBoxPreference randomDispositions =
                (CheckBoxPreference) findPreference("ui_disposition_random");
        randomDispositions.setOnPreferenceChangeListener(mOnChangeListener);

        // -- Interval
        final int[] randomDispositionsIntervals =
                res.getIntArray(R.array.random_dispositions_intervals_values);
        mRandomDispositionsInterval =
                (DiscreteSeekBarProgressPreference)findPreference("ui_disposition_random_interval");
        mRandomDispositionsInterval.setMax(randomDispositionsIntervals.length - 1);
        int transitionInterval = prefs.getInt("ui_disposition_random_interval",
                Preferences.Layout.DEFAULT_RANDOM_DISPOSITIONS_INTERVAL_INDEX);
        if (transitionInterval > (randomDispositionsIntervals.length - 1)) {
            mRandomDispositionsInterval.setProgress(
                    Preferences.Layout.DEFAULT_RANDOM_DISPOSITIONS_INTERVAL_INDEX);
        }
        mRandomDispositionsInterval.setOnDisplayProgress(new OnDisplayProgress() {
            @Override
            public String onDisplayProgress(int progress) {
                int interval = randomDispositionsIntervals[progress];
                if (interval == 0) {
                    // Disabled
                    mRandomDispositionsInterval.setFormat(formatOnRotation);
                    return null;
                } else if (interval < 60000) {
                    // Seconds
                    mRandomDispositionsInterval.setFormat(formatSeconds);
                    return String.valueOf(interval / 1000);
                } else if (interval < 3600000) {
                    // Minutes
                    mRandomDispositionsInterval.setFormat(formatMinutes);
                    return String.valueOf(interval / 1000 / 60);
                } else if (interval < 86400000) {
                    // Hours
                    mRandomDispositionsInterval.setFormat(formatHours);
                    return String.valueOf(interval / 1000 / 60 / 60);
                }
                // Days
                mRandomDispositionsInterval.setFormat(formatDays);
                return String.valueOf(interval / 1000 / 60 / 60 / 24);
            }
        });
        mRandomDispositionsInterval.setShowPopUpIndicator(false);
        mRandomDispositionsInterval.setOnPreferenceChangeListener(mOnChangeListener);

        // -- Portrait
        mPortraitDisposition = findPreference("ui_disposition_portrait");
        mPortraitDisposition.setEnabled(!Preferences.Layout.isRandomDispositions(getActivity()));

        // -- Landscape
        mLandscapeDisposition = findPreference("ui_disposition_landscape");
        mLandscapeDisposition.setEnabled(!Preferences.Layout.isRandomDispositions(getActivity()));
    }

    @Override
    public void onResume() {
        super.onResume();

        updateArrangementSummary(mPortraitDisposition,
                PreferencesProvider.Preferences.Layout.getPortraitDisposition(getActivity()).size(),
                R.string.disposition_orientation_portrait);
        updateArrangementSummary(mLandscapeDisposition,
                PreferencesProvider.Preferences.Layout.getLandscapeDisposition(getActivity()).size(),
                R.string.disposition_orientation_landscape);
    }

    private void updateArrangementSummary(Preference pref, int count, int orientationLabelResId) {
        String orientation = getString(orientationLabelResId);
        String summary = getResources().getQuantityString(
                R.plurals.pref_disposition_summary_format, count, count, orientation);
        pref.setSummary(summary);
    }
}
