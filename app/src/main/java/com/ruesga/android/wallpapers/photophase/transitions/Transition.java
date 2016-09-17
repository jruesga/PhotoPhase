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

package com.ruesga.android.wallpapers.photophase.transitions;

import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;

import com.ruesga.android.wallpapers.photophase.PhotoFrame;
import com.ruesga.android.wallpapers.photophase.textures.TextureManager;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions.TRANSITIONS;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;

/**
 * The base class of all transitions that can be applied to the {@link PhotoFrame} classes.
 */
public abstract class Transition {

    public static final long MAX_TRANSTION_TIME = 2500L;

    private final Context mContext;
    private final TextureManager mTextureManager;

    protected int[] mProgramHandlers;
    protected int[] mTextureHandlers;
    protected int[] mPositionHandlers;
    protected int[] mTextureCoordHandlers;
    protected int[] mMVPMatrixHandlers;

    protected PhotoFrame mTarget;
    protected PhotoFrame mTransitionTarget;

    private final int[] mVertexShader;
    private final int[] mFragmentShader;

    private long mTime;
    protected boolean mRunning;

    private AccelerateInterpolator mInterpolator;

    /**
     * Constructor of <code>Transition</code>
     *
     * @param ctx The current context
     * @param tm The current texture manager
     * @param vertexShader The vertex shaders of the programs
     * @param fragmentShader The fragment shaders of the programs
     */
    public Transition(Context ctx, TextureManager tm, int[] vertexShader, int[] fragmentShader) {
        super();
        mContext = ctx;
        mTextureManager = tm;
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;

        // Compile every program
        if(mVertexShader.length != mFragmentShader.length) {
            throw new IllegalArgumentException("mVertexShader.length != mFragmentShader.length");
        }
        int cc = mVertexShader.length;
        mProgramHandlers = new int[cc];
        mTextureHandlers = new int[cc];
        mPositionHandlers = new int[cc];
        mTextureCoordHandlers = new int[cc];
        mMVPMatrixHandlers = new int[cc];
        for (int i = 0; i < cc; i++) {
            createProgram(i);
        }

        mInterpolator = new AccelerateInterpolator();
    }

    /**
     * Method that requests to apply this transition.
     *
     * @param target The target photo frame
     */
    public void select(PhotoFrame target) {
        mTarget = target;
        if (hasTransitionTarget()) {
            // Load the transition frame and request a picture for it
            mTransitionTarget =
                    new PhotoFrame(
                            target.getDisposition(),
                            mTextureManager,
                            mTarget.getFrameVertex(),
                            mTarget.getPhotoVertex(),
                            mTarget.getBackgroundColor());
        }
    }

    /**
     * Method that returns the target of the transition.
     *
     * @return PhotoFrame The target of the transition
     */
    public PhotoFrame getTarget() {
        return mTarget;
    }

    /**
     * Method that returns the transition target of the transition.
     *
     * @return PhotoFrame The transition target of the transition
     */
    public PhotoFrame getTransitionTarget() {
        return mTransitionTarget;
    }

    /**
     * Method that returns if the transition is selectable for the passed frame.
     *
     * @param frame The frame which the transition should be applied to
     * @return boolean If the transition is selectable for the passed frame
     */
    public boolean isSelectable(PhotoFrame frame) {
        return true;
    }

    /**
     * Method that resets the current status of the transition.
     */
    public void reset() {
        mTime = -1;
        mRunning = true;
    }

    public void swapTargets() {
        PhotoFrame target = mTransitionTarget;
        mTransitionTarget = mTarget;
        mTarget = target;
    }

    /**
     * Method that request a new mode
     */
    public void chooseMode() {
    }

    /**
     * Method that returns the type of transition.
     *
     * @return TRANSITIONS The type of transition
     */
    public abstract TRANSITIONS getType();

    /**
     * Returns the transition time
     */
    public abstract float getTransitionTime();

    /**
     * Method that requests to apply this transition.
     *
     * @param matrix The model-view-projection matrix
     * @param offset The x offset (only if Matrix.setIdentityM if applied to the matrix)
     */
    public final void apply(float[] matrix, float offset) {
        // Check internal vars
        if (mTarget == null ||
                mTarget.getPositionBuffer() == null ||
                mTarget.getTextureBuffer() == null) {
            return;
        }
        if (hasTransitionTarget() &&
                (mTransitionTarget == null ||
                mTransitionTarget.getPositionBuffer() == null ||
                mTransitionTarget.getTextureBuffer() == null)) {
            return;
        }

        // Set the time the first time
        if (mTime == -1) {
            mTime = SystemClock.uptimeMillis();
        }

        float delta = getDelta();
        applyTransition(delta, matrix, offset);

        mRunning = delta < 1;
    }

    protected abstract void applyTransition(float delta, float[] matrix, float offset);

    /**
     * Method that returns if the transition is being transition.
     *
     * @return boolean If the transition is being transition.
     */
    public boolean isRunning() {
        return mRunning;
    }

    /**
     * Method that return if the transition has a secondary target
     *
     * @return boolean If the transition has a secondary target
     */
    public boolean hasTransitionTarget() {
        return false;
    }

    /**
     * Method that creates the program
     */
    private void createProgram(int index) {
        mProgramHandlers[index] =
                GLESUtil.createProgram(
                        mContext.getResources(), mVertexShader[index], mFragmentShader[index]);
        mTextureHandlers[index] =
                GLES20.glGetUniformLocation(mProgramHandlers[index], "sTexture");
        GLESUtil.glesCheckError("glGetUniformLocation");
        mPositionHandlers[index] =
                GLES20.glGetAttribLocation(mProgramHandlers[index], "aPosition");
        GLESUtil.glesCheckError("glGetAttribLocation");
        mTextureCoordHandlers[index] =
                GLES20.glGetAttribLocation(mProgramHandlers[index], "aTextureCoord");
        GLESUtil.glesCheckError("glGetAttribLocation");
        mMVPMatrixHandlers[index] =
                GLES20.glGetUniformLocation(mProgramHandlers[index], "uMVPMatrix");
        GLESUtil.glesCheckError("glGetUniformLocation");
    }

    /**
     * Method that set the program to use
     *
     * @param index The index of the program to use
     */
    protected void useProgram(int index) {
        if (!GLES20.glIsProgram(mProgramHandlers[index])) {
            createProgram(index);
        }
        GLES20.glUseProgram(mProgramHandlers[index]);
        GLESUtil.glesCheckError("glUseProgram()");
    }

    /**
     * Method that requests to the transition to remove its internal references and resources.
     */
    public void recycle() {
        int cc = mProgramHandlers.length;
        for (int i = 0; i < cc; i++) {
            if (GLES20.glIsProgram(mProgramHandlers[i])) {
                if (GLESUtil.DEBUG_GL_MEMOBJS) {
                    Log.d(GLESUtil.DEBUG_GL_MEMOBJS_DEL_TAG, "glDeleteProgram: "
                            + mProgramHandlers[i]);
                }
                GLES20.glDeleteProgram(mProgramHandlers[i]);
                GLESUtil.glesCheckError("glDeleteProgram");
            }
            mProgramHandlers[i] = -1;
            mTextureHandlers[i] = -1;
            mPositionHandlers[i] = -1;
            mTextureCoordHandlers[i] = -1;
            mMVPMatrixHandlers[i] = -1;
        }
        mTransitionTarget = null;
        mTarget = null;
    }

    private float getDelta() {
        float delta = Math.min(
                SystemClock.uptimeMillis() - mTime, getTransitionTime()) / getTransitionTime();
        return mInterpolator.getInterpolation(delta);
    }
}
