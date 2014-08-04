/*
 * Copyright (C) 2014 Jorge Ruesga
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.R;

import java.io.InputStream;

/**
 * The activity for show the changelog of the application
 */
public class ChangeLogActivity extends Activity implements OnCancelListener, OnDismissListener {

    private static final String TAG = "ChangeLogActivity"; //$NON-NLS-1$

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle state) {
        //Save state
        super.onCreate(state);
        init();
    }

    /**
     * Initialize the activity. This method handles the passed intent, opens
     * the appropriate activity and ends.
     */
    private void init() {
        InputStream is = getApplicationContext().getResources().openRawResource(R.raw.changelog);
        if (is == null) {
            Log.e(TAG, "Changelog file not exists"); //$NON-NLS-1$
            finish();
            return;
        }

        try {
            // Read the changelog
            StringBuilder sb = new StringBuilder();
            int read = 0;
            byte[] data = new byte[512];
            while ((read = is.read(data, 0, 512)) != -1) {
                sb.append(new String(data, 0, read));
            }

            //Create the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.changelog_title);
            builder.setMessage(sb);
            builder.setPositiveButton(getString(R.string.mnu_ok), null);
            AlertDialog dialog = builder.create();
            dialog.setOnDismissListener(this);
            dialog.setOnCancelListener(this);
            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to read changelog file", e); //$NON-NLS-1$
            finish();

        } finally {
            try {
                is.close();
            } catch (Exception e) {/**NON BLOCK**/}
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        // We have to finish here; this activity is only a wrapper
        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        // We have to finish here; this activity is only a wrapper
        finish();
    }

}
