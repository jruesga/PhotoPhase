/*
 * Copyright (C) 2015 Jorge Ruesga
 * Copyright (c) 2010, Sony Ericsson Mobile Communication AB. All rights reserved.
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

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.media.ExifInterface;

import com.ruesga.android.wallpapers.photophase.AndroidHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A helper class for deal with Bitmaps
 */
public class BitmapUtils {

    /**
     * ScalingLogic defines how scaling should be carried out if source and
     * destination image has different aspect ratio.
     *
     * CROP: Scales the image the minimum amount while making sure that at least
     * one of the two dimensions fit inside the requested destination area.
     * Parts of the source image will be cropped to realize this.
     *
     * FIT: Scales the image the minimum amount while making sure both
     * dimensions fit inside the requested destination area. The resulting
     * destination dimensions might be adjusted to a smaller size than
     * requested.
     */
    public enum ScalingLogic {
        CROP, FIT
    }

    /**
     * Method that decodes a bitmap
     *
     * @param bitmap The bitmap buffer to decode
     * @return Bitmap The decoded bitmap
     */
    @SuppressWarnings("deprecation")
    public static Bitmap decodeBitmap(InputStream bitmap) {
        final Options options = new Options();
        options.inScaled = false;
        options.inDither = true;
        options.inPreferQualityOverSpeed = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeStream(bitmap, null, options);
    }

    /**
     * Method that decodes a bitmap
     *
     * @param file The bitmap file to decode
     * @param dstWidth The request width
     * @param dstHeight The request height
     * @return Bitmap The decoded bitmap
     */
    @SuppressWarnings("deprecation")
    public static Bitmap decodeBitmap(File file, int dstWidth, int dstHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final Options options = new Options();
        options.inScaled = false;
        options.inDither = true;
        options.inPreferQualityOverSpeed = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // Deprecated, but still valid for KitKat and lower apis
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Decode the bitmap with inSampleSize set
        options.inSampleSize = Math.min(1, calculateBitmapRatio(options, dstWidth, dstHeight));
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap == null) {
            return null;
        }

