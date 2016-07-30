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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;
import com.ruesga.android.wallpapers.photophase.AndroidHelper;
import com.ruesga.android.wallpapers.photophase.ICastService;
import com.ruesga.android.wallpapers.photophase.MediaPictureDiscoverer;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider.Preferences.Cast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fi.iki.elonen.NanoHTTPD;
import su.litvak.chromecast.api.v2.ChromeCast;

public class CastService extends Service implements CastServer.CastServerEventListener {
    private static final String TAG = "CastService";

    public static final String ACTION_DEVICE_SELECTED =
            "com.ruesga.android.wallpapers.photophase.actions.CAST_DEVICE_SELECTED";
    public static final String ACTION_CONNECTIVITY_CHANGED =
            "com.ruesga.android.wallpapers.photophase.actions.CAST_CONNECTIVITY_CHANGED";
    public static final String ACTION_MEDIA_COMMAND =
            "com.ruesga.android.wallpapers.photophase.actions.CAST_MEDIA_COMMAND";

    public static final String ACTION_ON_RELEASE_NETWORK =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_NETWORK_RELEASED";
    public static final String ACTION_SCAN_FINISHED =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_SCAN_FINISHED";
    public static final String ACTION_MEDIA_CHANGED =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_MEDIA_CHANGED";
    public static final String ACTION_QUEUE_CHANGED =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_QUEUE_CHANGED";
    public static final String ACTION_LOADING_MEDIA =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_LOADING_MEDIA";
    public static final String ACTION_SERVER_STOP =
            "com.ruesga.android.wallpapers.photophase.broadcast.SERVER_STOP";
    public static final String ACTION_SERVER_EXITED =
            "com.ruesga.android.wallpapers.photophase.broadcast.CAST_SERVER_EXITED";

    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_DEVICE = "device";
    public static final String EXTRA_IS_ERROR = "is_error";
    public static final String EXTRA_ROUTED = "routed";
    public static final String EXTRA_COMMAND = "command";

    public static final int CAST_MODE_NONE = -1;
    public static final int CAST_MODE_SINGLE = 0;
    public static final int CAST_MODE_SLIDESHOW = 1;

    public static final int INVALID_COMMAND = -1;
    public static final int COMMAND_PAUSE = 0;
    public static final int COMMAND_RESUME = 1;
    public static final int COMMAND_NEXT = 2;
    public static final int COMMAND_STOP = 3;

    public static class CastStatusInfo {
        public int mCastMode = CAST_MODE_NONE;
        public boolean mPaused;
    }

    private static final int MESSAGE_START_AND_CAST = 1;
    private static final int MESSAGE_CAST = 2;
    private static final int MESSAGE_SEND_CONFIGURATION = 3;
    private static final int MESSAGE_SELECT_DEVICE = 4;
    private static final int MESSAGE_REQUEST_SCAN = 5;
    private static final int MESSAGE_SLIDESHOW_NEXT = 6;
    private static final int MESSAGE_RESUME = 7;
    private static final int MESSAGE_PAUSE = 8;
    private static final int MESSAGE_SHOW = 9;
    private static final int MESSAGE_REMOVE = 10;
    private static final int MESSAGE_SHOW_PREVIOUS = 11;
    private static final int MESSAGE_SHOW_NEXT = 12;
    private static final int MESSAGE_STOP = 13;
    private static final int MESSAGE_EXIT = 14;

    private static final String CAST_SERVICE_TAG = "photophase-cast-slideshow";

    private CastServer mServer;
    private MediaPictureDiscoverer mMediaDiscoverer;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundHandlerThread;
    private GcmNetworkManager mGcmNetworkManager;
    private boolean mInDozeMode = false;

    private boolean mScanning;
    private boolean mHasNearDevices;

    private CastStatusInfo mCastStatusInfo = new CastStatusInfo();

