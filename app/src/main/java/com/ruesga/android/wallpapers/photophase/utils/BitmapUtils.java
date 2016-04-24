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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A helper class for deal with Bitmaps
 */
public class BitmapUtils {

    /**
     * Method that decodes a bitmap
     *
     * @param bitmap The bitmap buffer to decode
     * @return Bitmap The decoded bitmap
     */
    public static Bitmap decodeBitmap(InputStream bitmap) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferQualityOverSpeed = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = false;
        return BitmapFactory.decodeStream(bitmap, null, options);
    }

    /**
     * Method that decodes a bitmap
     *
     * @param file The bitmap file to decode
     * @param reqWidth The request width
     * @param reqHeight The request height
     * @return Bitmap The decoded bitmap
     */
    @SuppressWarnings("deprecation")
    public static Bitmap decodeBitmap(File file, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Calculate inSampleSize (use 1024 as maximum size, the minimum supported
        // by all the gles20 devices)
        options.inSampleSize = calculateBitmapRatio(
                                    options,
                                    Math.min(reqWidth, 1024),
                                    Math.min(reqHeight, 1024));

        // Decode the bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        options.inPreferQualityOverSpeed = false;
        // Deprecated, but still valid for KitKat and lower apis
        options.inPurgeable = true;
        options.inInputShareable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap == null) {
            return null;
        }

        // Test if the bitmap has exif format, and decode properly
        Bitmap out = decodeExifBitmap(file, bitmap);
        if (!out.equals(bitmap)) {
            bitmap.recycle();
        }
        return bitmap;
    }

    /**
     * Method that decodes an Exif bitmap
     *
     * @param file The file to decode
     * @param src The bitmap reference
     * @return Bitmap The decoded bitmap
     */
    private static Bitmap decodeExifBitmap(File file, Bitmap src) {
        try {
            // Try to load the bitmap as a bitmap file
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            if (orientation == 0) {
                return src;
            }
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
            } else if (orientation == 3) {
                matrix.postRotate(180);
            } else if (orientation == 8) {
                matrix.postRotate(270);
            }
            // Rotate the bitmap
            return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        } catch (IOException e) {
            // Ignore
        }
        return src;
    }

    /**
     * Method that calculate the bitmap size prior to decode
     *
     * @param options The bitmap factory options
     * @param reqWidth The request width
     * @param reqHeight The request height
     * @return int The picture ratio
     */
    private static int calculateBitmapRatio(Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
