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

import java.util.ArrayList;
import java.util.List;

/**
 * An array of {@link Disposition} classes.
 */
public class Dispositions {

    public static final int TYPE_CURRENT = 0;
    public static final int TYPE_SAVED = 1;
    public static final int TYPE_SYSTEM = 2;

    private final List<Disposition> mDispositions;
    private final int mRows;
    private final int mCols;
    private final int mType;

    /**
     * Constructor of <code>Dispositions</code>
     *
     * @param type Type of dispositions
     * @param dispositions List of all dispositions
     * @param rows The number of rows of the dispositions
     * @param cols The number of columns of the dispositions
     */
    public Dispositions(int type, List<Disposition> dispositions, int rows, int cols) {
        super();
        mType = type;
        mDispositions = dispositions;
        mRows = rows;
        mCols = cols;
    }

    /**
     * Method that returns an array of all the dispositions
     *
     * @return List<Disposition> All the dispositions
     */
    public List<Disposition> getDispositions() {
        if (mDispositions == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(mDispositions);
    }

    /**
     * Method that returns the number of rows of the dispositions
     *
     * @return int The number of rows of the dispositions
     */
    public int getRows() {
        return mRows;
    }

    /**
     * Method that returns the number of columns of the dispositions
     *
     * @return int The number of columns of the dispositions
     */
    public int getCols() {
        return mCols;
    }

    public int getType() {
        return mType;
    }
}
