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
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.webkit.MimeTypeMap;

import com.ruesga.android.wallpapers.photophase.AndroidHelper;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import su.litvak.chromecast.api.v2.ChromeCast;

public final class CastUtils {

    private static final String ICON_RESOURCE = "icon.png";

    private static final Object sLock = new Object();
    private static boolean sHasDevices;

    public static String getIconResource() {
        return ICON_RESOURCE;
    }

    public static String getTrackMimeType(File f) {
        if (!f.isFile()) {
            return null;
        }

        final String name = f.getName();
        int pos = name.lastIndexOf(".");
        if (pos == -1 || pos == name.length()) {
            return null;
        }
        String extension = f.getName().substring(pos + 1);
        if (TextUtils.isEmpty(extension)) {
            return null;
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    public static String getTrackName(File f) {
        // Try to extract the title from the exif metadata
        String title = null;
        if (AndroidHelper.isNougatOrGreater()) {
            try {
                ExifInterface exif = new ExifInterface(f.getAbsolutePath());

                title = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION);
                if (TextUtils.isEmpty(title)) {
                    title = exif.getAttribute(ExifInterface.TAG_USER_COMMENT);
                }
            } catch (IOException ex) {
                // Ignore
            }
        }
        if (!TextUtils.isEmpty(title)) {
            return title;
        }

        // Use the current file name
        final String name = f.getName();
        int pos = name.lastIndexOf(".");
        if (pos == -1 || pos == name.length()) {
            return null;
        }
        return f.getName().substring(0, pos);
    }

    public static String getAlbumName(File f) {
        return f.getParentFile().getName();
    }

    public static String chromecast2string(ChromeCast device) {
        return device.getAddress() + ":" + device.getPort() + "/" + device.getName();
    }

    public static ChromeCast string2chromecast(String s) {
        String address = s.substring(0, s.indexOf(":"));
        int port = Integer.parseInt(s.substring(s.indexOf(":") + 1, s.indexOf("/")));
        ChromeCast device = new ChromeCast(address, port);
        device.setName(s.substring(s.indexOf("/") + 1));
        return device;
    }

    public static boolean testConnectivity(ChromeCast device) {
        Socket client=new Socket();
        try {
            client.connect(new InetSocketAddress(device.getAddress(), device.getPort()), 1500);
            client.close();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean hasValidCastNetwork(Context context) {
        // ChromeCast only works on wifi networks, so it doesn't made sense to
        // try to seek devices in the current network if it isn't a wifi network
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public static synchronized boolean isNearDevicesAvailable(Context context) {
        sHasDevices = false;

        // Check if there are near devices
        CastDiscover discover = new CastDiscover(context, new CastDiscover.DeviceResolverListener() {
            @Override
            public void onDeviceDiscovered(ChromeCast device) {
                sHasDevices = true;
                synchronized (sLock) {
                    sLock.notify();
                }
            }
        });
        discover.startDiscovery();

        synchronized (sLock) {
            try {
                sLock.wait(DateUtils.SECOND_IN_MILLIS * 10);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        discover.stopDiscovery();

        return sHasDevices;
    }
}
