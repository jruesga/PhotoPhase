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

package org.cyanogenmod.wallpapers.photophase.widgets;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.cyanogenmod.wallpapers.photophase.R;

/**
 * The hold view to resize a frame. A square with 4 handles in every border
 * to drag and resize a view
 */
public class ResizeFrame extends RelativeLayout {

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
        void onResize(int mode, int delta);
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
         * @param mode The resize mode (left, right, top, bottom)
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
        init();
    }

    /**
     * Constructor of <code>ResizeFrame</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public ResizeFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
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
        init();
    }

    /**
     * Method that initializes the view
     */
    @SuppressWarnings("boxing")
    private void init() {
        setBackgroundResource(R.drawable.resize_frame);
        setPadding(0, 0, 0, 0);

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        o.inTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
        BitmapFactory.decodeResource(getContext().getResources(), R.drawable.resize_handle_left, o);
        mNeededPadding = (int)(o.outWidth / 1.5f);

        LayoutParams lp;
        mLeftHandle = new ImageView(getContext());
        mLeftHandle.setImageResource(R.drawable.resize_handle_left);
        mLeftHandle.setTag(Gravity.LEFT);
        lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        lp.addRule(RelativeLayout.CENTER_VERTICAL);
        addView(mLeftHandle, lp);

        mRightHandle = new ImageView(getContext());
        mRightHandle.setImageResource(R.drawable.resize_handle_right);
        mRightHandle.setTag(Gravity.RIGHT);
        lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        lp.addRule(RelativeLayout.CENTER_VERTICAL);
        addView(mRightHandle, lp);

        mTopHandle = new ImageView(getContext());
        mTopHandle.setImageResource(R.drawable.resize_handle_top);
        mTopHandle.setTag(Gravity.TOP);
        lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        addView(mTopHandle, lp);

        mBottomHandle = new ImageView(getContext());
        mBottomHandle.setImageResource(R.drawable.resize_handle_bottom);
        mBottomHandle.setTag(Gravity.BOTTOM);
        lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        addView(mBottomHandle, lp);

        mExtraHandlingSpace = getResources().getDimension(R.dimen.resize_frame_extra_handling_space);
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
    public boolean onTouchEvent(MotionEvent ev) {
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
                    mOnResizeListener.onStartResize(((Integer)mHandle.getTag()).intValue());
                }
                return true;
            }
            break;

        case MotionEvent.ACTION_MOVE:
            if (mHandle != null) {
                // Resize
                if (mOnResizeListener != null) {
                    int handle = ((Integer)mHandle.getTag()).intValue();
                    int delta =
                            handle == Gravity.LEFT || handle == Gravity.RIGHT
                            ? Math.round(x - mLastTouchX)
                            : Math.round(y - mLastTouchY);
                    mOnResizeListener.onResize(handle, delta);
                    invalidate();
                }
                mLastTouchX = x;
                mLastTouchY = y;
                return true;
            }
            break;

        case MotionEvent.ACTION_UP:
            if (mHandle != null) {
                if (mOnResizeListener != null) {
                    mOnResizeListener.onEndResize(((Integer)mHandle.getTag()).intValue());
                    return true;
                }
                cancelMotion();
                break;
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
