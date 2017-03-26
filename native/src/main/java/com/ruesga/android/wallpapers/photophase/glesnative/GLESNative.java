/*
 * Copyright (C) 2017 Jorge Ruesga
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

package com.ruesga.android.wallpapers.photophase.glesnative;

import android.graphics.Bitmap;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class GLESNative {

    private static final String TAG = "GLESNative";

    private static boolean NATIVE_TEXTURE_BIND = true;

    private static IntBuffer sNativeBuffer;

    static {
        if (NATIVE_TEXTURE_BIND) {
            try {
                System.loadLibrary("photophase");
                sNativeBuffer = null;
            } catch (UnsatisfiedLinkError ex) {
                NATIVE_TEXTURE_BIND = false;
                Log.w(TAG, "Can't load native library. Fallback to android texImage2D version", ex);
            }
        }
    }

    public static boolean isUseNativeTextureBind() {
        return NATIVE_TEXTURE_BIND;
    }

    public static void glTexImage2D(Bitmap texture) {
        // Create a buffer from the image
        int width = texture.getWidth();
        int height = texture.getHeight();
        final int size = height * texture.getRowBytes();
        if (sNativeBuffer == null || sNativeBuffer.capacity() < size) {
            sNativeBuffer = ByteBuffer.allocateDirect(size * 4).asIntBuffer();
        } else {
            sNativeBuffer.clear();
        }
        texture.copyPixelsToBuffer(sNativeBuffer);
        nativeGlTexImage2D(sNativeBuffer, width, height);
    }

    private static native void nativeGlTexImage2D(IntBuffer image, int width, int height);
}
