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

package com.ruesga.android.wallpapers.photophase.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.ruesga.android.wallpapers.photophase.R;

/**
 * The hold view to resize a frame. A square with 4 handles in every border
 * to drag and resize a view
 */
public class ResizeFrame extends FrameLayout {

    /**
     * An interface to communicate resize event states
     */
    public interface OnResizeListener {
        /**
         * Called when the resize is going to start
         *
         * @param mode The resize mode (left, right, top, bottom)
         * @see Gravity
         */
        void onStartResize(int mode);
        /**
         * Called when the resize is going to start
         *
         * @param mode The resize mode (left, right, top, bottom)
         * @param delta The delta motion
         * @see Gravity
         */
        void onResize(int mode, float delta);
        /**
         * Called when the resize was ended
         *
         * @param mode The resize mode (left, right, top, bottom)
         * @see Gravity
         */
        void onEndResize(int mode);
        /**
         * Called when the resize was cancelled
         *
         * @see Gravity
         */
        void onCancel();
    }

    private int mNeededPadding;

    private ImageView mLeftHandle;
    private ImageView mRightHandle;
    private ImageView mTopHandle;
    private ImageView mBottomHandle;

    private float mExtraHandlingSpace;

    private View mHandle;

    private float mLastTouchX;
    private float mLastTouchY;

    private OnResizeListener mOnResizeListener;

    /**
     * Constructor of <code>ResizeFrame</code>.
     *
     * @param context The current context
     */
    public ResizeFrame(Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructor of <code>ResizeFrame</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public ResizeFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructor of <code>ResizeFrame</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public ResizeFrame(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /**
     * Method that initializes the view
     */
    @SuppressLint("RtlHardcoded")
    private void init(Context context) {
        final Resources res = context.getResources();

        final int handleMargin = res.getDimensionPixelSize(R.dimen.resize_frame_handle_margin);
        mExtraHandlingSpace = res.getDimension(R.dimen.resize_frame_extra_handling_space);
        mNeededPadding = handleMargin;
        int overlayColor = ContextCompat.getColor(context, R.color.color_primary);

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, R.drawable.resize_frame_handle, o);
        mNeededPadding = handleMargin + (o.outWidth / 2);

        setBackgroundResource(R.drawable.resize_frame_shadow);
        Drawable dw = ContextCompat.getDrawable(context, R.drawable.resize_frame);
        dw.setColorFilter(overlayColor, PorterDuff.Mode.SRC_ATOP);
        setForeground(dw);
        setPadding(0, 0, 0, 0);

        LayoutParams lp;
        mLeftHandle = new ImageView(context);
        mLeftHandle.setImageResource(R.drawable.resize_frame_handle);
        mLeftHandle.setColorFilter(overlayColor, PorterDuff.Mode.SRC_ATOP);
        mLeftHandle.setTag(Gravity.LEFT);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.LEFT | Gravity.CENTER_VERTICAL);
        lp.leftMargin = handleMargin;
        addView(mLeftHandle, lp);

        mRightHandle = new ImageView(context);
        mRightHandle.setImageResource(R.drawable.resize_frame_handle);
        mRightHandle.setColorFilter(overlayColor, PorterDuff.Mode.SRC_ATOP);
        mRightHandle.setTag(Gravity.RIGHT);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        lp.rightMargin = handleMargin;
        addView(mRightHandle, lp);

        mTopHandle = new ImageView(context);
        mTopHandle.setImageResource(R.drawable.resize_frame_handle);
        mTopHandle.setColorFilter(overlayColor, PorterDuff.Mode.SRC_ATOP);
        mTopHandle.setTag(Gravity.TOP);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        lp.topMargin = handleMargin;
        addView(mTopHandle, lp);

        mBottomHandle = new ImageView(context);
        mBottomHandle.setImageResource(R.drawable.resize_frame_handle);
        mBottomHandle.setColorFilter(overlayColor, PorterDuff.Mode.SRC_ATOP);
        mBottomHandle.setTag(Gravity.BOTTOM);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        lp.bottomMargin = handleMargin;
        addView(mBottomHandle, lp);
    }

    /**
     * Method that set the callback for resize events
     *
     * @param onResizeListener The callback
     */
    public void setOnResizeListener(OnResizeListener onResizeListener) {
        mOnResizeListener = onResizeListener;
    }

    /**
     * Method that hides the view
     */
    public void hide() {
        setVisibility(View.GONE);
    }

    /**
     * Method that shows the view
     */
    public void show() {
        setVisibility(View.VISIBLE);
    }

    /**
     * Method that returns the extra padding to draw the handlers
     *
     * @return The extra padding space
     */
    public int getNeededPadding() {
        return mNeededPadding;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressLint({"RtlHardcoded", "ClickableViewAccessibility"})
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        final int action = ev.getAction();
        final float x = ev.getX();
        final float y = ev.getY();

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mHandle = getHandleFromCoordinates(x, y);
            if (mHandle != null) {
                // Start moving the resize frame
                mLastTouchX = x;
                mLastTouchY = y;

                // Start motion
                if (mOnResizeListener != null) {
                    mOnResizeListener.onStartResize((Integer) mHandle.getTag());
                }
                return true;
            }
            break;

        case MotionEvent.ACTION_MOVE:
            if (mHandle != null) {
                // Resize
                if (mOnResizeListener != null) {
                    int handle = (Integer) mHandle.getTag();
                    float delta = handle == Gravity.RIGHT || handle == Gravity.LEFT
                            ? x - mLastTouchX
                            : y - mLastTouchY;
                    mOnResizeListener.onResize(handle, delta);
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                mLastTouchX = x;
                mLastTouchY = y;
                return true;
            }
            break;

        case MotionEvent.ACTION_UP:
            if (mHandle != null) {
                if (mOnResizeListener != null) {
                    mOnResizeListener.onEndResize((Integer) mHandle.getTag());
                    cancelMotion();
                    return true;
                }
            }

        //$FALL-THROUGH$
        case MotionEvent.ACTION_CANCEL:
            cancelMotion();
            break;

        default:
            break;
        }

        return false;
    }

    /**
     * Cancel motions
     */
    private void cancelMotion() {
        mHandle = null;
        mLastTouchX = 0;
        mLastTouchY = 0;
        if (mOnResizeListener != null) {
            mOnResizeListener.onCancel();
        }
    }

    /**
     * Method that returns the resize handle touch from the the screen coordinates
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return View The handle view or null if no handle touched
     */
    private View getHandleFromCoordinates(float x, float y) {
        final View[] handles = {mLeftHandle, mRightHandle, mTopHandle, mBottomHandle};
        for (View v : handles) {
            if ((v.getLeft() - mExtraHandlingSpace) < x && (v.getRight() + mExtraHandlingSpace) > x &&
                (v.getTop() - mExtraHandlingSpace) < y && (v.getBottom() + mExtraHandlingSpace) > y) {
                return v;
            }
        }
        return null;
    }
}
