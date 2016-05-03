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

package com.ruesga.android.wallpapers.photophase.adapters;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.ArrayRes;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.effects.Effects;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions;
import com.ruesga.android.wallpapers.photophase.widgets.LivePreviewView;

import java.util.Set;

public class LivePreviewAdapter extends PagerAdapter {

    public interface LivePreviewCallback {
        Set<String> getSelectedEntries();
        void setSelectedEntries(Set<String> entries);

        Transitions.TRANSITIONS getTransitionForPosition(int position);
        Effects.EFFECTS getEffectForPosition(int position);
    }

    private final Context mContext;
    private final String[] mLabels;
    private final String[] mEntries;

    private final LivePreviewCallback mCallback;
    private final Set<String> mSelectedEntries;

    public LivePreviewAdapter(Context context, @ArrayRes int labels,
            @ArrayRes int entries, LivePreviewCallback cb) {
        super();
        mContext = context;
        mLabels = context.getResources().getStringArray(labels);
        mEntries = context.getResources().getStringArray(entries);
        mCallback = cb;
        mSelectedEntries = cb.getSelectedEntries();
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        LayoutInflater li = LayoutInflater.from(mContext);
        final View view = li.inflate(R.layout.live_preview_view, container, false);
        SwitchCompat check = (SwitchCompat) view.findViewById(R.id.check);
        check.setChecked(mSelectedEntries.contains(mEntries[position]));
        check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSelectedEntries.add(mEntries[position]);
                } else {
                    mSelectedEntries.remove(mEntries[position]);
                }
                mCallback.setSelectedEntries(mSelectedEntries);
            }
        });

        TextView label = (TextView) view.findViewById(R.id.label);
        label.setText(mLabels[position]);
        LivePreviewView preview = (LivePreviewView) view.findViewById(R.id.preview);
        preview.setEffect(mCallback.getEffectForPosition(position));
        preview.setTransition(mCallback.getTransitionForPosition(position));

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        LivePreviewView v = (LivePreviewView) ((View) object).findViewById(R.id.preview);
        v.recycle();
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return mEntries.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public float getPageWidth(int position) {
        int orientation = mContext.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT ? 1.0f : 0.5f;
    }

}
