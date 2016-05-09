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

package com.ruesga.android.wallpapers.photophase.model;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents an album
 */
public class Album implements Comparable<Album>, Cloneable {

    private Drawable mIcon;
    private String mPath;
    private String mName;
    private String mDate;
    private boolean mSelected;
    private List<Picture> mItems;
    // We have this array for performance access. Do not forget to sync with Picture#selected
    private List<String> mSelectedItems;

    // Ui properties
    private boolean mDisplayed = false;

    public Drawable getIcon() {
        return mIcon;
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        this.mPath = path;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        this.mDate = date;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        this.mSelected = selected;
    }

    public List<Picture> getItems() {
        return mItems;
    }

    public void setItems(List<Picture> items) {
        this.mItems = items;
    }

    public List<String> getSelectedItems() {
        return mSelectedItems;
    }

    public void setSelectedItems(List<String> selectedItems) {
        this.mSelectedItems = selectedItems;
    }

    public boolean isDisplayed() {
        return mDisplayed;
    }

    public void setDisplayed(boolean displayed) {
        this.mDisplayed = displayed;
    }

    @Override
    public int compareTo(@NonNull Album another) {
        return mPath.compareTo(another.mPath);
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Object clone() {
        Album album = new Album();
        album.mIcon = mIcon;
        album.mPath = mPath;
        album.mName = mName;
        album.mDate = mDate;
        album.mItems = new ArrayList<>(mItems);
        album.mSelectedItems = new ArrayList<>(mSelectedItems);
        album.mSelected = mSelected;
        album.mDisplayed = mDisplayed;
        return album;
    }
}
