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

import android.animation.Animator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.ruesga.android.wallpapers.photophase.AndroidHelper;
import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.borders.Borders;
import com.ruesga.android.wallpapers.photophase.effects.Effects;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions;
import com.ruesga.android.wallpapers.photophase.widgets.LivePreviewView;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.util.Set;

public class LivePreviewAdapter extends PagerAdapter {

    public interface LivePreviewCallback {
        Set<String> getSelectedEntries();
        void setSelectedEntries(Set<String> entries);

        Transitions.TRANSITIONS getTransitionForId(int id);
        Effects.EFFECTS getEffectForId(int id);
        Borders.BORDERS getBorderForId(int id);

        boolean hasSettings(int id);
        void configureSettings(int id, DiscreteSeekBar seekBar);
        void saveSettings(int id, int newVal);
    }

    private static class ShowAnimatorListener implements Animator.AnimatorListener {
        private final View mTarget;

        public ShowAnimatorListener(View v) {
            mTarget = v;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            mTarget.setAlpha(0.0f);
            mTarget.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }
    private static class HideAnimatorListener implements Animator.AnimatorListener {
        private final View mTarget;

        public HideAnimatorListener(View v) {
            mTarget = v;
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mTarget.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }

    private final Context mContext;
    private final String[] mLabels;
    private final String[] mValues;

    private final LivePreviewCallback mCallback;
    private final Set<String> mSelectedEntries;

    public LivePreviewAdapter(Context context, @ArrayRes int labels,
            @ArrayRes int values, LivePreviewCallback cb) {
        super();
        mContext = context;
        Pair<String[], String[]> entries = AndroidHelper.sortEntries(context, labels, values);
        mLabels = entries.first;
        mValues = entries.second;
        mCallback = cb;
        mSelectedEntries = cb.getSelectedEntries();
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        LayoutInflater li = LayoutInflater.from(mContext);
        final View view = li.inflate(R.layout.live_preview_view, container, false);
        SwitchCompat check = view.findViewById(R.id.check);
        check.setChecked(mSelectedEntries.contains(mValues[position]));
        check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSelectedEntries.add(mValues[position]);
                } else {
                    mSelectedEntries.remove(mValues[position]);
                }
                mCallback.setSelectedEntries(mSelectedEntries);
            }
        });

        int id = Integer.valueOf(mValues[position]);
        TextView label = view.findViewById(R.id.label);
        label.setText(mLabels[position]);
        LivePreviewView preview = view.findViewById(R.id.preview);
        preview.setEffect(mCallback.getEffectForId(id));
        preview.setBorder(mCallback.getBorderForId(id));
        preview.setTransition(mCallback.getTransitionForId(id));

        configureSettings(view, id, preview);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        LivePreviewView v = ((View) object).findViewById(R.id.preview);
        v.recycle();
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return mValues.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public float getPageWidth(int position) {
        int orientation = mContext.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT ? 1.0f : 0.5f;
    }

    private void configureSettings(View view, final int id, final LivePreviewView preview) {
        final ImageView settings = view.findViewById(R.id.settings);
        final View settingsBlock = view.findViewById(R.id.settings_block);
        if (mCallback.hasSettings(id)) {
            settings.setColorFilter(ContextCompat.getColor(mContext, R.color.color_accent),
                    PorterDuff.Mode.SRC_ATOP);
            settings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    settingsBlock.animate().alpha(1.0f).setDuration(600L).setListener(
                            new ShowAnimatorListener(settingsBlock)).start();
                    settings.animate().alpha(0.0f).setDuration(600L).setListener(
                            new HideAnimatorListener(settings)).start();
                }
            });

            final View done = view.findViewById(R.id.done);
            done.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    settings.animate().alpha(1.f).setDuration(600L).setListener(
                            new ShowAnimatorListener(settings)).start();
                    settingsBlock.animate().alpha(0.0f).setDuration(600L).setListener(
                            new HideAnimatorListener(settingsBlock)).start();
                }
            });

            final DiscreteSeekBar seekBar = view.findViewById(R.id.strength);
            mCallback.configureSettings(id, seekBar);
            seekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                @Override
                public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int i, boolean b) {
                    mCallback.saveSettings(id, i);
                    preview.recreate();
                }

                @Override
                public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) {
                }

                @Override
                public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
                }
            });
        } else {
            settings.setVisibility(View.GONE);
        }
    }
}
