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

package com.ruesga.android.wallpapers.photophase.cast;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;

@SuppressWarnings({"unused", "WeakerAccess"})
public class GmsCastTaskManager implements ICastTaskManager {
    private static final String TAG = "CastGcmTaskManager";

    private static final String CAST_SERVICE_TAG = "photophase-cast-slideshow";

    private GcmNetworkManager mGcmNetworkManager;

    public void instance(Context context) {
        Log.i(TAG, "Using GCM cast task manager");
        try {
            mGcmNetworkManager = GcmNetworkManager.getInstance(context);
        } catch (Exception ex) {
            Log.e(TAG, "No Gcm network", ex);
        }
    }

    public boolean canNetworkSchedule() {
        return mGcmNetworkManager != null;
    }

    public void schedule(long time) {
        OneoffTask task = new OneoffTask.Builder()
                .setService(CastGcmTaskService.class)
                .setTag(CAST_SERVICE_TAG)
                .setExecutionWindow(time - 1, time)
                .setRequiredNetwork(Task.NETWORK_STATE_UNMETERED)
                .build();
        mGcmNetworkManager.schedule(task);
    }

    @Override
    public void cancelTasks() {
        mGcmNetworkManager.cancelAllTasks(CastGcmTaskService.class);
    }
}
