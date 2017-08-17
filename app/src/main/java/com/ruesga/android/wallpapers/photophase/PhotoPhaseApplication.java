/*
 * Copyright (C) 2017 Jorge Ruesga
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
package com.ruesga.android.wallpapers.photophase;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class PhotoPhaseApplication extends Application {

    public static final String CAST_NOTIFICATION_CHANNEL_ID = "photophase-cast";

    @Override
    public void onCreate() {
        super.onCreate();

        createCastNotificationChannel(getApplicationContext());
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void createCastNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(CAST_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.cast_notification_channel),
                    NotificationManager.IMPORTANCE_LOW);
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setShowBadge(false);
            nm.createNotificationChannel(channel);
        }
    }
}
