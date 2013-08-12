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
import org.cyanogenmod.wallpapers.photophase.widgets.DispositionView.OnFrameSelectedListener;
import org.cyanogenmod.wallpapers.photophase.widgets.ResizeFrame;

import java.util.List;

/**
 * An abstract fragment class that allow to choose the layout disposition of the wallpaper.
 */
public abstract class DispositionFragment
    extends PreferenceFragment implements OnFrameSelectedListener {

    private Runnable mRedraw = new Runnable() {
        @Override
        public void run() {
            if (getActivity() == null) return;
            try {
                mDispositionView.setDispositions(getUserDispositions(), getCols(), getRows());
            } catch (Exception ex) {
                // Ignored
            }
        }
    };

    /*package*/ DispositionView mDispositionView;

    private MenuItem mDeleteMenu;

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
     * Method that returns the default preference for the disposition
     *
     * @return List<Disposition> The default preference dispositions
     */
    public abstract List<Disposition> getDefaultDispositions();

    /**
     * Method that request to save the dispositions
     *
     * @param dispositions The dispositions to save
     */
    public abstract void saveDispositions(List<Disposition> dispositions);

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
        mDispositionView.setOnFrameSelectedListener(this);
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
            if (mDispositionView.isChanged()) {
                saveDispositions(mDispositionView.getDispositions());
            }
        }

        // Notify that the settings was changed
        Intent intent = new Intent(PreferencesProvider.ACTION_SETTINGS_CHANGED);
        if (mDispositionView.isChanged()) {
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_REDRAW, Boolean.TRUE);
            intent.putExtra(PreferencesProvider.EXTRA_FLAG_RECREATE_WORLD, Boolean.TRUE);
        }
        getActivity().sendBroadcast(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.dispositions, menu);
        mDeleteMenu = menu.findItem(R.id.mnu_delete);
        if (mDeleteMenu != null) {
            mDeleteMenu.setVisible(false);
        }
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
            case R.id.mnu_delete:
                deleteFrame();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Method that restores the disposition view to the default state
     */
    private void restoreData() {
        mDispositionView.setDispositions(getUserDispositions(), getCols(), getRows());
    }

    /**
     * Method that restores the disposition view to the default state
     */
    private void deleteFrame() {
        mDispositionView.deleteCurrentFrame();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFrameSelectedListener(View v) {
        if (mDeleteMenu != null) {
            mDeleteMenu.setVisible(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFrameUnselectedListener() {
        if (mDeleteMenu != null) {
            mDeleteMenu.setVisible(false);
        }
    }
}
