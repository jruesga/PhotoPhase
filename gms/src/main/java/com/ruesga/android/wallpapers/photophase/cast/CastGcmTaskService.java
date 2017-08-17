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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

public class CastGcmTaskService extends GcmTaskService {

    private static final long MAX_NETWORK_WAIT = 8000L;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mNetworkLock) {
                mNetworkLock.notify();
            }
        }
    };

    private final Object mNetworkLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(CastServiceConstants.ACTION_ON_RELEASE_NETWORK);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        Intent i = new Intent();
        i.setPackage(getPackageName());
        i.setAction(CastServiceConstants.ACTION_MEDIA_COMMAND);
        i.putExtra(CastServiceConstants.EXTRA_COMMAND, CastServiceConstants.COMMAND_NEXT);
        // This should work since GcmTaskService should be in foreground, and it service
        // should be previously created
        startService(i);

        // Hold a bit the job, to ensure the picture was sent over the network
        try {
            synchronized (mNetworkLock) {
                mNetworkLock.wait(MAX_NETWORK_WAIT);
            }
        } catch (InterruptedException ex) {
            // Ignore
        }

        return GcmNetworkManager.RESULT_SUCCESS;
    }
}
