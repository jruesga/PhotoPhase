/*
 * Copyright (C) 2016 Jorge Ruesga
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.MultiSelectListPreference;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.WindowManager;

import com.ruesga.android.wallpapers.photophase.AndroidHelper;

public class MultiSelectListPreferenceCompat extends MultiSelectListPreference {

    private static class FakeAlertBuilder extends android.app.AlertDialog.Builder {
        private CharSequence[] mItems;
        private boolean[] mCheckedItems;
        private OnMultiChoiceClickListener mOnMultiChoiceClickListener;

        public FakeAlertBuilder(Context context) {
            super(context);
        }

        @Override
        public android.app.AlertDialog.Builder setMultiChoiceItems(
                CharSequence[] items, boolean[] checkedItems, OnMultiChoiceClickListener listener) {
            mItems = items;
            mCheckedItems = checkedItems;
            mOnMultiChoiceClickListener = listener;
            return this;
        }
    }

    private AlertDialog mDialog;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MultiSelectListPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MultiSelectListPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MultiSelectListPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiSelectListPreferenceCompat(Context context) {
        super(context);
    }

    @Override
    protected void showDialog(Bundle state) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(getDialogTitle())
                .setIcon(getDialogIcon())
                .setPositiveButton(getPositiveButtonText(), this)
                .setNegativeButton(getNegativeButtonText(), this);

        FakeAlertBuilder fakeBuilder = new FakeAlertBuilder(getContext());
        onPrepareDialogBuilder(fakeBuilder);
        builder.setMultiChoiceItems(fakeBuilder.mItems,
                fakeBuilder.mCheckedItems, fakeBuilder.mOnMultiChoiceClickListener);
        AndroidHelper.tryRegisterActivityDestroyListener(getPreferenceManager(), this);
        mDialog = builder.create();
        if (mDialog.getWindow() != null) {
            mDialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        mDialog.setOnDismissListener(this);
        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.show();
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
        super.onDialogClosed(positiveResult);
        mDialog = null;
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
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            try {
                super.onRestoreInstanceState(state);
            } catch (IllegalArgumentException e) {
                // https://code.google.com/p/android/issues/detail?id=70088
            }
            return;
        }

        SavedState myState = (SavedState) state;
        try {
            super.onRestoreInstanceState(myState.getSuperState());
        } catch (IllegalArgumentException e) {
            // https://code.google.com/p/android/issues/detail?id=70088
        }
        if (myState.isDialogShowing) {
            showDialog(myState.dialogBundle);
        }
    }

    private static class SavedState extends BaseSavedState {
        boolean isDialogShowing;
        Bundle dialogBundle;

        public SavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
            dialogBundle = source.readBundle(getClass().getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
            dest.writeBundle(dialogBundle);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
