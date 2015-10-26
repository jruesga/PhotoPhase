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
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.ruesga.android.wallpapers.photophase.R;

import java.util.List;

/**
 * The PhotoPhase Live Wallpaper preferences.
 */
public class PhotoPhasePreferences extends AppCompatPreferenceActivity {

    private OnBackPressedListener mCallback;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initToolbar();
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

        // Retrieve the about header
        Header aboutHeader = target.get(target.size() - 1);
        try {
            String appVer =
                    this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
            aboutHeader.summary = getString(R.string.pref_about_summary, appVer);
        } catch (Exception e) {
            aboutHeader.summary = getString(R.string.pref_about_summary, "");
        }
        aboutHeader.intent = new Intent(getApplicationContext(), ChangeLogActivity.class);
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
