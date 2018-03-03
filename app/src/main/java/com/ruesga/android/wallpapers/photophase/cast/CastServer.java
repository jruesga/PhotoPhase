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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.cast.CastDeviceMessages.BaseDeviceMessage;
import com.ruesga.android.wallpapers.photophase.cast.CastDeviceMessages.OnNewTrackMessage;
import com.ruesga.android.wallpapers.photophase.cast.CastDeviceMessages.OnReadyMessage;
import com.ruesga.android.wallpapers.photophase.cast.CastService.CastStatusInfo;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider;
import com.ruesga.android.wallpapers.photophase.utils.BitmapUtils;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;
import su.litvak.chromecast.api.v2.AppEvent;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEvent;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEventListener;
import su.litvak.chromecast.api.v2.Close;
import su.litvak.chromecast.api.v2.Status;

public class CastServer extends NanoHTTPD {
    private static final String TAG = "CastServer";

    private final static String PHOTOPHASE_APP_ID = "80F2080C";
    private final static String PHOTOPHASE_NAMESPACE =
            "urn:x-cast:com.ruesga.android.wallpapers.photophase";
    private final static int VERSION = 1;

    private static final String HASH_KEY = "k";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public interface CastServerEventListener {
        void onCastServerDisconnected();
        void onNewTrackReceived(String sender);
        CastStatusInfo obtainCastStatusInfo();
    }

    private final Context mContext;
    private PowerManager.WakeLock mCpuWakeLock;
    private final ChromeCast mChromecast;
    private boolean mIsSecure;
    private boolean mIsConnected;

    private final Map<String, String> mRequests = new HashMap<>();

    private final Object mLock = new Object();
    private boolean mCastReady;
    private boolean mCastReceiverNeedsUpgrade;

    private File mCurrentlyPlaying;
    private CastStatusInfo mLastStatus;

    private int mTimeout;
    private boolean mDaemon;

    private final CastServerEventListener mCastServerEventListener;

    private final ChromeCastSpontaneousEventListener mCastEventListener
            = new ChromeCastSpontaneousEventListener() {
        @Override
        public void spontaneousEventReceived(ChromeCastSpontaneousEvent evt) {
            Object data = evt.getData();
            if (data instanceof AppEvent) {
                AppEvent appEvt = (AppEvent) data;
                if (appEvt.namespace.equals(PHOTOPHASE_NAMESPACE)) {
                    Log.d(TAG, "Received app event: " + appEvt.message);
                    try {
                        BaseDeviceMessage msg = CastDeviceMessages.parseDeviceMessage(appEvt.message);
                        if (msg instanceof OnReadyMessage) {
                            OnReadyMessage message = (OnReadyMessage) msg;
                            synchronized (mLock) {
                                mCastReady = true;
                                mCastReceiverNeedsUpgrade = message.mVersion < VERSION;
                                mLock.notify();
                            }
                        } else if (msg instanceof OnNewTrackMessage) {
                            OnNewTrackMessage message = (OnNewTrackMessage) msg;
                            String media = mRequests.remove(message.mToken);
                            CastStatusInfo status = mLastStatus != null
                                    ? mLastStatus : mCastServerEventListener.obtainCastStatusInfo();
                            if (status != null && media != null) {
                                File f = new File(media);
                                CastNotification.showNotification(mContext, f, status);
                                setCurrentlyPlaying(f, false, status);

                                // Notify
                                mCastServerEventListener.onNewTrackReceived(message.mSender);
                            }
                        }
                    } catch (Exception ex) {
                        // Ignore
                        Log.e(TAG, "Can't parse spontaneous message: " + appEvt.message, ex);
                    }
                }
            } else if (data instanceof Close) {
                Close ev = (Close) data;
                if (!ev.requestedBySender) {
                    stop(true);
                    mCastServerEventListener.onCastServerDisconnected();
                }
            }
        }
    };

