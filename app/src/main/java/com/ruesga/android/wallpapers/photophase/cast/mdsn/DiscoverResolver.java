/* The MIT License (MIT)
 * Copyright (c) 2015 YouView Ltd
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ruesga.android.wallpapers.photophase.cast.mdsn;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>Uses Android's {@link NsdManager} to perform mDNS Service Discovery. Additionally makes use of
 * {@link MDNSDiscover} to query the TXT record of discovered services (something which is missing
 * from {@link NsdManager} due to a <a href="https://code.google.com/p/android/issues/detail?id=136099">bug</a>)</p>
 *
 * <p>This class presents a simplified client API compared with accessing {@link NsdManager}
 * directly.</p>
 *
 * <p>Another feature is <em>service visibility debouncing</em>: sometimes
 * {@link NsdManager.DiscoveryListener#onServiceLost(NsdServiceInfo)} occurs
 * then shortly afterwards the same service is reported again in
 * {@link NsdManager.DiscoveryListener#onServiceFound(NsdServiceInfo)}. Use the
 * {@code debounceMillis} value in the constructor to configure a tolerance to this - removed
 * services are not notified to the listener until this time elapses without the service
 * reappearing.</p>
 */
@TargetApi(value= Build.VERSION_CODES.JELLY_BEAN)
public class DiscoverResolver {

    private static final String TAG = DiscoverResolver.class.getSimpleName();
    private static final int RESOLVE_TIMEOUT = 1000;

    public interface Listener {
        void onServicesChanged(Map<String, MDNSDiscover.Result> services);
    }

    private final MapDebouncer<String, Object> mDebouncer;

    private final Context mContext;
    private final String mServiceType;
    private final HashMap<String, MDNSDiscover.Result> mServices = new HashMap<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Listener mListener;
    private boolean mStarted;
    private boolean mTransitioning;
    private ResolveTask mResolveTask;
    private final Map<String, NsdServiceInfo> mResolveQueue = new LinkedHashMap<>();

    /**
     * Equivalent to {@link #DiscoverResolver(Context, String, Listener, int)} with a
     * {@code debounceMillis} of 0.
     */
    public DiscoverResolver(Context context, String serviceType, Listener listener) {
        this(context, serviceType, listener, 0);
    }

    /**
     * @param context the Context to run in
     * @param serviceType mDNS service type such as {@code "_example._tcp"}
     * @param listener to receive updates to visible services
     * @param debounceMillis time to delay service signalling of services that may quickly disappear
     *                       then reappear. See {@link DiscoverResolver} for details.
     */
    public DiscoverResolver(Context context, String serviceType, Listener listener, int debounceMillis) {
        if     (context == null) throw new NullPointerException("context was null");
        if (serviceType == null) throw new NullPointerException("serviceType was null");
        if    (listener == null) throw new NullPointerException("listener was null");

        mContext = context;
        mServiceType = serviceType;
        mListener = listener;

        mDebouncer = new MapDebouncer<>(debounceMillis, new MapDebouncer.Listener<String, Object>() {
            @Override
            public void put(String name, Object o) {
                if (o != null) {
                    Log.d(TAG, "add: " + name);
                    synchronized (mResolveQueue) {
                        mResolveQueue.put(name, null);
                    }
                    startResolveTaskIfNeeded();
                } else {
                    Log.d(TAG, "remove: " + name);
                    synchronized (DiscoverResolver.this) {
                        synchronized (mResolveQueue) {
                            mResolveQueue.remove(name);
                        }
                        if (mStarted) {
                            if (mServices.remove(name) != null) {
                                dispatchServicesChanged();
                            }
                        }
                    }
                }
            }
        });
    }

    public synchronized void start() {
        if (mStarted) {
            throw new IllegalStateException();
        }
        if (!mTransitioning) {
            discoverServices(mServiceType, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            mTransitioning = true;
        }
        mStarted = true;
    }

    public synchronized void stop() {
        if (!mStarted) {
            throw new IllegalStateException();
        }
        if (!mTransitioning) {
            stopServiceDiscovery(mDiscoveryListener);
            mTransitioning = true;
        }
        synchronized (mResolveQueue) {
            mResolveQueue.clear();
        }
        mDebouncer.clear();
        mServices.clear();
        mServicesChanged = false;
        mStarted = false;
    }

    private NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {
        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.d(TAG, "onStartDiscoveryFailed() serviceType = [" + serviceType + "], errorCode = [" + errorCode + "]");
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.d(TAG, "onStopDiscoveryFailed() serviceType = [" + serviceType + "], errorCode = [" + errorCode + "]");
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.d(TAG, "onDiscoveryStarted() serviceType = [" + serviceType + "]");
            synchronized (DiscoverResolver.this) {
                if (!mStarted) {
                    stopServiceDiscovery(this);
                } else {
                    mTransitioning = false;
                }
            }
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.d(TAG, "onDiscoveryStopped() serviceType = [" + serviceType + "]");
            if (mStarted) {
                discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, this);
            } else {
                mTransitioning = false;
            }
        }

