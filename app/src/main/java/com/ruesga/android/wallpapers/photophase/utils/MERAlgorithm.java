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
import java.util.Stack;

/**
 * The maximal empty rectangle algorithm that allows to find the rectangle with the maximal
 * area that could be create in empty areas (in this case 0 in a byte matrix)
 */
//
// Based on the source discussed at http://discuss.leetcode.com/questions/260/maximal-rectangle
//
public final class MERAlgorithm {

    /**
     * Method that returns the maximal empty rectangle (MER) for a matrix of bytes (1/0)
     *
     * @param matrix The matrix
     * @return Rect The maximal empty rectangle
     */
    public static Rect getMaximalEmptyRectangle(byte[][] matrix) {
        // Check matrix
        int rows = matrix.length;
        if (rows == 0) return null;
        int cols = matrix[0].length;

        // Convert to histogram
        int[][] histogram = toHistogram(matrix);

        // Find the maximal area of every histogram
        Rect maxRect = new Rect();
        for (int i = 0; i < rows; ++i) {
            Rect rect = maximalRectangle(histogram[i], i);
            if ((maxRect.width() * maxRect.height()) < (rect.width() * rect.height())) {
                maxRect = rect;
            }
        }
        return ensureBounds(maxRect, cols, rows);
    }

    /**
     * Method that ensure the bounds of the max rectangle
     *
     * @param rect The rectangle to check
     * @param cols The number of cols
     * @param rows The number of rows
     * @return Rect The rectangle checked
     */
    private static Rect ensureBounds(Rect rect, int cols, int rows) {
        if (rect.right - rect.left >= cols) rect.right = cols;
        if (rect.bottom - rect.top >= rows) rect.bottom = rows;
        return rect;
    }

    /**
     * Method that returns the maximal rectangle for an histogram of areas
     *
     * @return Rect The maximal rectangle histogram/area
     */
    @SuppressWarnings("boxing")
    private static Rect maximalRectangle(int[] histogram, int row) {
        Stack<Integer> stack = new Stack<>();
        int length = histogram.length;
        Rect maxRect = new Rect();
        int i = 0;
        while (i < length) {
            if (stack.isEmpty() || histogram[i] >= histogram[stack.peek()]) {
                stack.push(i++);
            } else {
                Rect rect = new Rect();
                rect.left = stack.pop();
                rect.right = rect.left + (stack.isEmpty() ? i : (i - stack.peek() - 1));
                rect.top = row - histogram[rect.left] + 1;
                rect.bottom = rect.top + histogram[rect.left];
                if ((maxRect.width() * maxRect.height()) < (rect.width() * rect.height())) {
                    maxRect = rect;
                }
            }
        }
        while (!stack.isEmpty()) {
            Rect rect = new Rect();
            rect.left = stack.pop();
            rect.right = rect.left + (stack.isEmpty() ? i : (i - stack.peek() - 1));
            rect.top = row - histogram[rect.left] + 1;
            rect.bottom = rect.top + histogram[rect.left];
            if ((maxRect.width() * maxRect.height()) < (rect.width() * rect.height())) {
                maxRect = rect;
            }
        }
        return maxRect;
    }

    /**
     * Method that converts the empty areas to a histogram
     *
     * @param matrix The matrix where to find the MER
     * return int[][] The histogram of empty areas
     */
    private static int[][] toHistogram(byte[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[][] histogram = new int[rows][cols];
        for (int h=0; h < cols; h++) {
            if (matrix[0][h] == 0) {
                histogram[0][h] = 1;
            }
        }
        for (int w=1; w < rows; w++) {
            for (int h=0; h < cols; h++) {
                if (matrix[w][h] == 1) {
                    continue;
                }
                histogram[w][h] = histogram[w-1][h] + 1;
            }
        }
        return histogram;
    }
}