    private List<String> mQueue = new ArrayList<>();
    private List<String> mShuffleQueue = new ArrayList<>();
    private int mQueuePointer;
    private Random mRandom;

    private final Handler.Callback mMessenger = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_START_AND_CAST:
                    //noinspection unchecked
                    Pair<String, String> o1 = (Pair<String, String>) message.obj;
                    startServerAndCastPath(o1.first, o1.second);
                    return true;

                case MESSAGE_CAST:
                    performCast((String) message.obj);
                    return true;

                case MESSAGE_SEND_CONFIGURATION:
                    performSendConfiguration();
                    return true;

                case MESSAGE_SELECT_DEVICE:
                    //noinspection unchecked
                    Pair<String, Boolean> o2 = (Pair<String, Boolean>) message.obj;
                    performSelectDevice(o2.first, o2.second);
                    return true;

                case MESSAGE_REQUEST_SCAN:
                    performRequestScan();
                    return true;

                case MESSAGE_SLIDESHOW_NEXT:
                    performSlideShowNext();
                    return true;

                case MESSAGE_RESUME:
                    performResume();
                    return true;

                case MESSAGE_PAUSE:
                    performPause();
                    return true;

                case MESSAGE_SHOW:
                    performShow((String) message.obj);
                    return true;

                case MESSAGE_REMOVE:
                    performRemove((String) message.obj);
                    return true;

                case MESSAGE_SHOW_PREVIOUS:
                    performShowPrevious();
                    return true;

                case MESSAGE_SHOW_NEXT:
                    performShowNext();
                    return true;

                case MESSAGE_STOP:
                    performStopCast();
                    return true;

