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
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.cyanogenmod.wallpapers.photophase.R;
import org.cyanogenmod.wallpapers.photophase.model.Disposition;
import org.cyanogenmod.wallpapers.photophase.widgets.DispositionView;
import org.cyanogenmod.wallpapers.photophase.widgets.ResizeFrame;

import java.util.List;

/**
 * An abstract fragment class that allow to choose the layout disposition of the wallpaper.
 */
public abstract class DispositionFragment extends PreferenceFragment {

    private Runnable mRedraw = new Runnable() {
        @Override
        public void run() {
            if (getActivity().isDestroyed()) return;
            mDispositionView.setDispositions(getUserDispositions(), getCols(), getRows());
        }
    };

    DispositionView mDispositionView;

    /**
     * Constructor of <code>DispositionFragment</code>
     */
    public DispositionFragment() {
        super();
    }

    /**
     * Method that returns the current user preference for the disposition
     *
     * @return List<Disposition> The current user preference dispositions
     */
    public abstract List<Disposition> getUserDispositions();

    /**
     * Method that returns the number of rows to use
     *
     * @return int The number of rows
     */
    public abstract int getRows();

    /**
     * Method that returns the number of cols to use
     *
     * @return int The number of cols
     */
    public abstract int getCols();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Change the preference manager
        getPreferenceManager().setSharedPreferencesName(PreferencesProvider.PREFERENCES_FILE);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup v = (ViewGroup)inflater.inflate(R.layout.choose_disposition_fragment, container, false);
        mDispositionView = (DispositionView)v.findViewById(R.id.disposition_view);
        mDispositionView.setResizeFrame((ResizeFrame)v.findViewById(R.id.resize_frame));
        mDispositionView.post(mRedraw);
        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDispositionView != null) {
            mDispositionView.removeCallbacks(mRedraw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.accept_restore_preference, menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_ok:
                getActivity().finish();
                return true;
            case R.id.mnu_restore:
                restoreData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Method that restores the disposition view to the default state
     */
    private void restoreData() {
        //TODO Restore disposition
    }

}
