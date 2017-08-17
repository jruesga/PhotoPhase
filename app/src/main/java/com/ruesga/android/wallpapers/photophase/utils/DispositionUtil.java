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

package com.ruesga.android.wallpapers.photophase.utils;

import android.graphics.Rect;

import com.ruesga.android.wallpapers.photophase.model.Disposition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A helper class with disposition utils
 */
public final class DispositionUtil {

    /**
     * Method that converts a disposition string to a disposition reference
     *
     * @param value The value to convert
     * @return List<Disposition> The  dispositions reference
     */
    public static List<Disposition> toDispositions(String value) {
        String[] v = value.split("\\|");
        List<Disposition> dispositions = new ArrayList<>(v.length);
        for (String s : v) {
            int flags = Disposition.ALL_FLAGS;
            if (s.contains("~")) {
                String[] s0 = s.split("~");
                flags = Integer.parseInt(s0[1]);
                s = s0[0];
            }
            String[] s1 = s.split(":");
            String[] s2 = s1[0].split("x");
            String[] s3 = s1[1].split("x");
            Disposition disposition = new Disposition();
            disposition.x = Integer.parseInt(s2[0]);
            disposition.y = Integer.parseInt(s2[1]);
            disposition.w = Integer.parseInt(s3[0]) - disposition.x + 1;
            disposition.h = Integer.parseInt(s3[1]) - disposition.y + 1;
            disposition.flags = flags;
            dispositions.add(disposition);
        }
        Collections.sort(dispositions);
        return dispositions;
    }

    /**
     * Method that converts a disposition reference to a disposition string
     *
     * @param dispositions The value to convert
     * @return String The dispositions string
     */
    public static String fromDispositions(List<Disposition> dispositions) {
        Collections.sort(dispositions);
        StringBuilder sb = new StringBuilder();
        int count = dispositions.size();
        for (int i = 0; i < count; i++) {
            Disposition disposition = dispositions.get(i);
            sb.append(disposition.x)
                .append("x")
                .append(disposition.y)
                .append(":")
                .append(disposition.x + disposition.w - 1)
                .append("x")
                .append(disposition.y + disposition.h - 1)
                .append("~")
                .append(disposition.flags);
            if (i < (count - 1)) {
                sb.append("|");
            }
        }
        return sb.toString();
    }

    /**
     * Method that transform the disposition to a byte matrix
     *
     * @param dispositions The
     * @return byte[][] The boolean matrix of the disposition
     */
    public static byte[][] toMatrix(List<Disposition> dispositions, int cols, int rows) {
        byte[][] matrix = new byte[rows][cols];
        for (Disposition disposition : dispositions) {
            int count = disposition.y + disposition.h;
            for (int row = disposition.y; row < count; row++) {
                int count2 = disposition.x + disposition.w;
                for (int col = disposition.x; col < count2; col++) {
                    if (row < rows && col < cols) {
                        matrix[row][col] = 1;
                    }
                }
            }
        }
        return matrix;
    }

    /**
     * Method that returns a disposition from a {@link Rect} reference
     *
     * @return Disposition The disposition
     */
    public static Disposition fromRect(Rect r) {
        Disposition disposition = new Disposition();
        disposition.x = r.left;
        disposition.y = r.top;
        disposition.w = r.width();
        disposition.h = r.height();
        return disposition;
    }
}
