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

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.model.Disposition;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences;
import com.ruesga.android.wallpapers.photophase.utils.DispositionUtil;

import java.util.List;

/**
 * A fragment class that allow to choose the layout disposition of the wallpaper for portrait
 * screen.
 */
public class PortraitDispositionFragment extends DispositionFragment {

    /**
     * Constructor of <code>PortraitDispositionFragment</code>
     */
    public PortraitDispositionFragment() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Disposition> getCurrentDispositions() {
        return Preferences.Layout.getPortraitDisposition(getActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Disposition> getDefaultDispositions() {
        return DispositionUtil.toDispositions(
                Preferences.Layout.DEFAULT_PORTRAIT_DISPOSITION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDispositionsTemplates() {
        return getActivity().getResources().getStringArray(R.array.portrait_disposition_templates);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveDispositions(List<Disposition> dispositions) {
        Preferences.Layout.setPortraitDisposition(getActivity(), dispositions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<Disposition>> getUserDispositions() {
        return PreferencesProvider.Preferences.Layout.getPortraitUserDispositions(getActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveUserDisposition(List<Disposition> disposition) {
        List<List<Disposition>> dispositions =
                PreferencesProvider.Preferences.Layout.getPortraitUserDispositions(getActivity());
        dispositions.add(disposition);
        PreferencesProvider.Preferences.Layout.setPortraitUserDispositions(getActivity(), dispositions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteUserDisposition(int position) {
        List<List<Disposition>> dispositions =
                PreferencesProvider.Preferences.Layout.getPortraitUserDispositions(getActivity());
        dispositions.remove(position);
        PreferencesProvider.Preferences.Layout.setPortraitUserDispositions(getActivity(), dispositions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRows() {
        return Preferences.Layout.getRows(getActivity());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCols() {
        return Preferences.Layout.getCols(getActivity());
    }
}
