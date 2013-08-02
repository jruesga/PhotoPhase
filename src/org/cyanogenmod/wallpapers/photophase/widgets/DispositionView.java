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
import android.graphics.Rect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ImageView.ScaleType;

import org.cyanogenmod.wallpapers.photophase.R;
import org.cyanogenmod.wallpapers.photophase.model.Disposition;
import org.cyanogenmod.wallpapers.photophase.widgets.ResizeFrame.OnResizeListener;

import java.util.List;

/**
 * A class that allow to select the frames disposition visually
 */
public class DispositionView extends RelativeLayout implements OnLongClickListener, OnResizeListener {

    private List<Disposition> mDispositions;
    private int mCols;
    private int mRows;

    private View mTarget;
    private ResizeFrame mResizeFrame;
    private int mInternalPadding;
    private Rect mOldResizeFrameLocation;

    private Vibrator mVibrator;

    /**
     * Constructor of <code>DispositionView</code>.
     *
     * @param context The current context
     */
    public DispositionView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>DispositionView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public DispositionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>DispositionView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public DispositionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Initialize the view
     */
    private void init() {
        mVibrator = (Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);
        mInternalPadding = (int)getResources().getDimension(R.dimen.disposition_frame_padding);
    }

    /**
     * Method that sets the disposition to draw on this view
     *
     * @param dispositions The dispositions to draw
     */
    public void setDispositions(List<Disposition> dispositions, int cols, int rows) {
        mDispositions = dispositions;
        mCols = cols;
        mRows = rows;

        // Remove all the current views and add the new ones
        removeAllViews();
        for (Disposition disposition : mDispositions) {
            createFrame(getLocationFromDisposition(disposition));
        }
    }

    /**
     * Method that sets the resize frame view
     *
     * @param resizeFrame The resize frame view
     */
    public void setResizeFrame(ResizeFrame resizeFrame) {
        mResizeFrame = resizeFrame;
        mResizeFrame.setOnResizeListener(this);
    }

    /**
     * Method that create a new frame to be drawn in the specified location
     *
     * @param r The location relative to the parent layout
     */
    private void createFrame(Rect r) {
        int padding = (int)getResources().getDimension(R.dimen.disposition_frame_padding);
        ImageView v = new ImageView(getContext());
        v.setImageResource(R.drawable.ic_camera);
        v.setScaleType(ScaleType.CENTER);
        v.setBackgroundColor(getResources().getColor(R.color.disposition_frame_bg_color));
        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(r.width() - padding, r.height() - padding);
        params.leftMargin = r.left + padding;
        params.topMargin = r.top + padding;
        v.setOnLongClickListener(this);
        addView(v, params);
    }

    /**
     * Method that returns the location of the frame from its disposition
     *
     * @param disposition The source disposition
     * @return Rect The location on parent view
     */
    private Rect getLocationFromDisposition(Disposition disposition) {
        int w = getMeasuredWidth() - (getPaddingLeft() + getPaddingRight());
        int h = getMeasuredHeight() - (getPaddingTop() + getPaddingBottom());
        int cw = w / mCols;
        int ch = h / mRows;

        Rect location = new Rect();
        location.left = disposition.x * cw;
        location.top = disposition.y * ch;
        location.right = location.left + disposition.w * cw;
        location.bottom = location.top + disposition.h * ch;
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onLongClick(View v) {
        // Do not do long click if we do not have a target
//        if (mTarget != null) return false;

        // Show the resize frame view just in place of the current clicked view
        mResizeFrame.hide();
        RelativeLayout.LayoutParams viewParams =
                (RelativeLayout.LayoutParams)v.getLayoutParams();
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams)mResizeFrame.getLayoutParams();
        int padding = mInternalPadding + mResizeFrame.getNeededPadding();
        frameParams.width = viewParams.width + (padding * 2);
        frameParams.height = viewParams.height + (padding * 2);
        mResizeFrame.setX(v.getLeft() - padding);
        mResizeFrame.setY(v.getTop() - padding);
        mVibrator.vibrate(300);
        mResizeFrame.show();

        // Save the new view
        mTarget = v;

        return true;
    }

    @Override
    public void onStartResize(int mode) {
        mOldResizeFrameLocation = new Rect(
                                        mResizeFrame.getLeft(),
                                        mResizeFrame.getTop(),
                                        mResizeFrame.getRight(),
                                        mResizeFrame.getBottom());
    }

