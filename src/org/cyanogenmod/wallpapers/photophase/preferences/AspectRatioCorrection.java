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

package org.cyanogenmod.wallpapers.photophase.preferences;

/**
 * An enumeration with all the touch actions supported
 */
public enum AspectRatioCorrection {
    /**
     * Do not apply aspect ratio correction
     */
    NONE(0),
    /**
     * Center the image and crop the image using the shortest size of the image
     */
    CROP(1),
    /**
     * Center using the largest size of the image
     */
    CENTER(2);

    private final int mValue;

    /**
     * Constructor of <code>AspectRatioCorrection</code>
     *
     * @param id The unique identifier
     */
    private AspectRatioCorrection(int value) {
        mValue = value;
    }

    /**
     * Method that returns the value
     *
     * @return int The value
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Method that gets the reference of a AspectRatioCorrection from its value
     *
     * @param value The value
     * @return AspectRatioCorrection The reference
     */
    public static final AspectRatioCorrection fromValue(int value) {
        if (value == CROP.mValue) return CROP;
        if (value == CENTER.mValue) return CENTER;
        return NONE;
    }
}
