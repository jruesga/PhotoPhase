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

package org.cyanogenmod.wallpapers.photophase.widgets;

import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask.Status;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import org.cyanogenmod.wallpapers.photophase.R;
import org.cyanogenmod.wallpapers.photophase.tasks.AsyncPictureLoaderTask;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A view that contains all the pictures of an album
 */
public class PicturesView extends HorizontalScrollView {

    private HashMap<File, AsyncPictureLoaderTask> mTasks;
    private Handler mHandler;

    /**
     * Constructor of <code>PicturesView</code>.
     *
     * @param context The current context
     */
    public PicturesView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>PicturesView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public PicturesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>PicturesView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public PicturesView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the structures of this class
     */
    private void init() {
        mTasks = new HashMap<File, AsyncPictureLoaderTask>();
        mHandler = new Handler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelTasks();
    }

    /**
     * Method that removes all tasks
     */
    public void cancelTasks() {
        // Cancel all the pending task
        Iterator<AsyncPictureLoaderTask> it = mTasks.values().iterator();
        while (it.hasNext()) {
            AsyncPictureLoaderTask task = it.next();
            if (task.getStatus().compareTo(Status.PENDING) == 0) {
                task.cancel(true);
            }
        }
        mTasks.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        // Estimated velocity (in some moment we must obtain some scrolling with an estimated
        // velocity below of 3)
        int velocity = Math.abs(l - oldl);
        if (velocity <= 3) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    requestLoadOfPendingPictures();
                }
            });
        }
    }

    /**
     * Method invoked when the view is displayed
     */
    public void onShow() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                requestLoadOfPendingPictures();
            }
        });
    }

    /**
     * Method that load in background all visible and pending pictures
     */
    /*package*/ void requestLoadOfPendingPictures() {
        // Get the visible rect
        Rect r = new Rect();
        getHitRect(r);

        // Get all the image views
        ViewGroup vg = (ViewGroup)getChildAt(0);
        int count = vg.getChildCount();
        for (int i = 0; i < count; i++) {
            ViewGroup picView = (ViewGroup)vg.getChildAt(i);
            File image = new File((String)picView.getTag());
            if (picView.getLocalVisibleRect(r) && !mTasks.containsKey(image)) {
                ImageView iv = (ImageView)picView.findViewById(R.id.picture_thumbnail);
                AsyncPictureLoaderTask task = new AsyncPictureLoaderTask(getContext(), iv);
                task.execute(image);
                mTasks.put(image, task);
            }
        }
    }
}
