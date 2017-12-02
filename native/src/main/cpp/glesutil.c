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

#include <jni.h>
#include <GLES2/gl2.h>

/**
 * Binds a IntBuffer image to OpenGL glTexSubImage2D
 */
JNIEXPORT void JNICALL
Java_com_ruesga_android_wallpapers_photophase_glesnative_GLESNative_nativeGlTexImage2D
        (JNIEnv *env, jclass clazz, jobject image, jint width, jint height) {
    jint *pixels = (*env)->GetDirectBufferAddress(env, image);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
}
