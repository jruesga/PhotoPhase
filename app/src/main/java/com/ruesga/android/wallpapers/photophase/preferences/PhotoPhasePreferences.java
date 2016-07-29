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

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.util.Colors;
import com.ruesga.android.wallpapers.photophase.AndroidHelper;
import com.ruesga.android.wallpapers.photophase.R;

import java.util.Calendar;
import java.util.List;

/**
 * The PhotoPhase Live Wallpaper preferences.
 */
public class PhotoPhasePreferences extends AppCompatPreferenceActivity {

    private OnBackPressedListener mCallback;
    private Header mAboutHeader;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initToolbar();
        AndroidHelper.setupRecentBar(this);
    }

    /**
     * Method that initializes the toolbar of the activity.
     */
    private void initToolbar() {
        // Add a toolbar
        ViewGroup root = (ViewGroup) findViewById(
                android.R.id.list).getParent().getParent().getParent();
        Toolbar toolbar = (Toolbar) LayoutInflater.from(this).inflate(
                R.layout.preference_toolbar, root, false);
        root.addView(toolbar, 0);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mCallback = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);

        // Cast is only supported after API16
        if (!AndroidHelper.isJellyBeanOrGreater()) {
            target.remove(target.size() - 2);
        }

        // Retrieve the about header
        mAboutHeader = target.get(target.size() - 1);
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        // Open the about libraries intent
        if (header.equals(mAboutHeader)) {
            // Build the legal stuff string
            int year = Calendar.getInstance().get(Calendar.YEAR);
            String legal = getString(R.string.app_legal, String.valueOf(year));

            // Open the attribution library
            new LibsBuilder()
                    .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                    .withFields(R.string.class.getFields())
                    .withAboutIconShown(true)
                    .withAboutVersionShown(true)
                    .withLicenseShown(true)
                    .withSortEnabled(true)
                    .withAboutSpecial1("Special")
                    .withActivityTitle(getString(R.string.pref_about))
                    .withAboutDescription(getString(R.string.app_description) + "\n" + legal)
                    .withActivityColor(new Colors(
                            ContextCompat.getColor(getApplicationContext(), R.color.color_primary),
                            ContextCompat.getColor(getApplicationContext(), R.color.color_primary_dark)))
                    .start(this);
            return;
        }

        super.onHeaderClick(header, position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()) {
          case android.R.id.home:
              if (mCallback == null || !mCallback.onBackPressed()) {
                  finish();
              }
              return true;
          default:
             return super.onOptionsItemSelected(item);
       }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mCallback == null || !mCallback.onBackPressed()) {
                finish();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof OnBackPressedListener) {
            mCallback = (OnBackPressedListener)fragment;
        } else {
            mCallback = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidFragment(String fragmentName) {
        return true;
    }
}
