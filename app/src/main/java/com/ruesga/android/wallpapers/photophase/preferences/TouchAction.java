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

/**
 * An enumeration with all the touch actions supported
 */
public enum TouchAction {
    /**
     * No action
     */
    NONE(0),
    /**
     * Force transition of the frame
     */
    TRANSITION(1),
    /**
     * Open the picture of the frame
     */
    OPEN(2),
    /**
     * Share/send the picture of the frame
     */
    SHARE(3),
    /**
     * Cast the picture to a remote chromacast device
     */
    CAST(4);

    private final int mValue;

    /**
     * Constructor of <code>TouchAction</code>
     *
     * @param value The internal value
     */
    TouchAction(int value) {
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
     * Method that gets the reference of a TouchAction from its value
     *
     * @param value The value
     * @return TouchAction The reference
     */
    public static TouchAction fromValue(int value) {
        if (value == TRANSITION.mValue) return TRANSITION;
        if (value == OPEN.mValue) return OPEN;
        if (value == SHARE.mValue) return SHARE;
        if (value == CAST.mValue) return CAST;
        return NONE;
    }
}