                case MESSAGE_EXIT:
                    stopSelfAndServer();
                    return true;
            }
            return false;
        }
    };

    private final BroadcastReceiver mSettingsChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case PreferencesProvider.ACTION_SETTINGS_CHANGED:
                    // If is not enabled, just shutdown
                    if (!Cast.isEnabled(context)) {
                        // Shutdown the service
                        stopSelfAndServer();
                        return;
                    }

                    // Send configuration
                    boolean changed = intent.getBooleanExtra(
                            PreferencesProvider.EXTRA_FLAG_CAST_CONFIGURATION_CHANGE, false);
                    if (changed) {
                        if (mServer != null) {
                            Message.obtain(mBackgroundHandler, MESSAGE_SEND_CONFIGURATION).sendToTarget();
                        }

                        String prefKey = intent.getStringExtra(PreferencesProvider.EXTRA_PREF_KEY);
                        if (prefKey == null) {
                            return;
                        }

                        // Restore the shuffle queue
                        if (prefKey.equals("cast_slideshow_shuffle")) {
                            mShuffleQueue.clear();
                            mShuffleQueue.addAll(mQueue);
                            mQueuePointer = 0;
                        }

                        // Reset alarm and set the new slideshow timeout
                        if (prefKey.equals("cast_slideshow_time")) {
                            cancelSlideShowAlarm();
                            if (mCastStatusInfo.mCastMode == CAST_MODE_SLIDESHOW) {
                                scheduleSlideShowAlarm();
                            }
                        }
                    }
                    break;
                case Intent.ACTION_SCREEN_ON:
                    mInDozeMode = false;
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    mInDozeMode = AndroidHelper.isMarshmallowOrGreater();
                    if (mCastStatusInfo.mCastMode == CAST_MODE_SLIDESHOW && !mCastStatusInfo.mPaused) {
                        // Change to gcm
                        scheduleSlideShowAlarm();
                    }
                    break;
            }
        }
    };

    private final ICastService.Stub mBinder = new ICastService.Stub() {
        @Override
        public boolean hasNearDevices() throws RemoteException {
            return mHasNearDevices;
        }

        @Override
        public void requestScan() throws RemoteException {
            if (!CastUtils.hasValidCastNetwork(CastService.this)) {
                return;
            }

            Message.obtain(mBackgroundHandler, MESSAGE_REQUEST_SCAN).sendToTarget();
        }

        @Override
        public void cast(String path) throws RemoteException {
            // Check if device is configured
            if (!canCastToDevice(path, false)) {
                return;
            }

            // Send the path to the device
            Message.obtain(mBackgroundHandler, MESSAGE_CAST, path).sendToTarget();
        }

        @Override
        public void enqueue(String path) throws RemoteException {
            // Check if device is configured
            if (!canCastToDevice(path, false)) {
                return;
            }

            // Send the path to the device
            performEnqueue(path);
        }

        @Override
        public void pause() throws RemoteException {
            performPause();
        }

        @Override
        public void resume() throws RemoteException {
            if (mCastStatusInfo.mCastMode == CAST_MODE_SLIDESHOW ||
                    (mCastStatusInfo.mCastMode == CAST_MODE_NONE && !mQueue.isEmpty())) {
                Message.obtain(mBackgroundHandler, MESSAGE_RESUME).sendToTarget();
            }
        }

        @Override
        public void show(String media) throws RemoteException {
            if (mCastStatusInfo.mCastMode == CAST_MODE_SLIDESHOW ||
                    (mCastStatusInfo.mCastMode == CAST_MODE_NONE && !mQueue.isEmpty())) {
                Message.obtain(mBackgroundHandler, MESSAGE_SHOW, media).sendToTarget();
            }
        }

        @Override
        public void remove(String media) throws RemoteException {
            if (mCastStatusInfo.mCastMode == CAST_MODE_SLIDESHOW ||
                    (mCastStatusInfo.mCastMode == CAST_MODE_NONE && !mQueue.isEmpty())) {
                performRemove(media);
            }
        }

        @Override
        public void previous() throws RemoteException {
            if (mCastStatusInfo.mCastMode == CAST_MODE_SLIDESHOW ||
                    (mCastStatusInfo.mCastMode == CAST_MODE_NONE && !mQueue.isEmpty())) {
                Message.obtain(mBackgroundHandler, MESSAGE_SHOW_PREVIOUS).sendToTarget();
            }
        }

        @Override
        public void next() throws RemoteException {
            if (mCastStatusInfo.mCastMode == CAST_MODE_SLIDESHOW ||
                    (mCastStatusInfo.mCastMode == CAST_MODE_NONE && !mQueue.isEmpty())) {
                Message.obtain(mBackgroundHandler, MESSAGE_SHOW_NEXT).sendToTarget();
            }
        }

        @Override
        public void stop() throws RemoteException {
            Message.obtain(mBackgroundHandler, MESSAGE_STOP).sendToTarget();
        }

        @Override
        public void exit() throws RemoteException {
            Message.obtain(mBackgroundHandler, MESSAGE_EXIT).sendToTarget();
        }

        @Override
        public String getCurrentPlaying() throws RemoteException {
            if (mServer == null || mServer.getCurrentlyPlaying() == null) {
                return null;
            }
            return mServer.getCurrentlyPlaying().getAbsolutePath();
        }

        @Override
        public String[] getCurrentQueue() throws RemoteException {
            return mQueue.toArray(new String[mQueue.size()]);
        }

        @Override
        public int getCurrentCastMode() throws RemoteException {
            return mCastStatusInfo.mCastMode;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mRandom = new Random();
        try {
            mGcmNetworkManager = GcmNetworkManager.getInstance(this);
        } catch (Exception ex) {
            Log.e(TAG, "No Gcm network", ex);
        }

        // Create a background messenger
        mBackgroundHandlerThread = new HandlerThread(TAG + "BackgroundThread");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper(), mMessenger);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PreferencesProvider.ACTION_SETTINGS_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mSettingsChanged, filter);

        mMediaDiscoverer = new MediaPictureDiscoverer(this);

        // Request an device scan
        Message.obtain(mBackgroundHandler, MESSAGE_REQUEST_SCAN).sendToTarget();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister receiver
        unregisterReceiver(mSettingsChanged);

        // Destroy the server
        cancelSlideShowAlarm();
        stopServer();

        // Destroy background thread
        mBackgroundHandlerThread.quit();
        mBackgroundHandler = null;
        mBackgroundHandlerThread = null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopServer();
    }

    private boolean canCastToDevice(String path, boolean isError) {
        // We are connected to the device
        if (mServer != null) {
            return true;
        }

        // We are not connected and there aren't near devices
        if (!mHasNearDevices) {
            return false;
        }

        // Cannot cast now, but we can ask the user to select a route
        askForCastDevice(path, isError);
        return false;
    }

    private void askForCastDevice(String path, boolean isError) {
        Pair<String, Boolean> o = new Pair<>(path, isError);
        Message.obtain(mBackgroundHandler, MESSAGE_SELECT_DEVICE, o).sendToTarget();
    }

    public boolean startServer(String deviceInfo) {
        Log.d(TAG, "Start server");

        ChromeCast device = CastUtils.string2chromecast(deviceInfo);
        try {
            if (mServer != null &&
                mServer.getChromecast().getAddress().equals(device.getAddress()) &&
                mServer.getChromecast().getPort() == device.getPort()) {
                // The Chromecast device is the same and is currently casting. Don't start it again

                String oldName = mServer.getChromecast().getName();
                String newName = device.getName();
                if (!oldName.equals(newName)) {
                    // Update the device name and send the configuration
                    mServer.getChromecast().setName(device.getName());
                    performSendConfiguration();
                }
                return true;
            }

            // Stop the current cast server
            stopServer();

            // Create a new cast server
            startServer(device);
            Cast.setLastConnectedDevice(this, device);
            return true;

        } catch (IOException ex) {
            Log.e(TAG, "Failed to start cast server", ex);
            mServer = null;
        }
        return false;
    }

    private void stopServer() {
        Log.d(TAG, "Stop server");

        mCastStatusInfo.mCastMode = CAST_MODE_NONE;
        cancelSlideShowAlarm();
        if (mServer != null) {
            try {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mServer.stop();
                        mServer = null;
                        Log.i(TAG, "Cast server was stopped");
                    }
                });
                t.start();
                t.join(3000L);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        Intent i = new Intent(ACTION_SERVER_EXITED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private void performCast(String path) {
        Log.d(TAG, "Cast " + path);

        try {
            File f = new File(path);

            // It's a folder? Then obtain all the pictures, send the first one and enqueue th
            // rest ones
            if (f.exists() && f.isDirectory()) {
                List<File> pictures = mMediaDiscoverer.obtain(f);
                if (pictures.isEmpty()) {
                    return;
                }

                // Stop current alarm
                cancelSlideShowAlarm();

                // Enqueue and cast this
                mQueue.clear();
                mShuffleQueue.clear();
                for (File pic : pictures) {
                    String p = pic.getAbsolutePath();
                    if (!mQueue.contains(p)) {
                        mQueue.add(p);
                        mShuffleQueue.add(p);
                    }
                }

                mCastStatusInfo.mCastMode = CAST_MODE_SLIDESHOW;
                mServer.send(chooseNextPicture());
                sendLoadingStatus();
                mQueuePointer = 0;
            } else {
                cancelSlideShowAlarm();
                mCastStatusInfo.mCastMode = CAST_MODE_SINGLE;
                mServer.send(path);
                sendLoadingStatus();
            }
        } catch (Exception ex) {
            Log.w(TAG, "Cannot send path to cast device: " + path, ex);
        }
    }

    private void performEnqueue(String path) {
        Log.d(TAG, "Enqueue " + path);

        File f = new File(path);

        // It's a folder? Then obtain all the pictures, send the first one and enqueue the
        // rest ones
        if (f.exists() && f.isDirectory()) {
            List<File> pictures = mMediaDiscoverer.obtain(f);
            if (pictures.isEmpty()) {
                return;
            }

            for (File pic : pictures) {
                String p = pic.getAbsolutePath();
                if (!mQueue.contains(p)) {
                    mQueue.add(p);
                    mShuffleQueue.add(p);
                }
            }
        } else {
            String p = f.getAbsolutePath();
            if (!mQueue.contains(p)) {
                mQueue.add(p);
                mShuffleQueue.add(p);
            }
        }

        if (mCastStatusInfo.mCastMode != CAST_MODE_SLIDESHOW) {
            final String p = chooseNextPicture();
            try {
                mCastStatusInfo.mCastMode = CAST_MODE_SLIDESHOW;
                mServer.send(p);
                mQueuePointer = 0;
                sendLoadingStatus();
            } catch (Exception ex) {
                Log.e(TAG, "Cannot send picture to device: " + p, ex);
            }
        }

        // Notify
        Intent i = new Intent(ACTION_QUEUE_CHANGED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private void performSendConfiguration() {
        Log.d(TAG, "Send configuration");

        try {
            checkAndRestoreServerStatusIfNeeded();
            if (mServer != null) {
                mServer.sendConfiguration();
            } else {
                Log.e(TAG, "Cannot configure device. Server is down");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Cannot configure device", ex);
        }
    }

    private void performStopCast() {
        Log.d(TAG, "Stop cast");

        try {
            mQueuePointer = 0;
            cancelSlideShowAlarm();

            checkAndRestoreServerStatusIfNeeded();
            if (mServer != null) {
                mServer.sendStopCast();

                mCastStatusInfo.mCastMode = CAST_MODE_NONE;
                mServer.onCastStatusUpdated(mCastStatusInfo);

                Intent i = new Intent(ACTION_SERVER_STOP);
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);

                mQueuePointer = -2;
            } else {
                Log.e(TAG, "Cannot send stop app to device. Server is down");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Cannot send stop app to device", ex);
        }
    }

    private void performSlideShowNext(){
        Log.d(TAG, "Slide show next");

        cancelSlideShowAlarm();
        if (mCastStatusInfo.mCastMode != CAST_MODE_SLIDESHOW || mCastStatusInfo.mPaused) {
            // Notify
            Intent i = new Intent(ACTION_ON_RELEASE_NETWORK);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            return;
        }

        if (mQueue.isEmpty()) {
            performStopCast();
            return;
        }

        if (mQueuePointer == -1) {
            boolean repeat = Cast.isSlideshowRepeat(this);
            if (!repeat) {
                // Stop here
                performStopCast();
                return;
            }

            mShuffleQueue.clear();
            mShuffleQueue.addAll(mQueue);
        } else if (mQueuePointer <= -2) {
            mQueuePointer = -1;
        }


        // Point to the next picture
        mQueuePointer++;
        int size = mQueue.size();
        if (mQueuePointer >= size) {
            boolean repeat = Cast.isSlideshowRepeat(this);
            if (!repeat) {
                // Stop here
                performStopCast();
                return;
            }

            mShuffleQueue.clear();
            mShuffleQueue.addAll(mQueue);

            mQueuePointer = 0;
        }

        // Choose the next picture to show
        final String f = chooseNextPicture();
        try {
            checkAndRestoreServerStatusIfNeeded();
            if (mServer != null) {
                mServer.send(f);
                sendLoadingStatus();
            } else {
                Log.e(TAG, "Cannot change to next image " + f + ". Server is down");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Cannot change to next image " + f + " for device", ex);
        }

        // Mark End of Queue
        boolean eoq = mQueuePointer >= (size - 1);
        if (eoq) {
           mQueuePointer = -1;
        }
    }

    private void performSelectDevice(String path, boolean isError) {
        boolean isRouted = false;
        ChromeCast device = Cast.getLastConnectedDevice(this);
        if (!isError && device != null && Cast.isUseLastConnectedDevice(this)) {
            if (CastUtils.testConnectivity(device)) {
                Pair<String, String> o = new Pair<>(CastUtils.chromecast2string(device), path);
                Message.obtain(mBackgroundHandler, MESSAGE_START_AND_CAST, o).sendToTarget();
                isRouted = true;
            }
        }

        // Open the cast route activity
        Intent i = new Intent(this, CastRouteActivity.class);
        i.putExtra(EXTRA_ROUTED, isRouted);
        i.putExtra(EXTRA_PATH, path);
        i.putExtra(EXTRA_IS_ERROR, isError);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void performRequestScan() {
        if (mScanning) {
            return;
        }

        // Stop server if we shouldn't listen for cast devices
        if (!Cast.isEnabled(this)) {
            mHasNearDevices = false;
            stopSelfAndServer();
            return;
        }

        if (!CastUtils.hasValidCastNetwork(this)) {
            mHasNearDevices = false;
            stopSelfAndServer();
            return;
        }

        // Check if at least one devices is listening
        mScanning = true;
        mHasNearDevices = CastUtils.isNearDevicesAvailable(this);
        mScanning = false;
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_SCAN_FINISHED));
        Log.d(TAG, "Performed discovery device scan. Has near devices?: " + mHasNearDevices);
    }

    private void performShow(String media) {
        Log.d(TAG, "Show media: " + media);
        if (!mQueue.isEmpty() && mCastStatusInfo.mCastMode == CAST_MODE_NONE) {
            mCastStatusInfo.mCastMode = CAST_MODE_SLIDESHOW;
        }
        int pos = mQueue.indexOf(media);
        if (pos >= 0) {
            cancelSlideShowAlarm();
            if (PreferencesProvider.Preferences.Cast.isSlideshowShuffle(this)) {
                // Restore the shuffle queue
                mShuffleQueue.clear();
                mShuffleQueue.addAll(mQueue);
            }
            mQueuePointer = pos;

            try {
                checkAndRestoreServerStatusIfNeeded();
                if (mServer != null) {
                    mServer.send(media);
                    sendLoadingStatus();
                } else {
                    Log.e(TAG, "Cannot change to image " + media + ". Server is down");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Cannot change to image " + media + " for device", ex);
            }
        }
    }

    private void performRemove(String media) {
        Log.d(TAG, "Remove media: " + media);
        boolean current = mServer != null && mServer.getCurrentlyPlaying() != null
                && mServer.getCurrentlyPlaying().getAbsolutePath().equals(media);
        int pos = mQueue.indexOf(media);
        mQueue.remove(media);
        mShuffleQueue.remove(media);
        if (pos >= 0 && current) {
            mQueuePointer--;
            Message.obtain(mBackgroundHandler, MESSAGE_SLIDESHOW_NEXT).sendToTarget();
        }
    }

    private void performShowPrevious() {
        Log.d(TAG, "Show previous");
        if (!mQueue.isEmpty() && mCastStatusInfo.mCastMode == CAST_MODE_NONE) {
            mCastStatusInfo.mCastMode = CAST_MODE_SLIDESHOW;
        }
        File current = mServer.getCurrentlyPlaying();
        if (current != null) {
            int pos = mQueue.indexOf(current.getAbsolutePath());
            if (pos >= 0) {
                cancelSlideShowAlarm();
                pos--;
                if (pos < 0) {
                    pos = mQueue.size();
                }
                if (PreferencesProvider.Preferences.Cast.isSlideshowShuffle(this)) {
                    // Restore the shuffle queue
                    mShuffleQueue.clear();
                    mShuffleQueue.addAll(mQueue);
                }
                mQueuePointer = pos;

                final String f = mQueue.get(pos);
                try {
                    checkAndRestoreServerStatusIfNeeded();
                    if (mServer != null) {
                        mServer.send(f);
                        sendLoadingStatus();
                    } else {
                        Log.e(TAG, "Cannot change to previous image " + f + ". Server is down");
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Cannot change to previous image " + f + " for device", ex);
                }
            }
        }
    }

    private void performShowNext() {
        Log.d(TAG, "Show next");
        if (!mQueue.isEmpty() && mCastStatusInfo.mCastMode == CAST_MODE_NONE) {
            mCastStatusInfo.mCastMode = CAST_MODE_SLIDESHOW;
        }
        File current = mServer.getCurrentlyPlaying();
        if (current != null) {
            int pos = mQueue.indexOf(current.getAbsolutePath());
            if (pos >= 0) {
                cancelSlideShowAlarm();
                pos++;
                if (pos >= mQueue.size()) {
                    pos = 0;
                }
                if (PreferencesProvider.Preferences.Cast.isSlideshowShuffle(this)) {
                    // Restore the shuffle queue
                    mShuffleQueue.clear();
                    mShuffleQueue.addAll(mQueue);
                }
                mQueuePointer = pos;

                final String f = mQueue.get(pos);
                try {
                    checkAndRestoreServerStatusIfNeeded();
                    if (mServer != null) {
                        mServer.send(f);
                        sendLoadingStatus();
                    } else {
                        Log.e(TAG, "Cannot change to next image " + f + ". Server is down");
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Cannot change to next image " + f + " for device", ex);
                }
            }
        }
    }

    private void performResume() {
        Log.d(TAG, "Resume slide show");
        if (!mQueue.isEmpty() && mCastStatusInfo.mCastMode == CAST_MODE_NONE) {
            mCastStatusInfo.mCastMode = CAST_MODE_SLIDESHOW;
        }
        mCastStatusInfo.mPaused = false;
        performSlideShowNext();
        if (mServer != null) {
            mServer.onCastStatusUpdated(mCastStatusInfo);
        }
    }

    private void performPause() {
        Log.d(TAG, "Pause slide show");
        cancelSlideShowAlarm();
        mCastStatusInfo.mPaused = true;
        if (mServer != null) {
            mServer.onCastStatusUpdated(mCastStatusInfo);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "Cast service started without an intent");
            return START_NOT_STICKY;
        }

        // Check which action the service was
        Log.w(TAG, "Received intent: " + intent);
        switch (intent.getAction()) {
            case ACTION_DEVICE_SELECTED:
                if (!intent.hasExtra(EXTRA_PATH)) {
                    Log.w(TAG, "Cast service started without a photo to send to");
                    return START_NOT_STICKY;
                }
                if (!intent.hasExtra(EXTRA_DEVICE)) {
                    Log.w(TAG, "Cast service started without a device to send to");
                    return START_NOT_STICKY;
                }

                // Get the parameters
                String path = intent.getStringExtra(EXTRA_PATH);
                String device = intent.getStringExtra(EXTRA_DEVICE);

                // Start a new server and associate it to the chromecast device
                Pair<String, String> o = new Pair<>(device, path);
                Message.obtain(mBackgroundHandler, MESSAGE_START_AND_CAST, o).sendToTarget();
                return START_STICKY;

            case ACTION_CONNECTIVITY_CHANGED:
                Message.obtain(mBackgroundHandler, MESSAGE_REQUEST_SCAN).sendToTarget();
                break;

            case ACTION_MEDIA_COMMAND:
                int command = intent.getIntExtra(EXTRA_COMMAND, INVALID_COMMAND);
                if (command == INVALID_COMMAND) {
                    return START_STICKY;
                }

                switch (command) {
                    case COMMAND_RESUME:
                        Message.obtain(mBackgroundHandler, MESSAGE_RESUME).sendToTarget();
                        break;

                    case COMMAND_PAUSE:
                        Message.obtain(mBackgroundHandler, MESSAGE_PAUSE).sendToTarget();
                        performPause();
                        break;

                    case COMMAND_NEXT:
                        Message.obtain(mBackgroundHandler, MESSAGE_SLIDESHOW_NEXT).sendToTarget();
                        break;

                    case COMMAND_STOP:
                        Message.obtain(mBackgroundHandler, MESSAGE_EXIT).sendToTarget();
                        break;
                }
                return START_STICKY;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCastServerDisconnected() {
        mServer = null;
        Log.i(TAG, "Cast server was disconnected");
    }

    @Override
    public void onNewTrackReceived(String sender) {
        if (!sender.equals(Settings.Secure.ANDROID_ID)) {
            performPause();
        }  else {
            Intent i = new Intent(ACTION_MEDIA_CHANGED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);

            scheduleSlideShowAlarm();
        }
    }

    @Override
    public CastStatusInfo obtainCastStatusInfo() {
        return mCastStatusInfo;
    }

    private void startServerAndCastPath(String device, String path) {
        if (!startServer(device)) {
            askForCastDevice(path, true);
            return;
        }
        performCast(path);
    }

    private void scheduleSlideShowAlarm() {
        cancelSlideShowAlarm();

        // Don't try to schedule
        if (mCastStatusInfo.mCastMode != CAST_MODE_SLIDESHOW || mCastStatusInfo.mPaused) {
            return;
        }

        if (!mInDozeMode) {
            // AlarmManager
            Intent i = new Intent(this, CastService.class);
            i.setAction(ACTION_MEDIA_COMMAND);
            i.putExtra(EXTRA_COMMAND, COMMAND_NEXT);
            PendingIntent pi = PendingIntent.getService(
                    this, 1000, i, PendingIntent.FLAG_UPDATE_CURRENT);

            long time = Cast.getSlideshowTime(this) * 1000L;
            long due = System.currentTimeMillis() + time;
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (AndroidHelper.isMarshmallowOrGreater()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pi);
            } else if (AndroidHelper.isKitKatOrGreater()) {
                am.setExact(AlarmManager.RTC_WAKEUP, due, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, due, pi);
            }
        } else {
            // Gcm
            long time = Cast.getSlideshowTime(this);
            if (mGcmNetworkManager != null) {
                OneoffTask task = new OneoffTask.Builder()
                        .setService(CastGcmTaskService.class)
                        .setTag(CAST_SERVICE_TAG)
                        .setExecutionWindow(time - 1, time)
                        .setRequiredNetwork(Task.NETWORK_STATE_UNMETERED)
                        .build();
                mGcmNetworkManager.schedule(task);
            }
        }
    }

    private void cancelSlideShowAlarm() {
        // Cancel both path (AlarmManger and Gcm)
        Intent i = new Intent(this, CastService.class);
        i.setAction(ACTION_MEDIA_COMMAND);
        i.putExtra(EXTRA_COMMAND, COMMAND_NEXT);
        PendingIntent pi = PendingIntent.getService(
                this, 1000, i, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pi);

        if (mGcmNetworkManager != null) {
            mGcmNetworkManager.cancelAllTasks(CastGcmTaskService.class);
        }
    }

    private void stopSelfAndServer() {
        stopServer();
        stopSelf();
    }

    private String chooseNextPicture() {
        if (Cast.isSlideshowShuffle(this)) {
            int size = mShuffleQueue.size();
            int next = size == 1 ? 0 : Math.round(mRandom.nextFloat() * (size - 1));
            return mShuffleQueue.remove(next);
        }
        return mQueue.get(mQueuePointer);
    }

    private void startServer(ChromeCast device) throws IOException {
        CastServer castServer = new CastServer(this, device, this);
        castServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        mServer = castServer;
    }

    private void checkAndRestoreServerStatusIfNeeded() {
        if (mServer == null) {
            try {
                startServer(Cast.getLastConnectedDevice(this));
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    private void sendLoadingStatus() {
        Intent i = new Intent(ACTION_LOADING_MEDIA);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }
}
