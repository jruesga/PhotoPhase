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

package org.cyanogenmod.wallpapers.photophase.effects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

/**
 * This effect converts the source image to sepia color scheme.
 */
public class SepiaEffect extends Effect {

    /**
     * {@inheritDoc}
     */
    @Override
    public Bitmap apply(Bitmap src) {
        try {
            final int height = src.getHeight();
            final int width = src.getWidth();

            Bitmap dst = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            final Canvas c = new Canvas(dst);
            final Paint paint = new Paint();
            final ColorMatrix cmA = new ColorMatrix();
            cmA.setSaturation(0);
            final ColorMatrix cmB = new ColorMatrix();
            cmB.setScale(1f, .95f, .82f, 1.0f);
            cmA.setConcat(cmB, cmA);
            final ColorMatrixColorFilter f = new ColorMatrixColorFilter(cmA);
            paint.setColorFilter(f);
            c.drawBitmap(src, 0, 0, paint);
            return dst;
        } finally {
            src.recycle();
        }
    }

}
