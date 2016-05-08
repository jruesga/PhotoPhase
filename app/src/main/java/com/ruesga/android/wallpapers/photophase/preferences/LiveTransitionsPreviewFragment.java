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

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.adapters.LivePreviewAdapter.LivePreviewCallback;
import com.ruesga.android.wallpapers.photophase.borders.Borders;
import com.ruesga.android.wallpapers.photophase.effects.Effects;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences.General;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.util.HashSet;
import java.util.Set;

public class LiveTransitionsPreviewFragment extends LivePreviewFragment {

    private final LivePreviewCallback mCallback = new LivePreviewCallback() {
        @Override
        public Set<String> getSelectedEntries() {
            Transitions.TRANSITIONS[] transitions = General.Transitions.toTransitions(
                    General.Transitions.getSelectedTransitions(getActivity()));
            Set<String> set = new HashSet<>(transitions.length);
            for (Transitions.TRANSITIONS transition : transitions) {
                set.add(String.valueOf(transition.mId));
            }
            return set;
        }

        @Override
        public void setSelectedEntries(Set<String> entries) {
            General.Transitions.setSelectedTransitions(getActivity(), entries);
        }

        @Override
        public Transitions.TRANSITIONS getTransitionForId(int id) {
            return Transitions.TRANSITIONS.fromId(id);
        }

        @Override
        public Effects.EFFECTS getEffectForId(int id) {
            return Effects.EFFECTS.NO_EFFECT;
        }

        @Override
        public Borders.BORDERS getBorderForId(int id) {
            return Borders.BORDERS.NO_BORDER;
        }

        @Override
        public boolean hasSettings(int id) {
            return false;
        }

        @Override
        public void configureSettings(int id, DiscreteSeekBar seekBar) {
        }

        @Override
        public void saveSettings(int id, int newVal) {
        }
    };

    @Override
    public int getLabels() {
        return R.array.transitions_labels;
    }

    @Override
    public int getEntries() {
        return R.array.transitions_values;
    }

    @Override
    public LivePreviewCallback getLivePreviewCallback() {
        return mCallback;
    }
}
