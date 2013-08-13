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

package org.cyanogenmod.wallpapers.photophase;

import android.graphics.RectF;

import org.cyanogenmod.wallpapers.photophase.utils.GLESUtil.GLESTextureInfo;

/**
 * An interface that defines an object as able to request textures.
 */
public interface TextureRequestor {

    /**
     * Method that set the texture handle requested.
     *
     * @param ti The texture information
     */
    void setTextureHandle(GLESTextureInfo ti);

    /**
     * Method that returns the dimension of the requestor
     *
     * @return RectF The dimensions of the requestor
     */
    RectF getRequestorDimensions();
}
