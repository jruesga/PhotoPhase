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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.effect.Effect;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLUtils;
import android.util.Log;

import com.ruesga.android.wallpapers.photophase.AndroidHelper;
import com.ruesga.android.wallpapers.photophase.BuildConfig;
import com.ruesga.android.wallpapers.photophase.borders.Border;
import com.ruesga.android.wallpapers.photophase.glesnative.GLESNative;
import com.ruesga.android.wallpapers.photophase.preferences.PreferencesProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;


/**
 * A helper class with some useful methods for deal with GLES.
 */
public final class GLESUtil {

    private static final String TAG = "GLESUtil";

    private static final boolean DEBUG = false;

    public static final boolean DEBUG_GL_MEMOBJS = false;
    public static final String DEBUG_GL_MEMOBJS_NEW_TAG = "MEMOBJS_NEW";
    public static final String DEBUG_GL_MEMOBJS_DEL_TAG = "MEMOBJS_DEL";

    private static final Object SYNC = new Object();

    private static final int MAX_GLES_ERRORS = 50;
    private static int sGlErrors = 0;

    /**
     * A helper class to deal with OpenGL float colors.
     */
    public static class GLColor {

        private static final float MAX_COLOR = 255.0f;

        /**
         * Red
         */
        public final float r;
        /**
         * Green
         */
        public final float g;
        /**
         * Blue
         */
        public final float b;
        /**
         * Alpha
         */
        public float a;

        /**
         * Constructor of <code>GLColor</code> from ARGB
         *
         * @param a Alpha
         * @param r Red
         * @param g Green
         * @param b Alpha
         */
        public GLColor(int a, int r, int g, int b) {
            this.a = a / MAX_COLOR;
            this.r = r / MAX_COLOR;
            this.g = g / MAX_COLOR;
            this.b = b / MAX_COLOR;
        }

        /**
         * Constructor of <code>GLColor</code> from ARGB.
         *
         * @param argb An #AARRGGBB string
         */
        public GLColor(String argb) {
            int color = Color.parseColor(argb);
            this.a = Color.alpha(color) / MAX_COLOR;
            this.r = Color.red(color) / MAX_COLOR;
            this.g = Color.green(color) / MAX_COLOR;
            this.b = Color.blue(color) / MAX_COLOR;
        }

        public GLColor(GLColor color) {
            this.a = color.a;
            this.r = color.r;
            this.g = color.g;
            this.b = color.b;
        }

        /**
         * Constructor of <code>GLColor</code> from ARGB.
         *
         * @param argb An #AARRGGBB number
         */
        public GLColor(int argb) {
            this.a = Color.alpha(argb) / MAX_COLOR;
            this.r = Color.red(argb) / MAX_COLOR;
            this.g = Color.green(argb) / MAX_COLOR;
            this.b = Color.blue(argb) / MAX_COLOR;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Float.floatToIntBits(a);
            result = prime * result + Float.floatToIntBits(b);
            result = prime * result + Float.floatToIntBits(g);
            result = prime * result + Float.floatToIntBits(r);
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("SimplifiableIfStatement")
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            GLColor other = (GLColor) obj;
            if (Float.floatToIntBits(a) != Float.floatToIntBits(other.a))
                return false;
            if (Float.floatToIntBits(b) != Float.floatToIntBits(other.b))
                return false;
            if (Float.floatToIntBits(g) != Float.floatToIntBits(other.g))
                return false;
            return Float.floatToIntBits(r) == Float.floatToIntBits(other.r);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "#"+Integer.toHexString(Color.argb(
                    (int)a * 255, (int)r * 255, (int)g * 255, (int)b * 255));
        }

        public float[] asVec4() {
            return new float[]{r, g, b, a};
        }
    }

    /**
     * Class that holds some information about a GLES texture
     */
    public static class GLESTextureInfo {
        /**
         * Handle of the texture
         */
        public int handle = 0;
        /**
         * The bitmap reference
         */
        public Bitmap bitmap;
        /**
         * The path to the texture
         */
        public File path;
        /**
         * The effect to apply
         */
        public Effect effect;
        /**
         * The border to apply
         */
        public Border border;
    }

    /**
     * Method that load a vertex shader and returns its handler identifier.
     *
     * @param src The source shader
     * @return int The handler identifier of the shader
     */
    private static int loadVertexShader(String src) {
        return loadShader(src, GLES20.GL_VERTEX_SHADER);
    }

    /**
     * Method that load a fragment shader and returns its handler identifier.
     *
     * @param src The source shader
     * @return int The handler identifier of the shader
     */
    private static int loadFragmentShader(String src) {
        return loadShader(src, GLES20.GL_FRAGMENT_SHADER);
    }

