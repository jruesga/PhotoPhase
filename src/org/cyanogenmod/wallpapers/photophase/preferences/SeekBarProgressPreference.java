/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.cyanogenmod.wallpapers.photophase.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.cyanogenmod.wallpapers.photophase.R;

/**
 * A preference with a seekbar widget that display the progress
 */
public class SeekBarProgressPreference extends SeekBarPreference {

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

    private String mFormat;
    private OnDisplayProgress mOnDisplayProgress;

    TextView mTextView;

    /**
     * Constructor of <code>SeekBarProgressPreference</code>
     *
     * @param context The current context
     * @param attrs The attributes of the view
     * @param defStyle The resource with the style
     */
    public SeekBarProgressPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Constructor of <code>SeekBarProgressPreference</code>
     *
     * @param context The current context
     * @param attrs The attributes of the view
     */
    public SeekBarProgressPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>SeekBarProgressPreference</code>
     *
     * @param context The current context
     */
    public SeekBarProgressPreference(Context context) {
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
    protected void onBindView(View view) {
        super.onBindView(view);
        mTextView = (TextView) view.findViewById(R.id.text);
        setText();
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
        setText();
    }

    /**
     * Method that displays the progress value
     */
    private void setText() {
        if (mTextView != null) {
            String value = String.valueOf(getProgress());
            if (mOnDisplayProgress != null) {
                value = mOnDisplayProgress.onDisplayProgress(getProgress());
            }
            mTextView.setText(String.format(mFormat, value));
        }
    }

    /**
     * Method that sets the callback to intercept the progress value before it will be
     * displayed on screen.
     *
     * @param onDisplayProgress The callback
     */
    public void setOnDisplayProgress(OnDisplayProgress onDisplayProgress) {
        this.mOnDisplayProgress = onDisplayProgress;
    }

    /**
     * Method that set the format of the progress
     *
     * @param format The format of the string progress
     */
    public void setFormat(String format) {
        mFormat = format;
    }
}
