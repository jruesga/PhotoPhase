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

package com.ruesga.android.wallpapers.photophase.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.github.danielnilsson9.colorpickerview.view.ColorPanelView;
import com.github.danielnilsson9.colorpickerview.view.ColorPickerView;
import com.ruesga.android.wallpapers.photophase.AndroidHelper;
import com.ruesga.android.wallpapers.photophase.R;

import java.util.Locale;

/**
 * A {@link Preference} that allow to select/pick a color in a new window dialog.
 */
public class ColorPickerPreference extends DialogPreference {

    private AlertDialog mDialog;

    private ColorPanelView mColorPicker;
    private int mColor;

    private static class ColorDialogView extends FrameLayout implements TextWatcher {
        private EditText mColorText;
        private ColorPickerView mColorPicker;
        private ColorPanelView mCurrentColor;
        private ColorPanelView mNewColor;

        private int mMaxLayoutWidth;
        private int mMaxLayoutHeight;

        private boolean mIgnoreTextChanged;

        public ColorDialogView(Context context) {
            super(context);
            init();
        }

        private void init() {
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            mMaxLayoutWidth = mMaxLayoutHeight =
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 550, metrics);

            LayoutInflater inflater = LayoutInflater.from(getContext());
            View v = inflater.inflate(R.layout.color_picker_pref_dialog_view, this, false);
            mColorText = (EditText) v.findViewById(R.id.color_picker_pref_color_text);
            mColorPicker = (ColorPickerView) v.findViewById(R.id.color_picker_pref_color_picker);
            mCurrentColor = (ColorPanelView) v.findViewById(R.id.color_picker_pref_color_current);
            mNewColor = (ColorPanelView) v.findViewById(R.id.color_picker_pref_color_new);

            // Configure the color picker with alpha slider
            mColorPicker.setAlphaSliderVisible(true);
            mColorPicker.setAlphaSliderText(R.string.color_picker_alpha_slider_text);
            mColorPicker.setOnColorChangedListener(new ColorPickerView.OnColorChangedListener() {
                @Override
                public void onColorChanged(int newColor) {
                    mNewColor.setColor(newColor);
                    mIgnoreTextChanged = true;
                    mColorText.setText(String.format("%06X", newColor));
                    mIgnoreTextChanged = false;
                }
            });