    /**
     * Method that load a shader and returns its handler identifier.
     *
     * @param src The source shader
     * @param type The type of shader
     * @return int The handler identifier of the shader
     */
    private static int loadShader(String src, int type) {
        int[] compiled = new int[1];
        // Create, load and compile the shader
        int shader = GLES20.glCreateShader(type);
        if (GLESUtil.DEBUG_GL_MEMOBJS) {
            Log.d(GLESUtil.DEBUG_GL_MEMOBJS_NEW_TAG, "glCreateShader (" + type + "): " + shader);
        }
        GLESUtil.glesCheckError("glCreateShader");
        if (shader <= 0) {
            Log.e(TAG, "Cannot create a shader");
            return 0;
        }
        GLES20.glShaderSource(shader, src);
        GLESUtil.glesCheckError("glShaderSource");
        GLES20.glCompileShader(shader);
        GLESUtil.glesCheckError("glesCheckError");
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        GLESUtil.glesCheckError("glesCheckError");
        if (compiled[0] <= 0) {
            String msg = "Shader compilation error trace:\n" + GLES20.glGetShaderInfoLog(shader);
            Log.e(TAG, msg);
            return 0;
        }
        return shader;
    }

    /**
     * Method that create a new program from its shaders (vertex and fragment)
     *
     * @param res A resources reference
     * @param vertexShaderId The vertex shader glsl resource
     * @param fragmentShaderId  The fragment shader glsl resource
     * @return int The handler identifier of the program
     */
    public static int createProgram(Resources res, int vertexShaderId, int fragmentShaderId) {
        return createProgram(
                readResource(res, vertexShaderId),
                readResource(res, fragmentShaderId));
    }

    /**
     * Method that create a new program from its shaders (vertex and fragment)
     *
     * @param vertexShaderSrc The vertex shader
     * @param fragmentShaderSrc  The fragment shader
     * @return int The handler identifier of the program.
     */
    public static int createProgram(String vertexShaderSrc, String fragmentShaderSrc) {
        int vshader = 0;
        int fshader = 0;
        int progid;
        int[] link = new int[1];

        try {
            // Check that we have valid shaders
            if (vertexShaderSrc == null || fragmentShaderSrc == null) {
                return 0;
            }

            // Load the vertex and fragment shaders
            vshader = loadVertexShader(vertexShaderSrc);
            fshader = loadFragmentShader(fragmentShaderSrc);

            // Create the programa ref
            progid = GLES20.glCreateProgram();
            if (GLESUtil.DEBUG_GL_MEMOBJS) {
                Log.d(GLESUtil.DEBUG_GL_MEMOBJS_NEW_TAG, "glCreateProgram: " + progid);
            }
            GLESUtil.glesCheckError("glCreateProgram");
            if (progid <= 0) {
                String msg = "Cannot create a program";
                Log.e(TAG, msg);
                return 0;
            }

            // Attach the shaders
            GLES20.glAttachShader(progid, vshader);
            GLESUtil.glesCheckError("glAttachShader");
            GLES20.glAttachShader(progid, fshader);
            GLESUtil.glesCheckError("glAttachShader");

            // Link the program
            GLES20.glLinkProgram(progid);
            GLESUtil.glesCheckError("glLinkProgram");

            GLES20.glGetProgramiv(progid, GLES20.GL_LINK_STATUS, link, 0);
            GLESUtil.glesCheckError("glGetProgramiv");
            if (link[0] <= 0) {
                String msg = "Program compilation error trace:\n" + GLES20.glGetProgramInfoLog(progid);
                Log.e(TAG, msg);

                // If something is wrong repeatedly, then restart the wallpaper
                sGlErrors++;
                if (sGlErrors > MAX_GLES_ERRORS) {
                    AndroidHelper.restartWallpaper();
                }
                return 0;
            }
            sGlErrors = 0;

            // Return the program
            return progid;

        } finally {
            // Delete the shaders
            if (vshader != 0) {
                if (GLESUtil.DEBUG_GL_MEMOBJS) {
                    Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteShader (v): " + vshader);
                }
                GLES20.glDeleteShader(vshader);
                GLESUtil.glesCheckError("glDeleteShader");
            }
            if (fshader != 0) {
                if (GLESUtil.DEBUG_GL_MEMOBJS) {
                    Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteShader (f): " + fshader);
                }
                GLES20.glDeleteShader(fshader);
                GLESUtil.glesCheckError("glDeleteShader");
            }
        }
    }

