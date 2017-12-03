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
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.adapters.DispositionAdapter;
import com.ruesga.android.wallpapers.photophase.model.Disposition;
import com.ruesga.android.wallpapers.photophase.model.Dispositions;
import com.ruesga.android.wallpapers.photophase.utils.DispositionUtil;
import com.ruesga.android.wallpapers.photophase.widgets.DispositionView;
import com.ruesga.android.wallpapers.photophase.widgets.DispositionView.OnFrameSelectedListener;
import com.ruesga.android.wallpapers.photophase.widgets.ResizeFrame;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract fragment class that allow to choose the layout disposition of the wallpaper.
 */
public abstract class DispositionFragment extends PreferenceFragment
        implements OnFrameSelectedListener, OnPageChangeListener {

    private ViewPager mPager;

    private DispositionAdapter mAdapter;
    private ResizeFrame mResizeFrame;
    private TextView mAdvise;
    private int mAdviseLines = -1;

    private DispositionView mCurrentDispositionView;
    private int mCurrentPage;
    private int mNumberOfTemplates;

    private MenuItem mRestoreMenu;
    private MenuItem mDeleteMenu;
    private MenuItem mSaveMenu;

    private boolean mOkPressed;

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
    public abstract List<Disposition> getCurrentDispositions();

    /**
     * Method that returns the default preference for the disposition
     *
     * @return List<Disposition> The default preference dispositions
     */
    public abstract List<Disposition> getDefaultDispositions();

    /**
     * Method that returns the system-defined dispositions templates
     *
     * @return String[] The system-defined dispositions templates
     */
    public abstract String[] getDispositionsTemplates();

    /**
     * Method that request to save the dispositions
     *
     * @param dispositions The dispositions to save
     */
    public abstract void saveDispositions(List<Disposition> dispositions);

    /**
     * Method that returns the user saved dispositions
     *
     * @return List<Disposition> The user saved dispositions
     */
    public abstract List<List<Disposition>> getUserDispositions();

    /**
     * Method that request to save a user saved disposition
     *
     * @param disposition The disposition to save
     */
    public abstract void saveUserDisposition(List<Disposition> disposition);

    /**
     * Method that request to delete a user saved disposition
     *
     * @param position The position to delete
     */
    public abstract void deleteUserDisposition(int position);

    /**
     * Method that returns the number of rows to use
     *
     * @return int The number of rows
     */
    public abstract int getRows();

    /**
     * Method that returns the number of columns to use
     *
     * @return int The number of columns
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

        mOkPressed = false;
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPager.removeOnPageChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup v = (ViewGroup)inflater.inflate(R.layout.choose_disposition_fragment,
                container, false);

        mCurrentPage = 0;
        mNumberOfTemplates = getDispositionsTemplates().length;

        mAdvise = v.findViewById(R.id.advise);
        mResizeFrame = v.findViewById(R.id.resize_frame);

        mAdapter = new DispositionAdapter(getActivity(), getAllDispositions(), mResizeFrame, this);
        mPager = v.findViewById(R.id.dispositions_pager);
        mPager.setAdapter(mAdapter);
        mPager.addOnPageChangeListener(this);
        mPager.setCurrentItem(0);

        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        // Saved ?
        if (mOkPressed) {
            boolean saved = false;

            if (mCurrentDispositionView == null) {
                mCurrentDispositionView = mAdapter.getView(0);
            }

            if (mCurrentDispositionView != null) {
                if (mCurrentPage != 0 || mCurrentDispositionView.isChanged()) {
                    saveDispositions(mCurrentDispositionView.getDispositions());
                    saved = true;
                }

                // Notify that the settings was changed
                Intent intent = new Intent(PreferencesProvider.ACTION_SETTINGS_CHANGED);
                if (saved) {
                    intent.putExtra(PreferencesProvider.EXTRA_FLAG_REDRAW, Boolean.TRUE);
                    intent.putExtra(PreferencesProvider.EXTRA_FLAG_RECREATE_WORLD, Boolean.TRUE);
                }
                if (getActivity() != null) {
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                }
            }
        }

        super.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.dispositions, menu);
        mRestoreMenu = menu.findItem(R.id.mnu_restore);
        mSaveMenu = menu.findItem(R.id.mnu_save);
        mDeleteMenu = menu.findItem(R.id.mnu_delete);
        mDeleteMenu.setVisible(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnu_ok:
                mOkPressed = true;
                if (getActivity() != null && !getActivity().isTaskRoot()) {
                    getActivity().finish();
                } else {
                    getFragmentManager().popBackStack();
                }
                return super.onOptionsItemSelected(item);
            case R.id.mnu_restore:
                restoreData();
                return true;
            case R.id.mnu_save:
                addCurrentToSaved();
                return true;
            case R.id.mnu_delete:
                if (mPager.getCurrentItem() > 0) {
                    deleteCurrentSaved();
                } else {
                    deleteFrame();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Method that restores the disposition view to the default state
     */
    private void restoreData() {
        if (mCurrentDispositionView == null) {
            mCurrentDispositionView = mAdapter.getView(0);
        }
        mCurrentDispositionView.setDispositions(getCurrentDispositions(), getCols(), getRows(), true);
    }

    /**
     * Method that restores the disposition view to the default state
     */
    private void deleteFrame() {
        mCurrentDispositionView = mAdapter.getView(0);
        mCurrentDispositionView.deleteCurrentFrame();
    }

    /**
     * Method that returns the system-defined dispositions templates
     *
     * @return List<Dispositions> All the system-defined dispositions templates
     */
    public List<Dispositions> getAllDispositions() {
        final int rows = getRows();
        final int cols = getCols();

        List<Dispositions> allDispositions = new ArrayList<>();
        allDispositions.add(new Dispositions(
                Dispositions.TYPE_CURRENT, getCurrentDispositions(), rows, cols));
        allDispositions.addAll(getSavedDispositions(rows, cols));
        allDispositions.addAll(getSystemDefinedDispositions(rows, cols));
        return allDispositions;
    }

    /**
     * Method that returns the user-saved dispositions
     *
     * @param rows The number of rows
     * @param cols The number of columns
     * @return List<Dispositions> All the user-saved dispositions
     */
    private List<Dispositions> getSavedDispositions(int rows, int cols) {
        List<List<Disposition>> saved = getUserDispositions();
        List<Dispositions> savedDispositions = new ArrayList<>(saved.size());
        for (List<Disposition> d : saved) {
            savedDispositions.add(new Dispositions(Dispositions.TYPE_SAVED, d, rows, cols));
        }
        return savedDispositions;
    }

    /**
     * Method that returns the system-defined dispositions templates
     *
     * @param rows The number of rows
     * @param cols The number of columns
     * @return List<Dispositions> All the system-defined dispositions templates
     */
    private List<Dispositions> getSystemDefinedDispositions(int rows, int cols) {
        String[] templates = getDispositionsTemplates();
        List<Dispositions> systemDispositions = new ArrayList<>(templates.length);
        for (String template : templates) {
            systemDispositions.add(new Dispositions(Dispositions.TYPE_SYSTEM,
                    DispositionUtil.toDispositions(template), rows, cols));
        }
        return systemDispositions;
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
            mDeleteMenu.setVisible(mAdapter.getView(mPager.getCurrentItem()).isSaved());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // Ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageSelected(int position) {
        // Save state
        mCurrentPage = position;
        mCurrentDispositionView = mAdapter.getView(position);
        mCurrentDispositionView.deselectCurrentFrame();

        // Enable/Disable menus
        if (mRestoreMenu != null) {
            mRestoreMenu.setVisible(position == 0);
        }
        if (mDeleteMenu != null) {
            mDeleteMenu.setVisible(mAdapter.getView(position).isSaved());
        }
        if (mSaveMenu != null) {
            mSaveMenu.setVisible(position == 0);
        }

        // Set the title
        if (position == 0) {
            mAdvise.setText(getString(R.string.pref_disposition_description));
        } else {
            if (mAdviseLines == -1) {
                mAdviseLines = mAdvise.getLineCount();
                mAdvise.setLines(mAdviseLines);
            }
            int saved = mAdapter.getCountOfSavedDispositions();
            if (position <= saved) {
                mAdvise.setText(getString(R.string.pref_disposition_saved,
                        String.valueOf(position), String.valueOf(saved)));
            } else {
                mAdvise.setText(getString(R.string.pref_disposition_template,
                        String.valueOf(position - saved), String.valueOf(mNumberOfTemplates)));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrollStateChanged(int state) {
        if (state != ViewPager.SCROLL_STATE_IDLE && mDeleteMenu != null) {
            mDeleteMenu.setVisible(false);
        }
        mResizeFrame.setVisibility(View.GONE);
    }

    private void deleteCurrentSaved() {
        int current = mPager.getCurrentItem();
        deleteUserDisposition(current - 1);
        mAdapter.deleteUserDisposition(current);
        mPager.setCurrentItem(current - 1);
        mPager.setAdapter(mAdapter);
        Toast.makeText(getActivity(), R.string.saved_dispositions_delete_success,
                Toast.LENGTH_SHORT).show();
    }

    private void addCurrentToSaved() {
        List<Disposition> current = mAdapter.getView(0).getDispositions();
        if (getUserDispositions().contains(current)) {
            Toast.makeText(getActivity(), R.string.saved_dispositions_save_exists,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Dispositions dispositions = new Dispositions(Dispositions.TYPE_SAVED,
                current, getRows(), getCols());
        saveUserDisposition(current);
        mAdapter.addUserDisposition(dispositions);
        mPager.setAdapter(mAdapter);
        Toast.makeText(getActivity(), R.string.saved_dispositions_save_success,
                Toast.LENGTH_SHORT).show();
    }

}