            // Allow reset by clicking the current color
            mCurrentColor.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    setColor(mCurrentColor.getColor());
                }
            });

            // Configure the edittext and listen for text changes (only allow valid hex colors)
            mIgnoreTextChanged = false;
            InputFilter[] filters = new InputFilter[2];
            filters[0] = new InputFilter.LengthFilter(8);
            filters[1] = new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source,
                        int start, int end, Spanned dest, int dstart, int dend) {
                    if (start >= end) return "";
                    String s = source.subSequence(start, end).toString();
                    StringBuilder sb = new StringBuilder();
                    int cc = s.length();
                    for (int i = 0; i < cc; i++) {
                        char c = s.charAt(i);
                        if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') ||
                                (c >= 'A' && c <= 'F')) {
                            sb.append(c);
                        }
                    }
                    return sb.toString().toUpperCase(Locale.getDefault());
                }
            };
            mColorText.setFilters(filters);
            mColorText.addTextChangedListener(this);

            addView(v);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
            if(mMaxLayoutWidth < measuredWidth) {
                int measureMode = MeasureSpec.getMode(widthMeasureSpec);
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxLayoutWidth, measureMode);
            }
            int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
            if(mMaxLayoutHeight < measuredHeight) {
                int measureMode = MeasureSpec.getMode(heightMeasureSpec);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxLayoutHeight, measureMode);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        public int getColor() {
            return mColorPicker.getColor();
        }

        public void setColor(int newColor) {
            setColor(newColor, false);
        }

        private void setColor(int newColor, boolean user) {
            mColorPicker.setColor(newColor, false);
            mCurrentColor.setColor(newColor);
            mNewColor.setColor(newColor);
            if (!user) {
                mColorText.setText(String.format("%06X", newColor));
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Ignore
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Ignore
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!mIgnoreTextChanged && s.length() == 8) {
                try {
                    setColor(Color.parseColor("#" + s.toString()), true);
                } catch (Exception e) {/**NON BLOCK**/}
            }
        }
    }

    /**
     * Constructor of <code>ColorPickerPreference</code>
     *
     * @param context The current context
     */
    public ColorPickerPreference(Context context) {
        this(context, null);
    }

    /**
     * Constructor of <code>ColorPickerPreference</code>
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the preference.
     */
    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.color_picker_pref_item);
    }

    /**
     * Returns the color of the picker.
     *
     * @return The color of the picker.
     */
    public int getColor() {
        return mColor;
    }

    /**
     * Sets the color of the picker and saves it to the {@link SharedPreferences}.
     *
     * @param color The new color.
     */
    public void setColor(int color) {
        // Always persist/notify the first time; don't assume the field's default of false.
        final boolean changed = mColor != color;
        if (changed) {
            mColor = color;
            // when called from onSetInitialValue the view is still not set
            if (mColorPicker != null) {
                mColorPicker.setColor(color);
            }
            persistInt(color);
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getColor(index, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        int def = 0;
        if (defaultValue != null) {
            def = (int) defaultValue;
        }
        setColor(restoreValue ? getPersistedInt(def) : def);
    }

    protected void onPrepareDialogBuilderCompat(AlertDialog.Builder builder) {
        final ColorDialogView v = new ColorDialogView(getContext());
        v.setColor(mColor);
        builder.setView(v);

        // The color is selected by the user and confirmed by clicking ok
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            @SuppressWarnings("synthetic-access")
            public void onClick(DialogInterface dialog, int which) {
                int color = v.getColor();
                if (callChangeListener(color)) {
                    setColor(color);
                }
                dialog.dismiss();
            }
        });

    }

    @Override
    protected void showDialog(Bundle state) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(getDialogTitle())
                .setIcon(getDialogIcon())
                .setPositiveButton(getPositiveButtonText(), this)
                .setNegativeButton(getNegativeButtonText(), this);
        onPrepareDialogBuilderCompat(builder);
        AndroidHelper.tryRegisterActivityDestroyListener(getPreferenceManager(), this);
        mDialog = builder.create();
        mDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mDialog.setOnDismissListener(this);
        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.show();
        mDialog.getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    @Override
    protected void onClick() {
        if (mDialog != null && mDialog.isShowing()) return;
        showDialog(null);
    }

    public void onActivityDestroy() {
        if (mDialog == null || !mDialog.isShowing()) {
            return;
        }
        mDialog.dismiss();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(false);
        mDialog = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);
        View v = view.findViewById(R.id.color_picker);
        if (v != null && v instanceof ColorPanelView) {
            mColorPicker = (ColorPanelView)v;
            mColorPicker.setColor(mColor);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mDialog == null || !mDialog.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.isDialogShowing = true;
        myState.dialogBundle = mDialog.onSaveInstanceState();
        myState.color = getColor();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setColor(myState.color);
        if (myState.isDialogShowing) {
            showDialog(myState.dialogBundle);
        }
    }

    /**
     * A class for managing the instance state of a {@link ColorPickerPreference}.
     */
    static class SavedState extends BaseSavedState {
        boolean isDialogShowing;
        Bundle dialogBundle;
        int color;

        /**
         * Constructor of <code>SavedState</code>
         *
         * @param source The source
         */
        public SavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
            dialogBundle = source.readBundle(getClass().getClassLoader());
            color = source.readInt();
        }

        /**
         * Constructor of <code>SavedState</code>
         *
         * @param superState The parcelable state
         */
        public SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
            dest.writeBundle(dialogBundle);
            dest.writeInt(color);
        }

        /**
         * A class that generates instances of the <code>SavedState</code> class from a Parcel.
         */
        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
