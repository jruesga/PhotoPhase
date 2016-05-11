/*
 * Copyright (C) 2016 Jorge Ruesga
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

package com.ruesga.android.wallpapers.photophase.textures;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.effect.Effect;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.AndroidHelper;
import com.ruesga.android.wallpapers.photophase.borders.Border;
import com.ruesga.android.wallpapers.photophase.utils.BitmapUtils;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;

import java.io.IOException;
import java.io.InputStream;

public class SimpleTextureManager extends TextureManager {

    private static final String TAG = "SimpleTextureManager";

    private static final String ASSET_NAME1 = "sample1.jpg";
    private static final String ASSET_NAME2 = "sample2.jpg";

    private final Context mContext;
    private final Effect mEffect;
    private final Border mBorder;
    private final boolean mSingleTexture;

    private Rect mDimensions;

    private int last = 0;

    public SimpleTextureManager(Context context, Effect effect, Border border, boolean singleTexture) {
        // Pre-calculate the window size for the PhotoPhaseTextureManager. In onSurfaceChanged
        // the best fixed size will be set. The disposition size is simple for a better
        // performance of the internal arrays
        final Configuration conf = context.getResources().getConfiguration();
        int w = (int) AndroidHelper.convertDpToPixel(context, conf.screenWidthDp);
        int h = (int) AndroidHelper.convertDpToPixel(context, conf.screenHeightDp);
        mDimensions = new Rect(0, 0, w, h);
        mEffect = effect;
        mBorder = border;
        mContext = context;
        mSingleTexture = singleTexture;
    }

    public void setTargetDimensions(Rect dimensions) {
        mDimensions = dimensions;
    }

    @Override
    public void request(TextureRequestor requestor) {
        // Load the bitmap
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            is = mContext.getAssets().open(mSingleTexture || last % 2 == 0 ? ASSET_NAME1 : ASSET_NAME2);
            last++;
            bitmap = BitmapUtils.decodeBitmap(is);
        } catch (IOException ex) {
            Log.e(TAG, "Failed to load sample asset", ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }
        }

        if (bitmap != null) {
            requestor.setTextureHandle(
                    GLESUtil.loadTexture(mContext, bitmap, mEffect, mBorder, mDimensions));
        }
    }
}
