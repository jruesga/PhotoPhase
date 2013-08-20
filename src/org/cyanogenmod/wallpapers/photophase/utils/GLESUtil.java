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

package org.cyanogenmod.wallpapers.photophase.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.effect.Effect;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;


/**
 * A helper class with some useful methods for deal with GLES.
 */
public final class GLESUtil {

    private static final String TAG = "GLESUtil";

    private static final boolean DEBUG = false;

    private static final Object sSync = new Object();

    /**
     * A helper class to deal with OpenGL float colors.
     */
    public static class GLColor {

        private static final float MAX_COLOR = 255.0f;

        /**
         * Red
         */
        public float r;
        /**
         * Green
         */
        public float g;
        /**
         * Blue
         */
        public float b;
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
            if (Float.floatToIntBits(r) != Float.floatToIntBits(other.r))
                return false;
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "#"+Integer.toHexString(Color.argb((int)a, (int)r, (int)g, (int)b));
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
    }

    /**
     * Method that load a vertex shader and returns its handler identifier.
     *
     * @param src The source shader
     * @return int The handler identifier of the shader
     */
    public static int loadVertexShader(String src) {
        return loadShader(src, GLES20.GL_VERTEX_SHADER);
    }

    /**
     * Method that load a fragment shader and returns its handler identifier.
     *
     * @param src The source shader
     * @return int The handler identifier of the shader
     */
    public static int loadFragmentShader(String src) {
        return loadShader(src, GLES20.GL_FRAGMENT_SHADER);
    }

    /**
     * Method that load a shader and returns its handler identifier.
     *
     * @param src The source shader
     * @param type The type of shader
     * @return int The handler identifier of the shader
     */
    public static int loadShader(String src, int type) {
        int[] compiled = new int[1];
        // Create, load and compile the shader
        int shader = GLES20.glCreateShader(type);
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
        int progid = 0;
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
                return 0;
            }

            // Return the program
            return progid;

        } finally {
            // Delete the shaders
            if (vshader != 0) {
                GLES20.glDeleteShader(vshader);
                GLESUtil.glesCheckError("glDeleteShader");
            }
            if (fshader != 0) {
                GLES20.glDeleteShader(fshader);
                GLESUtil.glesCheckError("glDeleteShader");
            }
        }
    }

    /**
     * Method that loads a texture from a file.
     *
     * @param file The image file
     * @param dimensions The desired dimensions
     * @param effect The effect to apply to the image or null if no effect is needed
     * @param dimen The new dimensions
     * @param recycle If the bitmap should be recycled
     * @return GLESTextureInfo The texture info
     */
    public static GLESTextureInfo loadTexture(
            File file, Rect dimensions, Effect effect, Rect dimen, boolean recycle) {
        Bitmap bitmap = null;
        try {
            // Decode and associate the bitmap (invert the desired dimensions)
            bitmap = BitmapUtils.decodeBitmap(file, dimensions.height(), dimensions.width());
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode the file bitmap");
                return new GLESTextureInfo();
            }

            if (DEBUG) Log.d(TAG, "image: " + file.getAbsolutePath());
            GLESTextureInfo ti = loadTexture(bitmap, effect, dimen);
            ti.path = file;
            return ti;

        } catch (Exception e) {
            String msg = "Failed to generate a valid texture from file: " + file.getAbsolutePath();
            Log.e(TAG, msg, e);
            return new GLESTextureInfo();

        } finally {
            // Recycle the bitmap
            if (bitmap != null && recycle) {
                bitmap.recycle();
                bitmap = null;
            }
        }
    }

    /**
     * Method that loads a texture from a resource context.
     *
     * @param ctx The current context
     * @param resourceId The resource identifier
     * @param effect The effect to apply to the image or null if no effect is needed
     * @param dimen The new dimensions
     * @param recycle If the bitmap should be recycled
     * @return GLESTextureInfo The texture info
     */
    public static GLESTextureInfo loadTexture(
            Context ctx, int resourceId, Effect effect, Rect dimen, boolean recycle) {
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
            GLESTextureInfo ti = loadTexture(bitmap, effect, dimen);
            return ti;

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
                bitmap = null;
            }
        }
    }

    /**
     * Method that loads texture from a bitmap reference.
     *
     * @param bitmap The bitmap reference
     * @param effect The effect to apply to the image or null if no effect is needed
     * @param dimen The new dimensions
     * @return GLESTextureInfo The texture info
     */
    public static GLESTextureInfo loadTexture(Bitmap bitmap, Effect effect, Rect dimen) {
        // Check that we have a valid image name reference
        if (bitmap == null) {
            return new GLESTextureInfo();
        }

        int num = effect == null ? 1 : 2;

        int[] textureHandles = new int[num];
        GLES20.glGenTextures(num, textureHandles, 0);
        GLESUtil.glesCheckError("glGenTextures");
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
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        if (!GLES20.glIsTexture(textureHandles[0])) {
            Log.e(TAG, "Failed to load a valid texture");
            return new GLESTextureInfo();
        }

        // Has a effect?
        int handle = textureHandles[0];
        if (effect != null) {
            // Apply the effect (we need a thread-safe call here)
            synchronized (sSync) {
                // No more than 1024 (the minimum supported by all the gles20 devices)
                int w = Math.min(dimen.width(), 1024);
                int h = Math.min(dimen.width(), 1024);
                effect.apply(textureHandles[0], w, h, textureHandles[1]);
            }
            handle = textureHandles[1];

            // Delete the unused texture
            int[] textures = {textureHandles[0]};
            GLES20.glDeleteTextures(1, textures, 0);
            GLESUtil.glesCheckError("glDeleteTextures");
        }

        // Return the texture handle identifier and the associated info
        GLESTextureInfo ti = new GLESTextureInfo();
        ti.handle = handle;
        ti.bitmap = bitmap;
        ti.path = null;
        return ti;
    }

    /**
     * Method that checks if an GLES error is present
     *
     * @param func The GLES function to check
     * @return boolean If there was an error
     */
    public static boolean glesCheckError(String func) {
        int error = GLES20.glGetError();
        if (error != 0) {
            Log.e(TAG, "GLES20 Error (" + glesGetErrorModule() + ") (" + func + "): " +
                    GLUtils.getEGLErrorString(error));
            return true;
        }
        return false;
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
     * Method that read a resource.
     *
     * @param res The resources reference
     * @param resId The resource identifier
     * @return String The shader source
     * @throws IOException If an error occurs while loading the resource
     */
    private static String readResource(Resources res, int resId) {
        Reader reader = new InputStreamReader(res.openRawResource(resId));
        try {
            final int BUFFER = 1024;
            char[] data = new char[BUFFER];
            int read = 0;
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
