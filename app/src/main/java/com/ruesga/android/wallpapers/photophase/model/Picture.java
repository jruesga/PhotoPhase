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

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.io.File;

/**
 * A class that represents a picture
 */
public class Picture implements Comparable<Picture>, Cloneable {

    private String mPath;
    private boolean mSelected;
    private Bitmap mBitmap;

    public Picture(String path, boolean selected) {
        super();
        this.mPath = path;
        this.mSelected = selected;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        this.mPath = path;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        this.mSelected = selected;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public String getName() {
        return new File(mPath).getName();
    }

    @Override
    public int compareTo(@NonNull Picture another) {
        return mPath.compareTo(another.mPath);
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Object clone() {
        Picture pic = new Picture(mPath, mSelected);
        pic.mBitmap = mBitmap;
        return pic;
    }
}