    public CastServer(Context context, ChromeCast chromecast, CastServerEventListener listener) {
        super(resolveCastServerAddress(chromecast), 0);
        mContext = context;
        mChromecast = chromecast;
        mCastServerEventListener = listener;

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mCpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "photophase_server");
        mCpuWakeLock.setReferenceCounted(false);
    }

    public final String getServerBaseUrl() {
        return (mIsSecure ? "https" : "http") + "://" + getHostname() + ":" + getListeningPort();
    }

    public ChromeCast getChromecast() {
        return mChromecast;
    }

    public File getCurrentlyPlaying() {
        return mCurrentlyPlaying;
    }

    protected void onCastStatusUpdated(CastStatusInfo status) {
        mLastStatus = status;
        CastNotification.showNotification(mContext, mCurrentlyPlaying, status);
    }

    private String createAuthorizedUrl(String token) {
        return getServerBaseUrl() + "/?" + HASH_KEY + "=" + token;
    }

    @Override
    public void start(int timeout, boolean daemon) throws IOException {
        // Connect to the ChromeCast and start serving media
        synchronized (mLock) {
            mCastReceiverNeedsUpgrade = true;
        }
        int retry = 0;
        while (mCastReceiverNeedsUpgrade) {
            try {
                mChromecast.registerListener(mCastEventListener);
                mChromecast.connect();

                // Launch the app
                Status status = mChromecast.getStatus();
                if (!status.isAppRunning(PHOTOPHASE_APP_ID)) {
                    mChromecast.launchApp(PHOTOPHASE_APP_ID);
                }

                // And now configure the app and wait for ready event
                sendConfiguration();
                synchronized (mLock) {
                    if (!mCastReady) {
                        try {
                            mLock.wait(5000L);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }
                }

            } catch (Exception e) {
                throw new IOException(e);
            }

            // Check that app is ready
            if (!mCastReady) {
                throw new IOException("Cast app not ready.");
            }

            // Check if we are running a valid version of the client receiver
            if (retry == 0 && mCastReceiverNeedsUpgrade) {
                Status status = mChromecast.getStatus();
                if (status.isAppRunning(PHOTOPHASE_APP_ID)) {
                    mChromecast.stopApp();
                }
            }
            retry++;
        }

        // Start the server
        try {
            mTimeout = timeout;
            mDaemon = daemon;
            super.start(timeout, daemon);
        } catch (IOException ex) {
            stop(false);
            throw ex;
        }

        Log.d(TAG, "Cast server connected and running at " + getServerBaseUrl());
        mIsConnected = true;
    }

    protected void send(String path) throws IOException {
        File f = new File(path);
        if (!f.isFile()) {
            throw new IOException("Argument is not a file");
        }

        // Get bitmap dimensions
        Rect r = BitmapUtils.getBitmapDimensions(f);
        if (r == null) {
            throw new IOException("File not exists " + f.getAbsolutePath());
        }

        // Relaunch the app
        if (!safelyCheckIfAppIsRunning()) {
            mChromecast.launchApp(PHOTOPHASE_APP_ID);
        }

        // Notify the user that we are interacting with the device
        CastNotification.showNotification(mContext, mCurrentlyPlaying, new CastStatusInfo());

        // Authorize the request
        String token = UUID.randomUUID().toString();
        mRequests.put(token, path);

        String url = createAuthorizedUrl(token);
        String name = CastUtils.getTrackName(f);
        String album = CastUtils.getAlbumName(f);
        CastMessages.Cast cast = new CastMessages.Cast(url, token, name, album, r.width(), r.height());
        printRequestMessage(cast);
        mChromecast.send(PHOTOPHASE_NAMESPACE, cast);
    }

    protected void sendConfiguration() throws IOException {
        CastMessages.Configuration config = new CastMessages.Configuration();
        config.mName = mContext.getString(R.string.app_name);
        config.mLabel = mContext.getString(R.string.cast_app_description);
        config.mDeviceName = mChromecast.getName();
        config.mIcon = CastUtils.getIconResource();
        config.mShowTime = PreferencesProvider.Preferences.Cast.isShowTime(mContext);
        config.mShowWeather = PreferencesProvider.Preferences.Cast.isShowWeather(mContext);
        config.mShowLogo = PreferencesProvider.Preferences.Cast.isShowLogo(mContext);
        config.mShowTrack = PreferencesProvider.Preferences.Cast.isShowTrack(mContext);
        config.mCropCenter = !PreferencesProvider.Preferences.Cast.isKeepAspectRatio(mContext);
        config.mBlurBackground = PreferencesProvider.Preferences.Cast.isBlurredBackground(mContext);
        config.mLoadingMsg = mContext.getString(R.string.cast_loading_msg);
        printRequestMessage(config);
        if (safelyCheckIfAppIsRunning()) {
            mChromecast.send(PHOTOPHASE_NAMESPACE, config);
        }
    }

    protected void sendStopCast() throws IOException {
        CastMessages.Stop stop = new CastMessages.Stop();
        if (safelyCheckIfAppIsRunning()) {
            mChromecast.send(PHOTOPHASE_NAMESPACE, stop);
        }
        setCurrentlyPlaying(null, false, null);
    }

    public void stop(boolean closed) {
        if (!mIsConnected) {
            return;
        }

        // Stop the chromecast
        if (!closed) {
            try {
                if (mChromecast.isAppRunning(PHOTOPHASE_APP_ID)) {
                    mChromecast.stopApp();
                }
            } catch (Exception ex) {
                // Ignore
            }
        }
        try {
            mChromecast.unregisterListener(mCastEventListener);
            mChromecast.disconnect();
        } catch (IOException ex) {
            // Ignore
        }

        // Stop the server
        super.stop();
        mIsConnected = false;
        setCurrentlyPlaying(null, true, null);
    }

    private void reconnect() throws IOException {
        // Restart the server
        stop(false);
        start(mTimeout, mDaemon);
    }

    @Override
    public void makeSecure(SSLServerSocketFactory sslServerSocketFactory, String[] sslProtocols) {
        super.makeSecure(sslServerSocketFactory, sslProtocols);
        mIsSecure = true;
    }

    @Override
    @SuppressWarnings({"ConstantConditions"})
    public synchronized Response serve(IHTTPSession session) {
        // Acquire a wakelock while serving the file
        mCpuWakeLock.acquire(45000L);

        // We only allow request coming from the ChromeCast device we bound to
        if (!isAuthorized(session.getRemoteIpAddress())) {
            return createFailureResponse(Response.Status.UNAUTHORIZED);
        }

        // Check we received a valid request before serve it
        final Map<String, List<String>> params = session.getParameters();
        if (!params.containsKey(HASH_KEY)) {
            return createFailureResponse(Response.Status.BAD_REQUEST);
        }
        final String hash = params.isEmpty() ? null : params.get(HASH_KEY).get(0);
        if (TextUtils.isEmpty(hash)) {
            return createFailureResponse(Response.Status.BAD_REQUEST);
        }
        if (!mRequests.containsKey(hash)) {
            return createFailureResponse(Response.Status.FORBIDDEN);
        }
        File f = new File(mRequests.get(hash));
        String mimeType = CastUtils.getTrackMimeType(f);
        if (TextUtils.isEmpty(mimeType)) {
            return createFailureResponse(Response.Status.FORBIDDEN);
        }

        // Full quality or compressed?
        try {
            if (PreferencesProvider.Preferences.Cast.isFullQuality(mContext)) {
                // Full quality
                Log.d(TAG, "Sent success response " + f);
                return newChunkedResponse(
                        Response.Status.OK,
                        mimeType,
                        new BufferedInputStream(new FileInputStream(f), 4096));

            } else {
                // Compress to webp format
                long start = System.currentTimeMillis();
                Rect r = BitmapUtils.getBitmapDimensions(f);
                BitmapUtils.adjustRectToMinimumSize(r, BitmapUtils.calculateMaxAvailableSize(mContext));
                Bitmap src = BitmapUtils.createUnscaledBitmap(f, r.width(), r.height(), 1);
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    src.compress(Bitmap.CompressFormat.WEBP, 60, out);

                    long end = System.currentTimeMillis();
                    Log.d(TAG, "Compressed " + f + " to webp in " + (end - start) + "ms"
                        + "; dimensions " + src.getWidth() + "x" + src.getHeight()
                        + "; size: " + out.size() + "; original: "  + BitmapUtils.byteSizeOf(src));

                    Log.d(TAG, "Sent success response" + f);
                    return newChunkedResponse(
                            Response.Status.OK,
                            mimeType,
                            new BufferedInputStream(
                                    new ByteArrayInputStream(out.toByteArray()), 4096));

                } finally {
                    src.recycle();
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to response request: " + f, ex);
            return createFailureResponse(Response.Status.INTERNAL_ERROR);
        } finally {
            // Release the wakelock
            if (mCpuWakeLock.isHeld()) {
                mCpuWakeLock.release();
            }
        }
    }

    private boolean isAuthorized(String remote) {
        return mChromecast.getAddress().equalsIgnoreCase(remote);
    }

    private static String resolveCastServerAddress(ChromeCast chromecast) {
        try {
            Enumeration<NetworkInterface> e =
                    NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    String ipMask = ia.getAddress().getHostAddress()
                            + "/" + ia.getNetworkPrefixLength();
                    CIDRUtils cidr = new CIDRUtils(ipMask);
                    if (cidr.isInRange(chromecast.getAddress())) {
                        return ia.getAddress().getHostAddress();
                    }
                }
            }
        } catch (IOException ex) {
            // Ignore
        }
        return null;
    }

    private Response createFailureResponse(Response.Status status) {
        return newFixedLengthResponse(status, NanoHTTPD.MIME_HTML,
                "<html><body><b>" + status.getDescription() + "</b></body></html>");
    }

    private void printRequestMessage(CastMessages.BaseMessage request) {
        try {
            Log.d(TAG, "Cast Request Message: " + MAPPER.writeValueAsString(request));
        } catch (IOException e) {
            // Ignore
        }
    }

    private void setCurrentlyPlaying(File media, boolean hide, CastStatusInfo status) {
        mCurrentlyPlaying = media;
        mLastStatus = status;
        if (hide) {
            CastNotification.hideNotification(mContext);
            mLastStatus = null;
        }
    }

    private boolean safelyCheckIfAppIsRunning() {
        try {
            return mChromecast.isAppRunning(PHOTOPHASE_APP_ID);
        } catch (Exception ex) {
            // Ignore
        }

        // If we lost connectivity
        if (CastUtils.testConnectivity(mChromecast)) {
            try {
                mChromecast.disconnect();
            } catch (Exception ex) {
                // Ignore
            }
            try {
                mChromecast.connect();
            } catch (Exception ex) {
                return false;
            }

            try {
                reconnect();
            } catch (Exception ex) {
                return false;
            }

            try {
                return mChromecast.isAppRunning(PHOTOPHASE_APP_ID);
            } catch (Exception ex) {
                return false;
            }
        }
        return false;
    }
}
