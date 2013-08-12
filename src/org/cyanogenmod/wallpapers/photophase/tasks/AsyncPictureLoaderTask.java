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

package org.cyanogenmod.wallpapers.photophase.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import org.cyanogenmod.wallpapers.photophase.utils.BitmapUtils;

import java.io.File;

/**
 * A class for load images associated to a ImageView in background.
 */
public class AsyncPictureLoaderTask extends AsyncTask<File, Void, Drawable> {

    private final Context mContext;
    private final ImageView mView;

    /**
     * Constructor of <code>AsyncPictureLoaderTask</code>
     *
     * @param context The current context
     * @param v The associated view
     */
    public AsyncPictureLoaderTask(Context context, ImageView v) {
        super();
        mContext = context;
        mView = v;
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
            return new BitmapDrawable(mContext.getResources(), bitmap);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostExecute(Drawable result) {
        mView.setImageDrawable(result);
    }
}
