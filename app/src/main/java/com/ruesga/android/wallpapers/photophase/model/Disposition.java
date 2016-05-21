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

import android.support.annotation.NonNull;

import com.ruesga.android.wallpapers.photophase.PhotoFrame;

import java.util.UUID;

/**
 * A class that holds a {@link PhotoFrame} disposition.
 */
public class Disposition implements Comparable<Disposition> {

    public static final int BACKGROUND_FLAG = 0x01;
    public static final int TRANSITION_FLAG = 0x02;
    public static final int EFFECT_FLAG = 0x04;
    public static final int BORDER_FLAG = 0x08;

    public static final int ALL_FLAGS = BACKGROUND_FLAG | TRANSITION_FLAG
            | EFFECT_FLAG | BORDER_FLAG;

    /**
     * An internal uid
     */
    public String uid = UUID.randomUUID().toString();

    /**
     * Column
     */
    public int x;
    /**
     * Row
     */
    public int y;
    /**
     * Columns width
     */
    public int w;
    /**
     * Rows height
     */
    public int h;
    /**
     * Flags
     */
    public int flags = ALL_FLAGS;


    public boolean hasFlag(int f) {
        return (flags & f) == f;
    }

    public void addFlag(int f) {
        flags |= f;
    }

    public void removeFlag(int f) {
        flags = flags & ~f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + h;
        result = prime * result + w;
        result = prime * result + x;
        result = prime * result + y;
        result = prime * result + flags;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Disposition other = (Disposition) obj;
        if (h != other.h)
            return false;
        if (w != other.w)
            return false;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        return flags == other.flags;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Disposition [x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + ", flags=" + flags + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@NonNull Disposition another) {
        if (x == another.x && y == another.y && w == another.w
                && h == another.h && flags == another.flags) {
            return 0;
        }
        if (x < another.x) {
            return -1;
        }
        if (x > another.x) {
            return 1;
        }
        if (y < another.y) {
            return -1;
        }
        if (y > another.y) {
            return 1;
        }
        if (w < another.w) {
            return -1;
        }
        if (w > another.w) {
            return 1;
        }
        if (h < another.h) {
            return -1;
        }
        if (h > another.h) {
            return 1;
        }
        if (flags < another.flags) {
            return -1;
        }
        return 1;
    }
}
