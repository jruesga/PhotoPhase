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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.preferences.DiscreteSeekBarProgressPreference.OnDisplayProgress;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLColor;

import java.util.Set;

/**
 * A fragment class with all the general settings
 */
public class GeneralPreferenceFragment extends PreferenceFragment {

    private static final String TAG = "GeneralPrefFragment";

    private static final boolean DEBUG = false;

    private ListPreference mTouchActions;
    private MultiSelectListPreference mTransitionsTypes;
    private DiscreteSeekBarProgressPreference mTransitionsInterval;
    private MultiSelectListPreference mEffectsTypes;

    private boolean mRedrawFlag;
    private boolean mEmptyTextureQueueFlag;

    private final OnPreferenceChangeListener mOnChangeListener = new OnPreferenceChangeListener() {
        @Override
        @SuppressWarnings({"unchecked", "synthetic-access"})
        public boolean onPreferenceChange(final Preference preference, Object newValue) {
            String key = preference.getKey();
            if (DEBUG) Log.d(TAG, "Preference changed: " + key + "=" + newValue);
            if (key.compareTo("ui_wallpaper_dim") == 0) {
                mRedrawFlag = true;
            } else if (key.compareTo("ui_background_color") == 0) {
                mRedrawFlag = true;
                Colors.setBackground(new GLColor((Integer) newValue));
            } else if (key.compareTo("ui_fix_aspect_ratio") == 0) {
                mRedrawFlag = true;
            } else if (key.compareTo("ui_transition_types") == 0) {
                mRedrawFlag = true;
                updateTransitionTypeSummary((Set<String>) newValue);
            } else if (key.compareTo("ui_transition_interval") == 0) {
                mRedrawFlag = true;
            } else if (key.compareTo("ui_effect_types") == 0) {
                mRedrawFlag = true;
                mEmptyTextureQueueFlag = true;
                updateEffectTypeSummary((Set<String>) newValue);
            } else if (key.compareTo("ui_touch_action") == 0) {
                updateTouchActionSummary((String) newValue);
            } else if (key.compareTo("app_shortcut") == 0) {
                setAppShortcutState((Boolean) newValue);
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

        final String formatDisabled = getString(R.string.format_disabled);
        final String formatSeconds = getString(R.string.format_seconds);
        final String formatMinutes = getString(R.string.format_minutes);
        final String formatHours = getString(R.string.format_hours);
        final String formatDays = getString(R.string.format_days);
        final String formatDim = getString(R.string.format_dim);

        // Change the preference manager
        getPreferenceManager().setSharedPreferencesName(PreferencesProvider.PREFERENCES_FILE);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        final Resources res = getActivity().getResources();

        // Add the preferences
        addPreferencesFromResource(R.xml.preferences_general);

        SwitchPreference appShortcut = (SwitchPreference) findPreference("app_shortcut");
        appShortcut.setChecked(isAppShortcutEnabled());
        appShortcut.setOnPreferenceChangeListener(mOnChangeListener);

        DiscreteSeekBarProgressPreference wallpaperDim =
                (DiscreteSeekBarProgressPreference) findPreference("ui_wallpaper_dim");
        wallpaperDim.setFormat(formatDim);
        wallpaperDim.setOnPreferenceChangeListener(mOnChangeListener);
        // A excessive dim will just display a black screen. Restrict the max value to
        // a proper translucent value
        wallpaperDim.setMax(70);

        ColorPickerPreference backgroundColor =
                (ColorPickerPreference) findPreference("ui_background_color");
        backgroundColor.setOnPreferenceChangeListener(mOnChangeListener);

        mTouchActions = (ListPreference)findPreference("ui_touch_action");
        mTouchActions.setOnPreferenceChangeListener(mOnChangeListener);
        updateTouchActionSummary(mTouchActions.getValue());

        CheckBoxPreference fixAspectRatio =
                (CheckBoxPreference) findPreference("ui_fix_aspect_ratio");
        fixAspectRatio.setOnPreferenceChangeListener(mOnChangeListener);

        mTransitionsTypes = (MultiSelectListPreference)findPreference("ui_transition_types");
        mTransitionsTypes.setOnPreferenceChangeListener(mOnChangeListener);
        updateTransitionTypeSummary(mTransitionsTypes.getValues());

        final int[] transitionsIntervals = res.getIntArray(R.array.transitions_intervals_values);
        mTransitionsInterval =
                (DiscreteSeekBarProgressPreference)findPreference("ui_transition_interval");
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
                int interval = transitionsIntervals[progress];
                if (interval == 0) {
                    mTransitionsInterval.setFormat(formatDisabled);
                    return null;
                } else if (interval < 60000) {
                    // Seconds
                    mTransitionsInterval.setFormat(formatSeconds);
                    return String.valueOf(interval / 1000);
                } else if (interval < 3600000) {
                    // Minutes
                    mTransitionsInterval.setFormat(formatMinutes);
                    return String.valueOf(interval / 1000 / 60);
                } else if (interval < 86400000) {
                    // Hours
                    mTransitionsInterval.setFormat(formatHours);
                    return String.valueOf(interval / 1000 / 60 / 60);
                }

                // Days
                mTransitionsInterval.setFormat(formatDays);
                return String.valueOf(interval / 1000 / 60 / 60 / 24);
            }
        });
        mTransitionsInterval.setShowPopUpIndicator(false);
        mTransitionsInterval.setOnPreferenceChangeListener(mOnChangeListener);

        mEffectsTypes = (MultiSelectListPreference)findPreference("ui_effect_types");
        mEffectsTypes.setOnPreferenceChangeListener(mOnChangeListener);
        updateEffectTypeSummary(mEffectsTypes.getValues());
    }

    private void updateTouchActionSummary(String value) {
        int selectionIndex = mTouchActions.findIndexOfValue(value);
        String[] summaries = getResources().getStringArray(R.array.touch_actions_summaries);
        mTouchActions.setSummary(getString(R.string.pref_general_touch_action_summary_format,
                summaries[selectionIndex]));
    }

    private void updateTransitionTypeSummary(Set<String> selected) {
        CharSequence summary = getString(R.string.pref_general_transitions_types_summary_format,
                selected.size(),
                mTransitionsTypes.getEntries().length);
        mTransitionsTypes.setSummary(summary);
    }

    private void updateEffectTypeSummary(Set<String> selected) {
        CharSequence summary = getString(R.string.pref_general_effects_types_summary_format,
                selected.size(),
                mEffectsTypes.getEntries().length);
        mEffectsTypes.setSummary(summary);
    }

    private boolean isAppShortcutEnabled() {
        final Context ctx = getActivity();
        PackageManager pm = ctx.getPackageManager();
        ComponentName name = new ComponentName(
                ctx.getPackageName(), ctx.getPackageName() + ".PhotoPhasePreferences");
        int state = pm.getComponentEnabledSetting(name);
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                && state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
    }

    private void setAppShortcutState(boolean enabled) {
        final Context ctx = getActivity();
        PackageManager pm = ctx.getPackageManager();
        ComponentName name = new ComponentName(
                ctx.getPackageName(), ctx.getPackageName() + ".PhotoPhasePreferences");
        pm.setComponentEnabledSetting(
                name,
                enabled
                        ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}

