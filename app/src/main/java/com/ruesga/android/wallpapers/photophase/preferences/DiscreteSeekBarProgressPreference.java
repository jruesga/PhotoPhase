/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2015 Jorge Ruesga
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.ruesga.android.wallpapers.photophase.preferences;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.ruesga.android.wallpapers.photophase.R;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

/**
 * A preference with a discrete seekbar widget that display the progress
 */
@SuppressWarnings("unused")
public class DiscreteSeekBarProgressPreference extends DiscreteSeekBarPreference {

    /**
     * Interface to intercept the progress value to display on screen
     */
    public interface OnDisplayProgress {
        /**
         * Method invoked when a progress value is going to display on screen
         *
         * @param progress The real progress
         * @return The progress to display
         */
        String onDisplayProgress(int progress);
    }

    private boolean mShowPopUpIndicator = true;
    private String mFormat;
    private OnDisplayProgress mOnDisplayProgress;

    private TextView mTextView;


    /**
     * Constructor of <code>DiscreteSeekBarProgressPreference</code>
     *
     * @param context The current context
     * @param attrs The attributes of the view
     * @param defStyle The resource with the style
     */
    @SuppressWarnings("unused")
    public DiscreteSeekBarProgressPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Constructor of <code>DiscreteSeekBarProgressPreference</code>
     *
     * @param context The current context
     * @param attrs The attributes of the view
     */
    @SuppressWarnings("unused")
    public DiscreteSeekBarProgressPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>DiscreteSeekBarProgressPreference</code>
     *
     * @param context The current context
     */
    @SuppressWarnings("unused")
    public DiscreteSeekBarProgressPreference(Context context) {
        super(context);
        init();
    }

    /**
     * Method that initializes the preference
     */
    void init() {
        mFormat = "%s";
        mOnDisplayProgress = null;
        setWidgetLayoutResource(R.layout.preference_widget_seekbar_progress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);
        mTextView = view.findViewById(R.id.text);
        setText(getProgress());
        mSeekBar.setIndicatorPopupEnabled(mShowPopUpIndicator);
    }

    /**
     * Method that set the actual progress
     *
     * @param progress The actual progress
     * @param notifyChanged Whether notify if the progress was changed
     */
    @Override
    protected void setProgress(int progress, boolean notifyChanged) {
        super.setProgress(progress, notifyChanged);
        setText(progress);
    }

    @Override
    public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
        super.onProgressChanged(seekBar, value, fromUser);
        if (fromUser) {
            setText(value);
        }
    }

    @Override
    public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);
        setText(getProgress());
    }

    /**
     * Method that displays the progress value
     */
    private void setText(int progress) {
        if (mTextView != null) {
            String value = mOnDisplayProgress != null
                    ? mOnDisplayProgress.onDisplayProgress(progress) : String.valueOf(progress);
            mTextView.setText(String.format(mFormat, value));
        }
    }

    /**
     * Method that sets the callback to intercept the progress value before it will be
     * displayed on screen.
     *
     * @param onDisplayProgress The callback
     */
    @SuppressWarnings("unused")
    public void setOnDisplayProgress(OnDisplayProgress onDisplayProgress) {
        this.mOnDisplayProgress = onDisplayProgress;
    }

    /**
     * Method that set the format of the progress
     *
     * @param format The format of the string progress
     */
    @SuppressWarnings("unused")
    public void setFormat(String format) {
        mFormat = format;
    }

    /**
     * Method that enabled/disabled the discrete seekBar popup
     */
    @SuppressWarnings("unused")
    public void setShowPopUpIndicator(boolean show) {
        if (mSeekBar != null) {
            mSeekBar.setIndicatorPopupEnabled(show);
        }
        mShowPopUpIndicator = show;
    }
}
