/*
 * Copyright (C) 2015 Jorge Ruesga
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

package com.ruesga.android.wallpapers.photophase.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.ruesga.android.wallpapers.photophase.utils.BitmapUtils;

import java.io.File;

/**
 * A class for load images associated to a ImageView in background.
 */
public class AsyncPictureLoaderTask extends AsyncTask<File, Void, Drawable> {

    /**
     * Notify whether the picture was loaded
     */
    public static abstract class OnPictureLoaded {
        Object[] mRefs;

        /**
         * Constructor of <code>OnPictureLoaded</code>
         *
         * @param refs References to notify
         */
        public OnPictureLoaded(Object...refs) {
            super();
            mRefs = refs;
        }

        /**
         * Invoked when a picture is loaded
         *
         * @param o The original object reference
         * @param drawable The drawable
         */
        public abstract void onPictureLoaded(Object o, Drawable drawable);
    }

    private final Context mContext;
    private final ImageView mView;
    private final OnPictureLoaded mCallback;

    /**
     * Constructor of <code>AsyncPictureLoaderTask</code>
     *
     * @param context The current context
     * @param v The associated view
     */
    public AsyncPictureLoaderTask(Context context, ImageView v) {
        this(context, v, null);
    }

    /**
     * Constructor of <code>AsyncPictureLoaderTask</code>
     *
     * @param context The current context
     * @param v The associated view
     * @param callback A callback to notify when the picture was loaded
     */
    public AsyncPictureLoaderTask(Context context, ImageView v, OnPictureLoaded callback) {
        super();
        mContext = context;
        mView = v;
        mCallback = callback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Drawable doInBackground(File... params) {
        int width = mView.getMeasuredWidth();
        int height = mView.getMeasuredHeight();
        Bitmap bitmap = BitmapUtils.decodeBitmap(params[0], width, height);
        if (bitmap != null) {
            Drawable dw = new BitmapDrawable(mContext.getResources(), bitmap);
            if (mCallback != null) {
                for (Object o : mCallback.mRefs) {
                    if (!isCancelled()) {
                        mCallback.onPictureLoaded(o, dw);
                    }
                }
            }
            return dw;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostExecute(Drawable result) {
        if (!isCancelled()) {
            mView.setImageDrawable(result);
        }
    }
}
