package com.ruesga.android.wallpapers.photophase.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.ruesga.android.wallpapers.photophase.PhotoFrame;
import com.ruesga.android.wallpapers.photophase.borders.Border;
import com.ruesga.android.wallpapers.photophase.borders.Borders;
import com.ruesga.android.wallpapers.photophase.effects.Effects;
import com.ruesga.android.wallpapers.photophase.model.Disposition;
import com.ruesga.android.wallpapers.photophase.textures.SimpleTextureManager;
import com.ruesga.android.wallpapers.photophase.transitions.Transition;
import com.ruesga.android.wallpapers.photophase.transitions.Transitions;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil;
import com.ruesga.android.wallpapers.photophase.utils.GLESUtil.GLColor;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LivePreviewView extends GLSurfaceView {

    private class Renderer implements GLSurfaceView.Renderer {

        private static final long TRANSITION_TIMEOUT = 2500L;

        private int mWidth = -1;
        private int mHeight = -1;

        private final float[] mMVPMatrix = new float[16];
        private final float[] mProjMatrix = new float[16];
        private final float[] mVMatrix = new float[16];

        private GLColor mBackgroundColor;

        private EffectContext mEffectContext;
        private Effects mEffectsFactory;
        private Borders mBordersFactory;

        private SimpleTextureManager mTextureManager;
        private Effect mEffect;
        private Border mBorder;
        private PhotoFrame mFrame;
        private Transition mTransition;

        private Transitions.TRANSITIONS mTransitionType = Transitions.TRANSITIONS.NO_TRANSITION;
        private Effects.EFFECTS mEffectType = Effects.EFFECTS.NO_EFFECT;
        private Borders.BORDERS mBorderType = Borders.BORDERS.NO_BORDER;

        private boolean mRecycled;

        private final Context mContext;
        private final Object mLock = new Object();

        public Renderer(Context context) {
            mContext = context;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            TypedValue a = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, a, true);
            if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                mBackgroundColor = new GLColor(a.data);
            } else {
                mBackgroundColor = new GLColor(Color.WHITE);
            }

            // We have a 2d (fake) scenario, disable all unnecessary tests. Deep are
            // necessary for some 3d effects
            GLES20.glDisable(GL10.GL_DITHER);
            GLESUtil.glesCheckError("glDisable");
            GLES20.glDisable(GL10.GL_CULL_FACE);
            GLESUtil.glesCheckError("glDisable");
            GLES20.glEnable(GL10.GL_DEPTH_TEST);
            GLESUtil.glesCheckError("glEnable");
            GLES20.glDepthMask(false);
            GLESUtil.glesCheckError("glDepthMask");
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);
            GLESUtil.glesCheckError("glDepthFunc");

            // Recreate the effect contexts
            recycle();
            synchronized (mLock) {
                mEffectContext = EffectContext.createWithCurrentGlContext();
                mEffectsFactory = new Effects(mContext, mEffectContext);
                mBordersFactory = new Borders(mContext, mEffectContext);

                recreateContext();
                boolean singleTexture = mTransitionType.equals(Transitions.TRANSITIONS.NO_TRANSITION);
                mTextureManager = new SimpleTextureManager(mContext, mEffect, mBorder, singleTexture);
                mTransition = Transitions.createTransition(mContext, mTextureManager, mTransitionType);
            }

            mRecycled = false;
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            mWidth = width;
            mHeight = height;
            mTextureManager.setTargetDimensions(new Rect(0, 0, mWidth, mHeight));
            createNewFrame();

            GLES20.glViewport(0, 0, mWidth, mHeight);
            GLESUtil.glesCheckError("glViewport");
            Matrix.frustumM(mProjMatrix, 0, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 2.0f);
        }

        private void recreateContext() {
            mEffect = mEffectsFactory.getEffect(mEffectType);
            mBorder = mBordersFactory.getBorder(mBorderType);
            mBorder.mBgColor = mBackgroundColor;
        }

        private void createNewFrame() {
            float[] frameVertices =  new float[]{-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
            synchronized (mLock) {
                if (mFrame != null) {
                    mFrame.recycle();
                }
                mFrame = new PhotoFrame(new Disposition(), mTextureManager,
                        frameVertices, frameVertices, mBackgroundColor);
                mTransition.select(mFrame);
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            // Check whether we have a valid surface
            if (!hasValidSurface()) {
                return;
            }

            Matrix.setLookAtM(mVMatrix, 0, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

            // Draw the background
            if (!mRecycled) {
                drawBackground();

                // Draw the image
                mTransition.apply(mMVPMatrix, 0);
                if (!mTransitionType.equals(Transitions.TRANSITIONS.NO_TRANSITION)) {
                    if (!mTransition.isRunning()) {
                        if (getRenderMode() != RENDERMODE_WHEN_DIRTY) {
                            setRenderMode(RENDERMODE_WHEN_DIRTY);
                        }
                        postDelayed(mTransitionRequestRenderer, TRANSITION_TIMEOUT);
                    } else {
                        if (getRenderMode() != RENDERMODE_CONTINUOUSLY) {
                            setRenderMode(RENDERMODE_CONTINUOUSLY);
                        }
                    }
                }

            }
        }

        private void drawBackground() {
            GLES20.glClearColor(
                    mBackgroundColor.r,
                    mBackgroundColor.g,
                    mBackgroundColor.b,
                    mBackgroundColor.a);
            GLESUtil.glesCheckError("glClearColor");
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLESUtil.glesCheckError("glClear");
        }

        public void setEffect(Effects.EFFECTS effect) {
            synchronized (mLock) {
                mEffectType = effect;
            }
        }

        public void setBorder(Borders.BORDERS border) {
            synchronized (mLock) {
                mBorderType = border;
            }
        }

        public void setTransition(Transitions.TRANSITIONS transition) {
            synchronized (mLock) {
                mTransitionType = transition;
            }
        }

        public void recreate() {
            synchronized (mLock) {
                recreateContext();
                onSurfaceChanged(null, mWidth, mHeight);
            }
        }

        public void requestTransition() {
            synchronized (mLock) {
                if (!mRecycled) {
                    if (mTransition.hasTransitionTarget()) {
                        mTransition.swapTargets();
                    }
                    mTransition.chooseMode();
                    mTransition.reset();
                }
            }
        }

        public void recycle() {
            synchronized (mLock) {
                mRecycled = true;
                if (mEffectContext != null) {
                    mEffectContext.release();
                    mEffectContext = null;
                }
                if (mEffectsFactory != null) {
                    mEffectsFactory.release();
                    mEffectsFactory = null;
                }
                if (mBordersFactory != null) {
                    mBordersFactory.release();
                    mBordersFactory = null;
                }
                if (mTransition != null) {
                    mTransition.recycle();
                    mTransition = null;
                }
                if (mFrame != null) {
                    mFrame.recycle();
                    mFrame = null;
                }
            }
        }
    }

    private final Runnable mTransitionRequestRenderer = new Runnable() {
        @Override
        public void run() {
            mRenderer.requestTransition();
            requestRender();
        }
    };

    private boolean mRecycled;
    private Renderer mRenderer;

    public LivePreviewView(Context context) {
        super(context);
        init(context);
    }

    public LivePreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mRecycled = false;
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        setEGLConfigChooser(false);
        getHolder().setFormat(PixelFormat.RGBA_8888);
        mRenderer = new Renderer(context);
        setRenderer(mRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void setEffect(Effects.EFFECTS effect) {
        mRenderer.setEffect(effect);
        requestRender();
    }

    public void setBorder(Borders.BORDERS border) {
        mRenderer.setBorder(border);
        requestRender();
    }

    public void setTransition(Transitions.TRANSITIONS transition) {
        mRenderer.setTransition(transition);
        requestRender();
    }

    public void recreate() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.recreate();
                requestRender();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mRecycled) {
            requestRender();
        }
    }

    public void recycle() {
        mRecycled = true;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.recycle();
            }
        });
    }

    private boolean hasValidSurface() {
        try {
            return getHolder().getSurface().isValid();
        } catch (Exception ex) {
            return false;
        }
    }
}
