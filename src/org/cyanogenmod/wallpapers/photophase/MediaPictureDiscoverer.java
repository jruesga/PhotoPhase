/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package org.cyanogenmod.wallpapers.photophase;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A class that load asynchronously the paths of all media stored in the device.
 * This class only seek at the specified paths
 */
public class MediaPictureDiscoverer {

    private static final String TAG = "MediaPictureDiscoverer";

    private static final boolean DEBUG = false;

    /**
     * An interface that is called when new data is ready.
     */
    public interface OnMediaPictureDiscoveredListener  {
        /**
         * Called when the data is ready
         *
         * @param mpc The reference to the discoverer
         * @param images All the images paths found
         */
        void onMediaDiscovered(MediaPictureDiscoverer mpc, File[] images);
    }

    /**
     * The asynchronous task for query the MediaStore
     */
    private class AsyncDiscoverTask extends AsyncTask<Void, Void, List<File> > {

        private final ContentResolver mFinalContentResolver;
        private final OnMediaPictureDiscoveredListener mFinalCallback;
        private final Set<String> mFilter;

        /**
         * Constructor of <code>AsyncDiscoverTask</code>
         *
         * @param cr The {@link ContentResolver}
         * @param filter The filter of pictures and albums to retrieve
         * @param cb The {@link OnMediaPictureDiscoveredListener} listener
         */
        public AsyncDiscoverTask(ContentResolver cr, Set<String> filter,
                OnMediaPictureDiscoveredListener cb) {
            super();
            mFinalContentResolver = cr;
            mFinalCallback = cb;
            mFilter = filter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<File> doInBackground(Void...params) {
            try {
                // The columns to read
                final String[] projection = {MediaStore.MediaColumns.DATA};

                // Query external content
                List<File> paths =
                        getPictures(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                projection,
                                null,
                                null);
                if (DEBUG) {
                    int cc = paths.size();
                    Log.v(TAG, "Pictures found (" + cc + "):");
                    for (int i = 0; i < cc; i++) {
                        Log.v(TAG, "\t" + paths.get(i));
                    }
                }
                return paths;

            } catch (Exception e) {
                Log.e(TAG, "AsyncDiscoverTask failed.", e);

                // Return and empty list
                return new ArrayList<File>();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(List<File> result) {
            if (mFinalCallback != null) {
                mFinalCallback.onMediaDiscovered(
                        MediaPictureDiscoverer.this, result.toArray(new File[result.size()]));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onCancelled(List<File> result) {
            // Nothing found
            if (mFinalCallback != null) {
                mFinalCallback.onMediaDiscovered(
                        MediaPictureDiscoverer.this, new File[]{});
            }
        }

        /**
         * Method that return all the media store pictures for the content uri
         *
         * @param uri The content uri where to search
         * @param projection The field data to return
         * @param where A filter
         * @param args The filter arguments
         * @return List<File> The pictures found
         */
        private List<File> getPictures(
                Uri uri, String[] projection, String where, String[] args) {
            List<File> paths = new ArrayList<File>();
            Cursor c = mFinalContentResolver.query(uri, projection, where, args, null);
            if (c != null) {
                try {
                    while (c.moveToNext()) {
                        // Only valid files (those i can read)
                        String p = c.getString(0);
                        if (p != null) {
                            File f = new File(p);
                            if (f.isFile() && f.canRead() && matchFilter(f)) {
                                paths.add(f);
                            }
                        }
                    }
                } finally {
                    try {
                        c.close();
                    } catch (Exception e) {
                        // Ignore: handle exception
                    }
                }
            }
            return paths;
        }

        /**
         * Method that checks if the picture match the preferences filter
         *
         * @param picture The picture to check
         * @return boolean whether the picture match the filter
         */
        private boolean matchFilter(File picture) {
            Iterator<String> it = mFilter.iterator();
            boolean noFilter = true;
            while (it.hasNext()) {
                noFilter = false;
                File filter = new File(it.next());
                if (filter.isDirectory()) {
                    // Album match
                    if (filter.compareTo(picture.getParentFile()) == 0) {
                        return true;
                    }
                } else {
                    // Picture match
                    if (filter.compareTo(picture) == 0) {
                        return true;
                    }
                }
            }
            return noFilter;
        }
    }

    private final Context mContext;
    private final OnMediaPictureDiscoveredListener mCallback;

    private AsyncDiscoverTask mTask;

    /**
     * Constructor of <code>MediaPictureDiscoverer</code>.
     *
     * @param ctx The current context
     * @param callback A callback to returns the data when it gets ready
     */
    public MediaPictureDiscoverer(Context ctx, OnMediaPictureDiscoveredListener callback) {
        super();
        mContext = ctx;
        mCallback = callback;
    }

    /**
     * Method that request a new reload of the media store picture data.
     *
     * @param filter The filter of pictures and albums where to search images
     */
    public synchronized void discover(Set<String> filter) {
        if (mTask != null && !mTask.isCancelled()) {
            mTask.cancel(true);
        }
        mTask = new AsyncDiscoverTask(mContext.getContentResolver(), filter, mCallback);
        mTask.execute();
    }

    /**
     * Method that destroy the references of this class
     */
    public void recycle() {
        if (mTask != null && !mTask.isCancelled()) {
            mTask.cancel(true);
        }
    }

}
