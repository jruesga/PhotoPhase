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

package org.cyanogenmod.wallpapers.photophase.model;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents an album
 */
public class Album  implements Comparable<Album>, Cloneable {

    private Drawable mIcon;
    private String mPath;
    private String mName;
    private String mDate;
    private boolean mSelected;
    private List<String> mItems;
    private List<String> mSelectedItems;

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

    public List<String> getItems() {
        return mItems;
    }

    public void setItems(List<String> items) {
        this.mItems = items;
    }

    public List<String> getSelectedItems() {
        return mSelectedItems;
    }

    public void setSelectedItems(List<String> selectedItems) {
        this.mSelectedItems = selectedItems;
    }

    @Override
    public int compareTo(Album another) {
        return mPath.compareTo(another.mPath);
    }

    @Override
    public Object clone() {
        Album album = new Album();
        album.mIcon = mIcon;
        album.mPath = mPath;
        album.mName = mName;
        album.mDate = mDate;
        album.mItems = new ArrayList<String>(mItems);
        album.mSelectedItems = new ArrayList<String>(mSelectedItems);
        album.mSelected = mSelected;
        return album;
    }
}
