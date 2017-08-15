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

package com.ruesga.android.wallpapers.photophase.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;

/**
 * A fragment class with all the media settings
 */
public class MediaPreferenceFragment extends PreferenceFragment {

    private static final String TAG = "MediaPreferenceFragment";

    private static final boolean DEBUG = false;

    private ListPreference mRefreshInterval;
    private SwitchPreference mRememberLastMediaShown;

    private boolean mMediaIntevalChangedFlag;
    private boolean mEmptyTextureQueueFlag;

    private final OnPreferenceChangeListener mOnChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object newValue) {
            String key = preference.getKey();
            if (DEBUG) Log.d(TAG, "Preference changed: " + key + "=" + newValue);
            if (key.compareTo("ui_media_refresh_interval") == 0) {
                setRefreshIntervalSummary(Integer.valueOf(String.valueOf(newValue)));
                mMediaIntevalChangedFlag = true;
            } else if (key.compareTo("ui_media_random") == 0) {
                mEmptyTextureQueueFlag = true;

                boolean random = (Boolean) newValue;
                if (random) {
                    mRememberLastMediaShown.setChecked(false);
                }
                mRememberLastMediaShown.setEnabled(!random);
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
        if (mMediaIntevalChangedFlag) {
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_MEDIA_INTERVAL_CHANGED, Boolean.TRUE);
        }
        if (mEmptyTextureQueueFlag) {
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_EMPTY_TEXTURE_QUEUE, Boolean.TRUE);
        }
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        }
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

        // Add the preferences
        addPreferencesFromResource(R.xml.preferences_media);

        mRefreshInterval = (ListPreference)findPreference("ui_media_refresh_interval");
        setRefreshIntervalSummary(Preferences.Media.getRefreshFrequency(getActivity()));
        mRefreshInterval.setOnPreferenceChangeListener(mOnChangeListener);

        Preference refreshNow = findPreference("ui_media_refresh_now");
        refreshNow.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Request a refresh of the media data
                Intent intent = new Intent(PreferencesProvider.ACTION_SETTINGS_CHANGED);
                intent.putExtra(PreferencesProvider.EXTRA_FLAG_MEDIA_RELOAD, Boolean.TRUE);
                intent.putExtra(PreferencesProvider.EXTRA_ACTION_MEDIA_USER_RELOAD_REQUEST, Boolean.TRUE);
                if (getActivity() != null) {
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                }
                return true;
            }
        });

        SwitchPreference random = (SwitchPreference) findPreference("ui_media_random");
        random.setOnPreferenceChangeListener(mOnChangeListener);

        mRememberLastMediaShown
                = (SwitchPreference) findPreference("ui_media_remember_last_media_show");
        boolean isRandom = Preferences.Media.isRandomSequence(getActivity());
        if (isRandom) {
            mRememberLastMediaShown.setChecked(false);
        }
        mRememberLastMediaShown.setEnabled(!isRandom);
    }

    /**
     * Method that set the refresh interval summary
     *
     * @param interval The interval value
     */
    private void setRefreshIntervalSummary(int interval) {
        String v = String.valueOf(interval);
        String[] labels = getResources().getStringArray(R.array.refresh_intervals_labels);
        String[] values = getResources().getStringArray(R.array.refresh_intervals_values);
        int cc = values.length;
        for (int i = 0; i < cc; i++) {
            if (values[i].compareTo(String.valueOf(v)) == 0) {
                v = labels[i];
                break;
            }
        }
        String summary =
                (interval == 0)
                ? getString(R.string.pref_media_settings_refresh_interval_disable)
                : getString(R.string.pref_media_settings_refresh_interval_summary, v);
        mRefreshInterval.setSummary(summary);
    }
}
