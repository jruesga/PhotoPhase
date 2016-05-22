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

package com.ruesga.android.wallpapers.photophase.textures;

import android.graphics.RectF;

import com.ruesga.android.wallpapers.photophase.model.Disposition;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLESTextureInfo;

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

    /**
     * Returns the mapped disposition or null if there is associated disposition
     *
     * @return the associated disposition
     */
    Disposition getDisposition();
}