        @Override
        public void onServiceFound(final NsdServiceInfo serviceInfo) {
            Log.d(TAG, "onServiceFound() serviceInfo = [" + serviceInfo + "]");
            synchronized (DiscoverResolver.this) {
                if (mStarted) {
                    String name = serviceInfo.getServiceName() + "." + serviceInfo.getServiceType() + "local";
                    mDebouncer.put(name, DUMMY);
                }
            }
        }

        @Override
        public void onServiceLost(final NsdServiceInfo serviceInfo) {
            Log.d(TAG, "onServiceLost() serviceInfo = [" + serviceInfo + "]");
            synchronized (DiscoverResolver.this) {
                if (mStarted) {
                    String name = serviceInfo.getServiceName() + "." + serviceInfo.getServiceType() + "local";
                    mDebouncer.put(name, null);
                }
            }
        }
    };

    /**
     * A non-null value that indicates membership in the MapDebouncer, null indicates non-membership
     */
    private Object DUMMY = new Object();

    private boolean mServicesChanged;

    private void dispatchServicesChanged() {
        if (!mStarted) {
            throw new IllegalStateException();
        }
        // Multiple calls to this method are possible before mServicesChangedRunnable executes.
        // We don't post the runnable every time this method is called, instead we set a flag and
        // post only if the flag was previously unset. The runnable clears the flag.
        // In this way, the main thread can coalesce several updates into a single call to
        // onServicesChanged().
        if (!mServicesChanged) {
            mServicesChanged = true;
            mHandler.post(mServicesChangedRunnable);
        }
    }

    private Runnable mServicesChangedRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (DiscoverResolver.this) {
                if (mStarted && mServicesChanged) {
                    @SuppressWarnings("unchecked")
                    Map<String, MDNSDiscover.Result> services = (Map) mServices.clone();
                    mListener.onServicesChanged(services);
                }
                mServicesChanged = false;
            }
        }
    };

    private class ResolveTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            while (!isCancelled()) {
                String serviceName;
                synchronized (mResolveQueue) {
                    Iterator<String> it = mResolveQueue.keySet().iterator();
                    if (!it.hasNext()) {
                        break;
                    }
                    serviceName = it.next();
                    it.remove();
                }
                try {
                    MDNSDiscover.Result result = resolve(serviceName, RESOLVE_TIMEOUT);
                    synchronized (DiscoverResolver.this) {
                        if (mStarted) {
                            mServices.put(serviceName, result);
                            dispatchServicesChanged();
                        }
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mResolveTask = null;
            startResolveTaskIfNeeded();
        }
    }

    private void startResolveTaskIfNeeded() {
        if (mResolveTask == null) {
            synchronized (mResolveQueue) {
                if (!mResolveQueue.isEmpty()) {
                    mResolveTask = new ResolveTask();
                    mResolveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        }
    }

    // default implementation is to delegate to NsdManager
    // tests can stub this to mock the NsdManager
    protected void discoverServices(String serviceType, int protocol, NsdManager.DiscoveryListener listener) {
        ((NsdManager) mContext.getSystemService(Context.NSD_SERVICE)).discoverServices(serviceType, protocol, listener);
    }

    // default implementation is to delegate to NsdManager
    // tests can stub this to mock the NsdManager
    protected void stopServiceDiscovery(NsdManager.DiscoveryListener listener) {
        ((NsdManager) mContext.getSystemService(Context.NSD_SERVICE)).stopServiceDiscovery(listener);
    }

    // default implementation is to delegate to MDNSDiscover
    // tests can stub this to mock it
    protected MDNSDiscover.Result resolve(String serviceName, int resolveTimeout) throws IOException {
        return MDNSDiscover.resolve(serviceName, resolveTimeout);
    }
}
