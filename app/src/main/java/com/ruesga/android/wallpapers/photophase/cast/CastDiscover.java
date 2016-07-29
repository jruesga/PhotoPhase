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

import com.ruesga.android.wallpapers.photophase.AndroidHelper;
import com.ruesga.android.wallpapers.photophase.cast.mdsn.DiscoverResolver;
import com.ruesga.android.wallpapers.photophase.cast.mdsn.MDNSDiscover;

import java.util.Map;

import su.litvak.chromecast.api.v2.ChromeCast;

public class CastDiscover {

    public static final String SERVICE_TYPE = "_googlecast._tcp.";

    public interface DeviceResolverListener {
        void onDeviceDiscovered(ChromeCast device);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final DiscoverResolver.Listener mResolverListener = new DiscoverResolver.Listener() {
        @Override
        public void onServicesChanged(Map<String, MDNSDiscover.Result> services) {
            for (MDNSDiscover.Result result : services.values()) {
                if (result.srv != null && result.srv.fqdn != null
                        && result.srv.fqdn.contains(SERVICE_TYPE)){
                    // this is a chromecast device
                    String ip = result.a.ipaddr;
                    int port = result.srv.port;
                    String name = result.txt.dict.get("fn");
                    mListener.onDeviceDiscovered(new ChromeCast(ip, port, name));
                }
            }
        }
    };

    private DiscoverResolver mResolver;
    private DeviceResolverListener mListener;

    public CastDiscover(Context context, DeviceResolverListener listener) {
        if (AndroidHelper.isJellyBeanOrGreater()) {
            mResolver = new DiscoverResolver(context, SERVICE_TYPE, mResolverListener);
            mListener = listener;
        }
    }

    public void startDiscovery() {
        if (AndroidHelper.isJellyBeanOrGreater()) {
            mResolver.start();
        }
    }

    public void stopDiscovery() {
        if (AndroidHelper.isJellyBeanOrGreater()) {
            mResolver.stop();
        }
    }
}