    @Override
    public void onResize(int mode, int delta) {
        if (mTarget == null) return;

        int w = getMeasuredWidth() - (getPaddingLeft() + getPaddingRight());
        int h = getMeasuredHeight() - (getPaddingTop() + getPaddingBottom());
        int minWidth = (w / mCols) + (w / mCols) / 2;
        int minHeight = (h / mRows) + (h / mRows) / 2;

        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams)mResizeFrame.getLayoutParams();
        switch (mode) {
            case Gravity.LEFT:
                float newpos = mResizeFrame.getX() + delta;
                if ((delta < 0 && newpos < (getPaddingLeft() * -1)) ||
                    (delta > 0 && newpos > (mResizeFrame.getX() + params.width - minWidth))) {
                    return;
                }
                mResizeFrame.setX(newpos);
                params.width -= delta;
                break;
            case Gravity.RIGHT:
                if ((delta < 0 && ((params.width + delta) < minWidth)) ||
                    (delta > 0 && (mResizeFrame.getX() + delta + params.width) > (getPaddingLeft() + getMeasuredWidth()))) {
                    return;
                }
                params.width += delta;
                break;
            case Gravity.TOP:
                newpos = mResizeFrame.getY() + delta;
                if ((delta < 0 && newpos < (getPaddingTop() * -1)) ||
                    (delta > 0 && newpos > (mResizeFrame.getY() + params.height - minHeight))) {
                    return;
                }
                mResizeFrame.setY(newpos);
                params.height -= delta;
                break;
            case Gravity.BOTTOM:
                if ((delta < 0 && ((params.height + delta) < minHeight)) ||
                    (delta > 0 && (mResizeFrame.getY() + delta + params.height) > (getPaddingTop() + getMeasuredHeight()))) {
                    return;
                }
                params.height += delta;
                break;

            default:
                break;
        }
        mResizeFrame.setLayoutParams(params);
    }

    @Override
    public void onEndResize(int mode) {
        try {
//        int w = getMeasuredWidth();
//        int h = getMeasuredHeight();
//        int cw = w / mCols;
//        int ch = h / mRows;
//
//        // Retrieve the new layout params
//        int neededPadding = mResizeFrame.getNeededPadding();
//        int padding = (int)getResources().getDimension(R.dimen.disposition_frame_padding)
//                            + neededPadding;
//        FrameLayout.LayoutParams params =
//                (FrameLayout.LayoutParams)mResizeFrame.getLayoutParams();
//        switch (mode) {
//            case Gravity.LEFT:
//                int left = params.leftMargin + padding;
//                if (left % cw != 0) {
//                    params.leftMargin = ((left / cw) * cw) - padding;
////                    params.width += ((left / cw) * cw) - left + (padding * 2);
//                }
//                break;
//            case Gravity.RIGHT:
//                int right = params.rightMargin + padding;
//                if (right % cw != 0) {
//                    params.rightMargin = ((right / cw) * cw) - padding;
////                    params.width += ((right / cw) * cw) - right + (padding * 2);
//                }
//                break;
//            case Gravity.TOP:
//                int top = params.topMargin + padding;
//                if (top % ch != 0) {
//                    params.topMargin = ((top / ch) * ch) - padding;
////                    params.height += ((top / cw) * cw) - top + (padding * 2);
//                }
//                break;
//            case Gravity.BOTTOM:
//                int bottom = params.bottomMargin + padding;
//                if (bottom % ch != 0) {
//                    params.bottomMargin = ((bottom / ch) * ch) - padding;
////                    params.height += ((bottom / cw) * cw) - bottom + (padding * 2);
//                }
//                break;
//
//            default:
//                break;
//        }
//        mResizeFrame.setLayoutParams(params);
//        mResizeFrame.invalidate();

        // Recalculate all the dispositions in base to the new positions

        } finally {
            // Reset vars
//            mOldResizeFrameLocation = null;
//            mTarget = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancel() {
        if (mOldResizeFrameLocation != null) {
            mTarget.setLeft(mOldResizeFrameLocation.left);
            mTarget.setRight(mOldResizeFrameLocation.right);
            mTarget.setTop(mOldResizeFrameLocation.top);
            mTarget.setBottom(mOldResizeFrameLocation.bottom);
        }
//        mOldResizeFrameLocation = null;
//        mTarget = null;
    }
}
