/*
 * Copyright (C) 2013 Jorge Ruesga
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

package com.ruesga.android.wallpapers.photophase.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.model.Dispositions;
import com.ruesga.android.wallpapers.photophase.widgets.DispositionView;
import com.ruesga.android.wallpapers.photophase.widgets.DispositionView.OnFrameSelectedListener;
import com.ruesga.android.wallpapers.photophase.widgets.ResizeFrame;

import java.util.List;

/**
 * An {@link PagerAdapter} implementation for display all current templates
 */
public class DispositionAdapter extends PagerAdapter {

    final List<Dispositions> mDispositions;
    private final ResizeFrame mResizeFrame;
    private final OnFrameSelectedListener mCallback;

    private final SparseArray<DispositionView> mCurrentViews;

    private LayoutInflater mInflater;

    boolean mFirstAnimation;

    /**
     * Constructor of <code>DispositionAdapter</code>.
     *
     * @param ctx The current context
     * @param dispositions An array with all dispositions
     * @param resizeFrame The resize frame
     * @param callback The callback where return selection events
     */
    public DispositionAdapter(Context ctx, List<Dispositions> dispositions,
            ResizeFrame resizeFrame, OnFrameSelectedListener callback) {
        super();
        mDispositions = dispositions;
        mResizeFrame = resizeFrame;
        mCallback = callback;
        mFirstAnimation = true;
        mInflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCurrentViews = new SparseArray<DispositionView>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return mDispositions.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        final DispositionView view = (DispositionView)mInflater.inflate(
                R.layout.disposition_view, null);
        if (position == 0) {
            view.setResizeFrame(mResizeFrame);
            view.setOnFrameSelectedListener(mCallback);
        }
        view.post(new Runnable() {
            @Override
            public void run() {
                view.setDispositions(mDispositions.get(position),
                        position == 0 &&  mFirstAnimation);
                mFirstAnimation = false;
            }
        });
        ((ViewPager)container).addView(view, 0);
        mCurrentViews.put(position, view);
        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        mCurrentViews.remove(position);
        ((ViewPager)container).removeView((View)object);
    }

    /**
     * Method that returns the current view
     *
     * @param position The position of the item to return
     * @return DispositionView The view or null if is not instance
     */
    public DispositionView getView(int position) {
        return mCurrentViews.get(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((View)object) || view == mResizeFrame;
    }

}