        // Test if the bitmap has exif format, and decode properly
        Bitmap out = decodeExifBitmap(file, bitmap);
        if (out != null && !out.equals(bitmap)) {
            bitmap.recycle();
        }
        return out;
    }

    public static Rect getBitmapDimensions(File file) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (options.outWidth != -1 && options.outHeight != -1) {
            return new Rect(0, 0, options.outWidth, options.outHeight);
        }
        // Not an image
        return null;
    }

    /**
     * Utility function for decoding an image file. The decoded bitmap will
     * be optimized for further scaling to the requested destination dimensions
     * and scaling logic.
     *
     * @param file The file to load
     * @param dstWidth Width of destination area
     * @param dstHeight Height of destination area
     * @return Decoded bitmap
     */
    @SuppressWarnings("deprecation")
    public static Bitmap createUnscaledBitmap(File file, int dstWidth, int dstHeight,
            int minSampleSize) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inDither = true;
        options.inPreferQualityOverSpeed = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // Deprecated, but still valid for KitKat and lower apis
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Determine how much to scale down the image
        int photoWidth = options.outWidth;
        int photoHeight = options.outHeight;

        // Decode the image file into a Bitmap sized to fill the view
        options.inJustDecodeBounds = false;
        options.inSampleSize = Math.max(minSampleSize, Math.max(Math.round(photoWidth / dstWidth),
                Math.round(photoHeight / dstHeight)));
        return decodeExifBitmap(file, BitmapFactory.decodeFile(file.getAbsolutePath(), options));
    }

    /**
     * Utility function for creating a scaled version of an existing bitmap
     *
     * @param unscaledBitmap Bitmap to scale
     * @param dstWidth Wanted width of destination bitmap
     * @param dstHeight Wanted height of destination bitmap
     * @param scalingLogic Logic to use to avoid image stretching
     * @return New scaled bitmap object
     */
    public static Bitmap createScaledBitmap(Bitmap unscaledBitmap, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        if (unscaledBitmap.getWidth() == dstWidth && unscaledBitmap.getHeight() == dstHeight) {
            return unscaledBitmap;
        }
        Rect srcRect = calculateSrcRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Rect dstRect = calculateDstRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Bitmap scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));
        return scaledBitmap;
    }

    /**
     * Method that decodes an Exif bitmap
     *
     * @param file The file to decode
     * @param src The bitmap reference
     * @return Bitmap The decoded bitmap
     */
    private static Bitmap decodeExifBitmap(File file, Bitmap src) {
        if (src != null) {
            try {
                // Try to load the bitmap as a bitmap file
                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                if (orientation == ExifInterface.ORIENTATION_UNDEFINED
                        || orientation == ExifInterface.ORIENTATION_NORMAL) {
                    return src;
                }
                Matrix matrix = new Matrix();
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                    matrix.postRotate(90);
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                    matrix.postRotate(180);
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                    matrix.postRotate(270);
                } else if (orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL) {
                    matrix.setScale(-1, 1);
                    matrix.postTranslate(src.getWidth(), 0);
                } else if (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL) {
                    matrix.setScale(1, -1);
                    matrix.postTranslate(0, src.getHeight());
                }
                // Rotate the bitmap
                return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
            } catch (IOException e) {
                // Ignore
            }
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

    /**
     * Calculates source rectangle for scaling bitmap
     *
     * @param srcWidth Width of source image
     * @param srcHeight Height of source image
     * @param dstWidth Width of destination area
     * @param dstHeight Height of destination area
     * @param scalingLogic Logic to use to avoid image stretching
     * @return Optimal source rectangle
     */
    public static Rect calculateSrcRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.CROP) {
            final float srcAspect = (float)srcWidth / (float)srcHeight;
            final float dstAspect = (float)dstWidth / (float)dstHeight;

            if (srcAspect > dstAspect) {
                final int srcRectWidth = (int)(srcHeight * dstAspect);
                final int srcRectLeft = (srcWidth - srcRectWidth) / 2;
                return new Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight);
            } else {
                final int srcRectHeight = (int)(srcWidth / dstAspect);
                final int scrRectTop = (srcHeight - srcRectHeight) / 2;
                return new Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight);
            }
        } else {
            return new Rect(0, 0, srcWidth, srcHeight);
        }
    }

    /**
     * Calculates destination rectangle for scaling bitmap
     *
     * @param srcWidth Width of source image
     * @param srcHeight Height of source image
     * @param dstWidth Width of destination area
     * @param dstHeight Height of destination area
     * @param scalingLogic Logic to use to avoid image stretching
     * @return Optimal destination rectangle
     */
    public static Rect calculateDstRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.FIT) {
            final float srcAspect = (float)srcWidth / (float)srcHeight;
            final float dstAspect = (float)dstWidth / (float)dstHeight;

            if (srcAspect > dstAspect) {
                return new Rect(0, 0, dstWidth, (int)(dstWidth / srcAspect));
            } else {
                return new Rect(0, 0, (int)(dstHeight * srcAspect), dstHeight);
            }
        } else {
            return new Rect(0, 0, dstWidth, dstHeight);
        }
    }

    /**
     * Check if the bitmap is a power of two
     *
     * @param bitmap The bitmap to check
     * @return boolean if the size is power of two
     */
    public static boolean isPowerOfTwo(Bitmap bitmap){
        return isPowerOfTwo(bitmap.getWidth(), bitmap.getHeight());
    }

    /**
     * Check if the width and height are power of two
     *
     * @param w Width
     * @param h Height
     * @return boolean if the size is power of two
     */
    public static boolean isPowerOfTwo(int w, int h){
        return isPowerOfTwo(w) && isPowerOfTwo(h);
    }

    private static boolean isPowerOfTwo(int x) {
        while (((x % 2) == 0) && x > 1) {
            x /= 2;
        }
        return (x == 1);
    }

    /**
     * Return the nearest upper power of two size
     *
     * @param v The original value
     * @return The nearest upper power of two
     */
    public static int calculateUpperPowerOfTwo(int v) {
        v--;
        v |= v >>> 1;
        v |= v >>> 2;
        v |= v >>> 4;
        v |= v >>> 8;
        v |= v >>> 16;
        v++;
        return v;
    }

    public static void adjustRectToMinimumSize(Rect r, int size) {
        int w = r.width();
        int h = r.height();
        if (w > size || h > size) {
            if (w == h && w > size) {
                r.right = r.bottom = size;
            } else if (w < h && w > size) {
                r.right = w * size / h;
                r.bottom = size;
            } else {
                r.bottom = h * size / w;
                r.right = size;
            }
        }
    }

    public static int calculateMaxAvailableSize(Context context) {
        if (AndroidHelper.isJellyBeanOrGreater()) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            return (int)((mi.totalMem / 1073741824) * 1024);
        }
        // The minimum for all android devices
        return 1024;
    }

    public static int byteSizeOf(Bitmap bitmap) {
        if (AndroidHelper.isKitKatOrGreater()) {
            return bitmap.getAllocationByteCount();
        }
        return bitmap.getByteCount();
    }
}
