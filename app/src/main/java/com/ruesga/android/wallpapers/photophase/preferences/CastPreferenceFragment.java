/*
 * Copyright (C) 2016 Jorge Ruesga
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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.R;

import su.litvak.chromecast.api.v2.ChromeCast;

/**
 * A settings preference to configure casting options
 */
public class CastPreferenceFragment extends PreferenceFragment {

    private static final String TAG = "CastPrefFragment";

    private static final boolean DEBUG = false;

    private PreferenceCategory mDiscoveryCategory;
    private PreferenceCategory mSlideshowCategory;
    private PreferenceCategory mVisualizationCategory;
    private PreferenceCategory mUiCategory;

    private final OnPreferenceChangeListener mOnChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object newValue) {
            String key = preference.getKey();
            if (DEBUG) Log.d(TAG, "Preference changed: " + key + "=" + newValue);

            boolean isNeedSendConfiguration = false;
            if (key.compareTo("cast_ui_show_time") == 0
                    || key.compareTo("cast_ui_show_weather") == 0
                    || key.compareTo("cast_ui_show_logo") == 0
                    || key.compareTo("cast_ui_show_track") == 0
                    || key.compareTo("cast_aspect_ratio") == 0
                    || key.compareTo("cast_blurred_background") == 0
                    || key.compareTo("cast_slideshow_time") == 0
                    || key.compareTo("cast_slideshow_repeat") == 0
                    || key.compareTo("cast_slideshow_shuffle") == 0) {
                isNeedSendConfiguration = true;
            } else if (key.compareTo("cast_enabled") == 0) {
                setEnabledDependencies((Boolean) newValue);
                isNeedSendConfiguration = true;
            }

            if (isNeedSendConfiguration) {
                sendConfigurationChangedEvent(key);
            }

            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String formatSeconds = getString(R.string.format_seconds);

        // Change the preference manager
        getPreferenceManager().setSharedPreferencesName(PreferencesProvider.PREFERENCES_FILE);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        // Add the preferences
        addPreferencesFromResource(R.xml.preferences_cast);

        mDiscoveryCategory = (PreferenceCategory) findPreference("category_cast_discovery");
        mSlideshowCategory = (PreferenceCategory) findPreference("category_cast_slideshow");
        mVisualizationCategory = (PreferenceCategory) findPreference("category_cast_visualization");
        mUiCategory = (PreferenceCategory) findPreference("category_cast_ui");

        // Enabled
        CheckBoxPreference castEnabled = (CheckBoxPreference) findPreference("cast_enabled");
        castEnabled.setOnPreferenceChangeListener(mOnChangeListener);

        //- Discovery settings
        final Preference lastConnectedDevice = findPreference("cast_last_connected_device");
        ChromeCast device = PreferencesProvider.Preferences.Cast.getLastConnectedDevice(getActivity());
        if (device == null) {
            lastConnectedDevice.setSummary(R.string.pref_cast_no_connected_device);
            lastConnectedDevice.setSelectable(false);
        } else {
            lastConnectedDevice.setSummary(
                    getActivity().getString(R.string.pref_cast_connected_device,
                            device.getName(),
                            device.getAddress() + ":" + device.getPort()));
            lastConnectedDevice.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.pref_cast_last_connected_device_title)
                            .setMessage(R.string.pref_cast_clear_connected_device)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    PreferencesProvider.Preferences.Cast.setLastConnectedDevice(
                                            getActivity(), null);
                                    PreferencesProvider.Preferences.Cast.setLastDiscoveredDevices(
                                            getActivity(), null);
                                    lastConnectedDevice.setSummary(R.string.pref_cast_no_connected_device);
                                    lastConnectedDevice.setSelectable(false);
                                    dialogInterface.dismiss();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return true;
                }
            });
        }

        DiscreteSeekBarProgressPreference discoveryTime =
                (DiscreteSeekBarProgressPreference) findPreference("cast_discovery_time");
        discoveryTime.setFormat(formatSeconds);
        discoveryTime.setOnPreferenceChangeListener(mOnChangeListener);
        discoveryTime.setMin(8);
        discoveryTime.setMax(15);

        //- Slideshow settings
        DiscreteSeekBarProgressPreference slideshowTime =
                (DiscreteSeekBarProgressPreference) findPreference("cast_slideshow_time");
        slideshowTime.setFormat(formatSeconds);
        slideshowTime.setOnPreferenceChangeListener(mOnChangeListener);
        slideshowTime.setMin(20);
        slideshowTime.setMax(120);
        CheckBoxPreference slideShowRepeat = (CheckBoxPreference) findPreference("cast_slideshow_repeat");
        slideShowRepeat.setOnPreferenceChangeListener(mOnChangeListener);
        CheckBoxPreference slideShowShuffle = (CheckBoxPreference) findPreference("cast_slideshow_shuffle");
        slideShowShuffle.setOnPreferenceChangeListener(mOnChangeListener);

        //- Visualization settings
        CheckBoxPreference aspectRatio =
                (CheckBoxPreference) findPreference("cast_aspect_ratio");
        aspectRatio.setOnPreferenceChangeListener(mOnChangeListener);
        CheckBoxPreference blurredBackground =
                (CheckBoxPreference) findPreference("cast_blurred_background");
        blurredBackground.setOnPreferenceChangeListener(mOnChangeListener);

        //- Ui settings
        CheckBoxPreference showTime = (CheckBoxPreference) findPreference("cast_ui_show_time");
        showTime.setOnPreferenceChangeListener(mOnChangeListener);
        CheckBoxPreference showWeather = (CheckBoxPreference) findPreference("cast_ui_show_weather");
        showWeather.setOnPreferenceChangeListener(mOnChangeListener);
        CheckBoxPreference showLogo = (CheckBoxPreference) findPreference("cast_ui_show_logo");
        showLogo.setOnPreferenceChangeListener(mOnChangeListener);
        CheckBoxPreference showTrack = (CheckBoxPreference) findPreference("cast_ui_show_track");
        showTrack.setOnPreferenceChangeListener(mOnChangeListener);

        setEnabledDependencies(PreferencesProvider.Preferences.Cast.isEnabled(getActivity()));
    }

    private void sendConfigurationChangedEvent(String key) {
        Intent intent = new Intent(PreferencesProvider.ACTION_SETTINGS_CHANGED);
        intent.putExtra(PreferencesProvider.EXTRA_FLAG_CAST_CONFIGURATION_CHANGE, Boolean.TRUE);
        intent.putExtra(PreferencesProvider.EXTRA_PREF_KEY, key);
        getActivity().sendBroadcast(intent);
    }

    private void setEnabledDependencies(boolean state) {
        mDiscoveryCategory.setEnabled(state);
        mSlideshowCategory.setEnabled(state);
        mVisualizationCategory.setEnabled(state);
        mUiCategory.setEnabled(state);
    }
}
