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
package com.ruesga.android.wallpapers.photophase.cast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider;

public class CastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Ignore any event if we are not ready for
        if (!PreferencesProvider.Preferences.Cast.isEnabled(context)) {
            return;
        }

        if (intent != null) {
            String action = intent.getAction();
            if (action.equals("android.net.wifi.supplicant.CONNECTION_CHANGE")
                    || action.equals("android.net.wifi.STATE_CHANGE")) {
                // Request a cast scan
                Intent i;
                try {
                    i = new Intent(context, CastService.class);
                    i.setAction(CastServiceConstants.ACTION_CONNECTIVITY_CHANGED);
                    context.startService(i);
                } catch (IllegalStateException ex) {
                    // This should happen because neither of this actions never got called on O
                }

                // Notify anyone that connectivity changed
                i = new Intent(CastServiceConstants.ACTION_CONNECTIVITY_CHANGED);
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
            }
        }
    }
}