    /**
     * Method that loads a fake texture (the bitmap but no gles data) from a file.
     *
     * @param file The image file
     * @param dimensions The desired dimensions
     * @return GLESTextureInfo The texture info
     */
    public static GLESTextureInfo loadFakeTexture(File file, Rect dimensions) {
        Bitmap bitmap = null;
        try {
            // Decode and associate the bitmap (invert the desired dimensions)
            bitmap = BitmapUtils.decodeBitmap(file, dimensions.width(), dimensions.height());
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode the file bitmap");
                return new GLESTextureInfo();
            }

            if (DEBUG) Log.d(TAG, "image: " + file.getAbsolutePath());
            GLESTextureInfo ti = new GLESTextureInfo();
            ti.bitmap = bitmap;
            ti.path = file;
            return ti;

        } catch (Exception e) {
            if (DEBUG)  {
                String msg = "Failed to generate a valid texture from file: " +
                        file.getAbsolutePath();
                Log.e(TAG, msg, e);
            }
            if (bitmap != null) {
                bitmap.recycle();
            }
            return new GLESTextureInfo();

        }
    }

    /**
     * Method that loads a texture from a resource context.
     *
     * @param ctx The current context
     * @param resourceId The resource identifier
     * @param effect The effect to apply to the image or null if no effect is needed
     * @param border The border to apply to the image or null if no border was defined
     * @param dimen The new dimensions
     * @param recycle If the bitmap should be recycled
     * @return GLESTextureInfo The texture info
     */
    public static GLESTextureInfo loadTexture(Context ctx, int resourceId, Effect effect,
            Border border, Rect dimen, boolean recycle) {
        Bitmap bitmap = null;
        InputStream raw = null;
        try {
            // Decode and associate the bitmap
            raw = ctx.getResources().openRawResource(resourceId);
            bitmap = BitmapUtils.decodeBitmap(raw);
            if (bitmap == null) {
                String msg = "Failed to decode the resource bitmap";
                Log.e(TAG, msg);
                return new GLESTextureInfo();
            }

            if (DEBUG) Log.d(TAG, "resourceId: " + resourceId);
            return loadTexture(ctx, bitmap, effect, border, dimen);

        } catch (Exception e) {
            String msg = "Failed to generate a valid texture from resource: " + resourceId;
            Log.e(TAG, msg, e);
            return new GLESTextureInfo();

        } finally {
            // Close the buffer
            try {
                if (raw != null) {
                    raw.close();
                }
            } catch (IOException e) {
                // Ignore.
            }
            // Recycle the bitmap
            if (bitmap != null && recycle) {
                bitmap.recycle();
            }
        }
    }

    /**
     * Method that loads texture from a bitmap reference.
     *
     * @param bitmap The bitmap reference
     * @param effect The effect to apply to the image or null if no effect is needed
     * @param border The border to apply to the image or null if no border was defined
     * @param dimen The new dimensions
     * @return GLESTextureInfo The texture info
     */
    public static synchronized GLESTextureInfo loadTexture(Context context, Bitmap bitmap,
            Effect effect, Border border, Rect dimen) {
        // Check that we have a valid image name reference
        if (bitmap == null) {
            return new GLESTextureInfo();
        }

        Bitmap texture = ensurePowerOfTwoTexture(context, bitmap);

        int num = 1;
        if (effect != null) {
            num++;
        }
        if (border != null) {
            num++;
        }

        int[] textureHandles = new int[num];
        GLES20.glGenTextures(num, textureHandles, 0);
        GLESUtil.glesCheckError("glGenTextures");
        for (int i = 0; i < num; i++) {
            if (GLESUtil.DEBUG_GL_MEMOBJS) {
                Log.d(GLESUtil.DEBUG_GL_MEMOBJS_NEW_TAG, "glGenTextures: " + textureHandles[i]);
            }
        }
        if (textureHandles[0] <= 0 || (effect != null && textureHandles[1] <= 0)) {
            Log.e(TAG, "Failed to generate a valid texture");
            return new GLESTextureInfo();
        }

        // Bind the texture to the name
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[0]);
        GLESUtil.glesCheckError("glBindTexture");

        // Set the texture properties
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLESUtil.glesCheckError("glTexParameteri");
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLESUtil.glesCheckError("glTexParameteri");
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLESUtil.glesCheckError("glTexParameteri");
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLESUtil.glesCheckError("glTexParameteri");

        // Load the texture
        if (GLESNative.isUseNativeTextureBind()) {
            GLESNative.glTexImage2D(texture);
        } else {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0);
        }

        if (!GLES20.glIsTexture(textureHandles[0])) {
            Log.e(TAG, "Failed to load a valid texture");
            return new GLESTextureInfo();
        }

        // Apply effects and borders. Don't apply effects if there is not a valid context
        int handle = textureHandles[0];
        if (hasValidEglContext()) {
            int n = 0;
            if (effect != null) {
                handle = applyEffect(textureHandles, n, effect, dimen);
                n++;
            }
            if (border != null) {
                handle = applyEffect(textureHandles, n, border, dimen);
            }
        }

        // Return the texture handle identifier and the associated info
        GLESTextureInfo ti = new GLESTextureInfo();
        ti.handle = handle;
        ti.bitmap = texture;
        ti.path = null;
        return ti;
    }

    private static int applyEffect(int[] textureHandles, int n, Effect effect, Rect dimen) {
        // Apply the border (we need a thread-safe call here)
        synchronized (SYNC) {
            // No more than 1024 (the minimum supported by all the gles20 devices)
            effect.apply(textureHandles[n], dimen.width(), dimen.height(), textureHandles[n + 1]);
        }

        // Delete the unused texture
        if (GLESUtil.DEBUG_GL_MEMOBJS) {
            Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteTextures: ["
                    + textureHandles[n] + "]");
        }
        GLES20.glDeleteTextures(1, textureHandles, n);
        GLESUtil.glesCheckError("glDeleteTextures");
        return textureHandles[n + 1];
    }

    /**
     * Ensure that the passed bitmap can be used a as power of two texture
     *
     * @param src The source bitmap
     * @return A bitmap which is power of two
     */
    private static Bitmap ensurePowerOfTwoTexture(Context context, Bitmap src) {
        if (!BitmapUtils.isPowerOfTwo(src) &&
                PreferencesProvider.Preferences.General.isPowerOfTwo(context)) {
            int powerOfTwo = BitmapUtils.calculateUpperPowerOfTwo(
                    Math.min(src.getWidth(), src.getHeight()));

            // Create a power of two bitmap
            Bitmap out = Bitmap.createScaledBitmap(src, powerOfTwo, powerOfTwo, false);
            src.recycle();
            return out;
        }
        return src;
    }

    /**
     * Method that checks if an GLES error is present
     *
     * @param func The GLES function to check
     */
    public static void glesCheckError(String func) {
        // Log when a call happens without a current context or outside the GLThread
        if (BuildConfig.DEBUG) {
            if (!hasValidEglContext()) {
                try {
                    throw new GLException(-1, "call to OpenGL ES API with no current context");
                } catch (GLException ex) {
                    Log.w(TAG, "GLES20 Error (" + glesGetErrorModule() + ") (" + func + "): call to " +
                            "OpenGL ES API with no current context", ex);
                }
            } else if (!Thread.currentThread().getName().startsWith("GLThread")) {
                try {
                    throw new GLException(-1, "call to OpenGL ES API outside GLThread");
                } catch (GLException ex) {
                    Log.w(TAG, "GLES20 Error (" + glesGetErrorModule() + ") (" + func + "): call to " +
                            "OpenGL ES API outside GLThread", ex);
                }
            }
        }

        int error = GLES20.glGetError();
        if (error != 0) {
            if (BuildConfig.DEBUG) {
                try {
                    throw new GLException(error);
                } catch (GLException ex) {
                    Log.e(TAG, "GLES20 Error (" + glesGetErrorModule() + ") (" + func + "): " +
                            GLUtils.getEGLErrorString(error), ex);
                }
            } else {
                Log.e(TAG, "GLES20 Error (" + glesGetErrorModule() + ") (" + func + "): " +
                        GLUtils.getEGLErrorString(error));
            }
        }
    }

    /**
     * Method that returns the line and module that generates the current error
     *
     * @return String The line and module
     */
    private static String glesGetErrorModule() {
        try {
            return String.valueOf(Thread.currentThread().getStackTrace()[4]);
        } catch (IndexOutOfBoundsException ioobEx) {
            // Ignore
        }
        return "";
    }

    /**
     * Return whether a valid Egl context exists
     *
     * @return boolean If a valid Egl context exists
     */
    private static boolean hasValidEglContext() {
        final EGL10 egl = (EGL10) EGLContext.getEGL();
        return egl != null &&
                egl.eglGetCurrentContext() != null &&
                !egl.eglGetCurrentContext().equals(EGL10.EGL_NO_CONTEXT);
    }

    /**
     * Method that read a resource.
     *
     * @param res The resources reference
     * @param resId The resource identifier
     * @return String The shader source
     */
    private static String readResource(Resources res, int resId) {
        Reader reader = new InputStreamReader(res.openRawResource(resId));
        try {
            final int BUFFER = 1024;
            char[] data = new char[BUFFER];
            int read;
            StringBuilder sb = new StringBuilder();
            while ((read = reader.read(data, 0, BUFFER)) != -1) {
                sb.append(data, 0, read);
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read the resource " + resId);
            return null;
        } finally {
            try {
                reader.close();
            } catch (Exception ex) {
                // Ignore
            }
        }
    }
}
