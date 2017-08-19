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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ruesga.android.wallpapers.photophase.R;
import com.ruesga.android.wallpapers.photophase.model.Disposition;
import com.ruesga.android.wallpapers.photophase.model.Dispositions;
import com.ruesga.android.wallpapers.photophase.utils.DispositionUtil;
import com.ruesga.android.wallpapers.photophase.utils.Evaluators;
import com.ruesga.android.wallpapers.photophase.utils.MERAlgorithm;
import com.ruesga.android.wallpapers.photophase.widgets.ResizeFrame.OnResizeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class that allow to select the frames disposition visually
 */
public class DispositionView extends RelativeLayout
        implements OnClickListener, OnLongClickListener, OnResizeListener {

    /**
     * An interface to communicate the selection/unselection of a frame
     */
    public interface OnFrameSelectedListener {
        /**
         * Invoked when a frame is selected
         *
         * @param v The frame view selected
         */
        void onFrameSelectedListener(View v);
        /**
         * Invoked when a frame is unselected
         */
        void onFrameUnselectedListener();
    }

    private boolean mChanged;
    private List<Disposition> mDispositions;
    private int mCols;
    private int mRows;
    private boolean mEditable = false;
    private boolean mSaved = false;

    private View mTarget;
    private ResizeFrame mResizeFrame;
    private int mInternalPadding;
    private Rect mOldResizeFrameLocation;

    private OnFrameSelectedListener mOnFrameSelectedListener;

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
        mInternalPadding = (int)getResources().getDimension(R.dimen.disposition_frame_padding);
    }

    /**
     * Method that returns the dispositions drawn on this view
     *
     * @return List<Disposition> The dispositions drawn
     */
    public List<Disposition> getDispositions() {
        return mDispositions;
    }

    /**
     * Method that sets the disposition to draw on this view
     *
     * @param dispositions The dispositions to draw
     * @param animate If should animate the view
     */
    public void setDispositions(Dispositions dispositions, boolean animate) {
        setDispositions(dispositions.getDispositions(), dispositions.getCols(),
                dispositions.getRows(), animate);
    }

    public boolean isEditable() {
        return mEditable;
    }

    public void setEditable(boolean editable) {
        mEditable = editable;
    }

    public boolean isSaved() {
        return mSaved;
    }

    public void setSaved(boolean saved) {
        mSaved = saved;
    }

    /**
     * Method that sets the disposition to draw on this view
     *
     * @param dispositions The dispositions to draw
     * @param cols The number of columns
     * @param rows The number of rows
     * @param animate If should animate the view
     */
    public void setDispositions(List<Disposition> dispositions, int cols, int rows,
            boolean animate) {
        mDispositions = dispositions;
        mCols = cols;
        mRows = rows;

        // Remove all the current views and add the new ones
        recreateDispositions(animate);
        if (mResizeFrame != null) {
            mResizeFrame.setVisibility(View.GONE);
        }
        mChanged = false;
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
     * Method that set the listener for listen frame selection/unselection events
     *
     * @param onFrameSelectedListener The callback
     */
    public void setOnFrameSelectedListener(OnFrameSelectedListener onFrameSelectedListener) {
        this.mOnFrameSelectedListener = onFrameSelectedListener;
    }

    /**
     * Method that returns if the view was changed
     *
     * @return boolean true if the view was changed
     */
    public boolean isChanged() {
        return mChanged;
    }

    /**
     * Method that recreates all the dispositions
     *
     * @param animate If the recreate should be done with an animation
     */
    private void recreateDispositions(boolean animate) {
        // Remove all the current views and add the new ones
        removeAllViews();
        for (Disposition disposition : mDispositions) {
            createFrame(disposition, getLocationFromDisposition(disposition), animate);
        }
        mOldResizeFrameLocation = null;
        mTarget = null;
        if (mOnFrameSelectedListener != null) {
            mOnFrameSelectedListener.onFrameUnselectedListener();
        }
    }

    public void deselectCurrentFrame() {
        mTarget = null;
        if (mResizeFrame != null) {
            mResizeFrame.setVisibility(View.GONE);
        }
    }

    /**
     * Method that request the deletion of the current selected frame
     */
    @SuppressWarnings("boxing")
    public void deleteCurrentFrame() {
        if (mTarget == null) return;
        if (mResizeFrame == null) return;

        final Disposition targetDisposition = resizerToDisposition();

        // Get valid dispositions to move
        final List<Disposition> adjacents = findAdjacentsDispositions(targetDisposition);
        if (adjacents == null) {
            // Nothing to do
            Toast.makeText(getContext(),
                    R.string.pref_disposition_unable_delete_advise, Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide resize rubber
        mResizeFrame.setVisibility(View.GONE);

        // Animate adjacent views
        List<Animator> animators = new ArrayList<>();
        animators.add(ObjectAnimator.ofFloat(mTarget, "scaleX", 1.0f, 0.0f));
        animators.add(ObjectAnimator.ofFloat(mTarget, "scaleY", 1.0f, 0.0f));

        Disposition first = null;
        for (Disposition adjacent : adjacents) {
            // Extract the view and remove from dispositions
            View v = findViewFromRect(getLocationFromDisposition(adjacent));
            mDispositions.remove(adjacent);

            // Clone first disposition
            if (first == null) {
                first = new Disposition();
                first.x = adjacent.x;
                first.y = adjacent.y;
                first.w = adjacent.w;
                first.h = adjacent.h;
            }

            // Add animators and fix the adjacent
            if (v != null) {
                if (first.x < targetDisposition.x) {
                    // From Left to Right
                    int width = mTarget.getWidth() + mInternalPadding;
                    animators.add(ValueAnimator.ofObject(
                            new Evaluators.WidthEvaluator(v), v.getWidth(), v.getWidth() + width));

                    // Update the adjacent
                    adjacent.w += targetDisposition.w;
                    mDispositions.add(adjacent);

                } else if (first.x > targetDisposition.x) {
                    // From Right to Left
                    int width = mTarget.getWidth() + mInternalPadding;
                    animators.add(ValueAnimator.ofObject(
                            new Evaluators.WidthEvaluator(v), v.getWidth(), v.getWidth() + width));
                    animators.add(ObjectAnimator.ofFloat(v, "x", v.getX(), mTarget.getX()));

                    // Update the adjacent
                    adjacent.x = targetDisposition.x;
                    adjacent.w += targetDisposition.w;
                    mDispositions.add(adjacent);

                } else if (first.y < targetDisposition.y) {
                    // From Top to Bottom
                    int height = mTarget.getHeight() + mInternalPadding;
                    animators.add(ValueAnimator.ofObject(
                            new Evaluators.HeightEvaluator(v), v.getHeight(), v.getHeight() + height));

                    // Update the adjacent
                    adjacent.h += targetDisposition.h;
                    mDispositions.add(adjacent);

                } else if (first.y > targetDisposition.y) {
                    // From Bottom to Top
                    int height = mTarget.getHeight() + mInternalPadding;
                    animators.add(ValueAnimator.ofObject(
                            new Evaluators.HeightEvaluator(v), v.getHeight(), v.getHeight() + height));
                    animators.add(ObjectAnimator.ofFloat(v, "y", v.getY(), mTarget.getY()));

                    // Update the adjacent
                    adjacent.y = targetDisposition.y;
                    adjacent.h += targetDisposition.h;
                    mDispositions.add(adjacent);
                }
            }
        }
        if (animators.size() > 0) {
            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(animators);
            animSet.setDuration(getResources().getInteger(R.integer.disposition_hide_anim));
            animSet.setInterpolator(new AccelerateInterpolator());
            animSet.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    // Ignore
                }
                @Override
                public void onAnimationRepeat(Animator animation) {
                    // Ignore
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    finishDeleteAnimation(targetDisposition);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    finishDeleteAnimation(targetDisposition);
                }
            });
            animSet.start();
        }
    }

    /**
     * Method that finalizes the delete animation
     *
     * @param target The disposition target
     */
    private void finishDeleteAnimation(Disposition target) {
        removeView(mTarget);
        mDispositions.remove(target);
        Collections.sort(mDispositions);
        mChanged = true;

        // Clean status
        mOldResizeFrameLocation = null;
        mTarget = null;
        if (mOnFrameSelectedListener != null) {
            mOnFrameSelectedListener.onFrameUnselectedListener();
        }
    }

    /**
     * Method that create a new frame to be drawn in the specified location
     *
     * @param r The location relative to the parent layout
     * @return v The new view
     */
    private View createFrame(Disposition disposition, Rect r, boolean animate) {
        int padding = (int)getResources().getDimension(R.dimen.disposition_frame_padding);

        final FrameLayout v = new FrameLayout(getContext());
        ViewGroup.LayoutParams params =
                new RelativeLayout.LayoutParams(r.width() - padding, r.height() - padding);
        v.setX(r.left + padding);
        v.setY(r.top + padding);
        if (mEditable) {
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
        }
        v.setTag(disposition.uid);
        addView(v, params);

        // Image
        final ImageView image = new ImageView(getContext());
        image.setImageResource(mEditable ? R.drawable.ic_settings : R.drawable.ic_photo);
        image.setScaleType(ScaleType.CENTER);
        image.setBackgroundColor(ContextCompat.getColor(getContext(), mResizeFrame == null
                ? mSaved
                    ? R.color.disposition_saved_frame_bg_color
                    : R.color.disposition_locked_frame_bg_color
                : R.color.disposition_frame_bg_color));
        v.addView(image);

        // Flags toolbar
        final LinearLayout toolbar = new LinearLayout(getContext());
        FrameLayout.LayoutParams toolbarParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toolbarParams.gravity = Gravity.BOTTOM | Gravity.START;
        v.addView(toolbar, toolbarParams);

        int flagDimen = (int) getResources().getDimension(R.dimen.frame_settings_flag_size);
        padding = (int) getResources().getDimension(R.dimen.frame_settings_flag_padding);
        LinearLayout.LayoutParams flagParams = new LinearLayout.LayoutParams(flagDimen, flagDimen);
        flagParams.setMargins(padding, padding, padding, padding);
        if (!disposition.hasFlag(Disposition.BACKGROUND_FLAG)) {
            toolbar.addView(createFrameSettingFlag(R.drawable.ic_background_off), flagParams);
        } else {
            if (!disposition.hasFlag(Disposition.TRANSITION_FLAG)) {
                toolbar.addView(createFrameSettingFlag(R.drawable.ic_pause), flagParams);
            }
            if (!disposition.hasFlag(Disposition.EFFECT_FLAG)) {
                toolbar.addView(createFrameSettingFlag(R.drawable.ic_effect_off), flagParams);
            }
            if (!disposition.hasFlag(Disposition.BORDER_FLAG)) {
                toolbar.addView(createFrameSettingFlag(R.drawable.ic_border_off), flagParams);
            }
        }

        // Animate the view
        if (animate) {
            List<Animator> animators = new ArrayList<>();
            animators.add(ObjectAnimator.ofFloat(v, "scaleX", 0.0f, 1.0f));
            animators.add(ObjectAnimator.ofFloat(v, "scaleY", 0.0f, 1.0f));
            animators.add(ObjectAnimator.ofFloat(v, "alpha", 0.0f, 1.0f));
            animators.add(ObjectAnimator.ofFloat(v, "alpha", 0.0f, 1.0f));

            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(animators);
            animSet.setDuration(getResources().getInteger(R.integer.disposition_show_anim));
            animSet.setInterpolator(new BounceInterpolator());
            animSet.setTarget(v);
            animSet.start();
        }

        return v;
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
    public void onClick(View v) {
        // if there is a frame selected then unselect it
        if (mResizeFrame != null && mResizeFrame.getVisibility() == View.VISIBLE) {
            mResizeFrame.hide();
            mTarget = null;
            if (mOnFrameSelectedListener != null) {
                mOnFrameSelectedListener.onFrameUnselectedListener();
            }
        } else {
            // Show settings?
            if (mEditable) {
                displayFrameSettings(v);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onLongClick(View v) {
        if (mResizeFrame != null && selectTarget(v)) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                            | HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        }
        return true;
    }

    @Override
    public void onStartResize(int mode) {
        if (mResizeFrame == null) return;
        mOldResizeFrameLocation = new Rect(
                                        mResizeFrame.getLeft(),
                                        mResizeFrame.getTop(),
                                        mResizeFrame.getRight(),
                                        mResizeFrame.getBottom());
    }

    @Override
    @SuppressLint("RtlHardcoded")
    public void onResize(int mode, float delta) {
        if (mTarget == null) return;
        if (mResizeFrame == null) return;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEndResize(final int mode) {
        if (mTarget == null) return;
        if (mResizeFrame == null) return;

        // Compute the removed dispositions
        computeRemovedDispositions();
        recreateDispositions(false);
        computeNewDispositions();

        // Finish resize (select the target and create the new dispositions)
        post(new Runnable() {
            @Override
            public void run() {
                // Select the target
                View v = findTargetFromResizeFrame();
                if (v != null) {
                    selectTarget(v);
                }
            }
        });
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
        mOldResizeFrameLocation = null;
        mTarget = null;
        if (mOnFrameSelectedListener != null) {
            mOnFrameSelectedListener.onFrameUnselectedListener();
        }
    }

    /**
     * Method that returns the target view for the current resize frame
     *
     * @return The target view
     */
    private View findTargetFromResizeFrame() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            if (v.getX() < (mResizeFrame.getX() + (mResizeFrame.getWidth() / 2)) &&
                (v.getX() + v.getWidth()) > (mResizeFrame.getX() + (mResizeFrame.getWidth() / 2)) &&
                v.getY() < (mResizeFrame.getY() + (mResizeFrame.getHeight() / 2)) &&
                (v.getY() + v.getHeight()) > (mResizeFrame.getY() + (mResizeFrame.getHeight() / 2))) {
                return v;
            }
        }
        return null;
    }

    /**
     * Method that returns the view under the rect
     *
     * @return The view
     */
    private View findViewFromRect(Rect r) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            if (v.getX() < (r.left + (r.width() / 2)) &&
                (v.getX() + v.getWidth()) > (r.left + (r.width() / 2)) &&
                v.getY() < (r.top + (r.height() / 2)) &&
                (v.getY() + v.getHeight()) > (r.top + (r.height() / 2))) {
                return v;
            }
        }
        return null;
    }

    /**
     * Method that select a view as the target of to resize
     *
     * @param v The target view
     */
    private boolean selectTarget(View v) {
        //Do not do long click if we do not have a target
        if (mTarget != null && v.equals(mTarget)) return false;
        if (mResizeFrame == null) return false;

        // Show the resize frame view just in place of the current clicked view

        mResizeFrame.hide();
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams)mResizeFrame.getLayoutParams();
        int padding = mInternalPadding + mResizeFrame.getNeededPadding();
        frameParams.width = v.getWidth() + (padding * 2);
        frameParams.height = v.getHeight() + (padding * 2);
        mResizeFrame.setX(v.getX() - padding);
        mResizeFrame.setY(v.getY() - padding);
        mResizeFrame.show();

        // Save the new view
        mTarget = v;
        if (mOnFrameSelectedListener != null) {
            mOnFrameSelectedListener.onFrameSelectedListener(v);
        }
        return true;
    }

    /**
     * Computes the removed layout disposition based on the actual resize frame
     */
    private void computeRemovedDispositions() {
        // Transform the resize rubber to a dispositions object
        Disposition resizeRubber = resizerToDisposition();

        // Delete all overlapped
        int count = mDispositions.size();
        for (int i = count - 1; i >= 0; i--) {
            Disposition disposition = mDispositions.get(i);
            if (!isVisible(disposition) || isOverlapped(resizeRubber, disposition)) {
                resizeRubber.uid = disposition.uid;
                resizeRubber.flags = disposition.flags;
                mDispositions.remove(disposition);
            }
        }

        // Add the new disposition
        mDispositions.add(resizeRubber);
        Collections.sort(mDispositions);

        mChanged = true;
    }

    /**
     * Computes the new layout disposition based on the actual resize frame
     */
    private void computeNewDispositions() {
        // Fill the empty areas
        do {
            byte[][] dispositionMatrix = DispositionUtil.toMatrix(mDispositions, mCols, mRows);
            Rect rect = MERAlgorithm.getMaximalEmptyRectangle(dispositionMatrix);
            if (rect == null || rect.width() == 0 && rect.height() == 0) {
                // No more empty areas
                break;
            }
            Disposition disposition = DispositionUtil.fromRect(rect);
            createFrame(disposition, getLocationFromDisposition(disposition), true);
            mDispositions.add(disposition);
        } while (true);

        // Now the view was changed and should be reported
        Collections.sort(mDispositions);
        mChanged = true;
    }

    /**
     * Method that converts the resize frame to a dispostion reference
     *
     * @return Disposition The disposition reference
     */
    private Disposition resizerToDisposition() {
        int w = getMeasuredWidth() - (getPaddingLeft() + getPaddingRight());
        int h = getMeasuredHeight() - (getPaddingTop() + getPaddingBottom());
        int cw = w / mCols;
        int ch = h / mRows;

        //Remove overlapped areas
        Disposition resizer = new Disposition();
        resizer.x = Math.round(mResizeFrame.getX() / cw);
        resizer.y = Math.round(mResizeFrame.getY() / ch);
        resizer.w = Math.round(mResizeFrame.getWidth() / cw);
        resizer.h = Math.round(mResizeFrame.getHeight() / ch);

        // Fix disposition (limits)
        resizer.x = Math.max(resizer.x, 0);
        resizer.y = Math.max(resizer.y, 0);
        resizer.w = Math.min(resizer.w, mCols - resizer.x);
        resizer.h = Math.min(resizer.h, mRows - resizer.y);

        return resizer;
    }

    /**
     * Method that returns all dispositions that matched exactly (in one side) with
     * the argument disposition.
     *
     * @param disposition The disposition to check
     */
    private List<Disposition> findAdjacentsDispositions(Disposition disposition) {
        if (mDispositions.size() <= 1) return null;

        // Check left size
        if (disposition.x != 0) {
            List<Disposition> dispositions = new ArrayList<>();
            for (Disposition d : mDispositions) {
                if (d.compareTo(disposition) != 0) {
                    if ((d.x + d.w) == disposition.x &&
                        (d.y >= disposition.y) &&
                        ((d.y + d.h) <= (disposition.y + disposition.h))) {
                        dispositions.add(d);
                    }
                }
            }
            // Check if the sum of all the dispositions matches the disposition
            int sum = 0;
            for (Disposition d : dispositions) {
                sum += d.h;
            }
            if (sum == disposition.h) {
                return dispositions;
            }
        }
        // Check top size
        if (disposition.y != 0) {
            List<Disposition> dispositions = new ArrayList<>();
            for (Disposition d : mDispositions) {
                if (d.compareTo(disposition) != 0) {
                    if ((d.y + d.h) == disposition.y &&
                        (d.x >= disposition.x) &&
                        ((d.x + d.w) <= (disposition.x + disposition.w))) {
                        dispositions.add(d);
                    }
                }
            }
            // Check if the sum of all the dispositions matches the disposition
            int sum = 0;
            for (Disposition d : dispositions) {
                sum += d.w;
            }
            if (sum == disposition.w) {
                return dispositions;
            }
        }
        // Check right size
        if ((disposition.x + disposition.w) != mCols) {
            List<Disposition> dispositions = new ArrayList<>();
            for (Disposition d : mDispositions) {
                if (d.compareTo(disposition) != 0) {
                    if ((d.x) == (disposition.x + disposition.w) &&
                        (d.y >= disposition.y) &&
                        ((d.y + d.h) <= (disposition.y + disposition.h))) {
                        dispositions.add(d);
                    }
                }
            }
            // Check if the sum of all the dispositions matches the disposition
            int sum = 0;
            for (Disposition d : dispositions) {
                sum += d.h;
            }
            if (sum == disposition.h) {
                return dispositions;
            }
        }
        // Check bottom size
        if ((disposition.y + disposition.h) != mRows) {
            List<Disposition> dispositions = new ArrayList<>();
            for (Disposition d : mDispositions) {
                if (d.compareTo(disposition) != 0) {
                    if ((d.y) == (disposition.y + disposition.h) &&
                        (d.x >= disposition.x) &&
                        ((d.x + d.w) <= (disposition.x + disposition.w))) {
                        dispositions.add(d);
                    }
                }
            }
            // Check if the sum of all the dispositions matches the disposition
            int sum = 0;
            for (Disposition d : dispositions) {
                sum += d.w;
            }
            if (sum == disposition.w) {
                return dispositions;
            }
        }
        return null;
    }

    /**
     * Method that checks if a dispositions overlaps another other disposition
     *
     * @param d1 One disposition
     * @param d2 Another disposition
     * @return boolean true if d1 overlaps d2
     */
    private static boolean isOverlapped(Disposition d1, Disposition d2) {
        Rect r1 = new Rect(d1.x, d1.y, d1.x + d1.w, d1.y + d1.h);
        Rect r2 = new Rect(d2.x, d2.y, d2.x + d2.w, d2.y + d2.h);
        return r1.intersect(r2);
    }

    /**
     * Method that checks if a dispositions is visible
     *
     * @param d The disposition to check
     * @return boolean true if d is visible
     */
    private static boolean isVisible(Disposition d) {
        return d.w > 0 && d.h > 0;
    }

    @SuppressLint("InflateParams")
    private void displayFrameSettings(final View v) {
        final String uid = (String) v.getTag();
        if (uid == null) {
            return;
        }
        final Disposition disposition = fromUid(uid);
        if (disposition == null) {
            return;
        }

        View dialogView = LayoutInflater.from(
                getContext()).inflate(R.layout.frame_settings, null, false);
        final SwitchCompat background = dialogView.findViewById(R.id.flag_background);
        final SwitchCompat transition = dialogView.findViewById(R.id.flag_transition);
        final SwitchCompat effect = dialogView.findViewById(R.id.flag_effect);
        final SwitchCompat border = dialogView.findViewById(R.id.flag_border);
        background.setChecked(disposition.hasFlag(Disposition.BACKGROUND_FLAG));
        transition.setChecked(disposition.hasFlag(Disposition.TRANSITION_FLAG));
        effect.setChecked(disposition.hasFlag(Disposition.EFFECT_FLAG));
        border.setChecked(disposition.hasFlag(Disposition.BORDER_FLAG));
        transition.setEnabled(background.isChecked());
        effect.setEnabled(background.isChecked());
        border.setEnabled(background.isChecked());
        background.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                transition.setEnabled(background.isChecked());
                effect.setEnabled(background.isChecked());
                border.setEnabled(background.isChecked());
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.frame_settings_dialog_title);
        builder.setView(dialogView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (background.isChecked()) {
                    disposition.addFlag(Disposition.BACKGROUND_FLAG);
                } else {
                    disposition.removeFlag(Disposition.BACKGROUND_FLAG);
                }
                if (transition.isChecked()) {
                    disposition.addFlag(Disposition.TRANSITION_FLAG);
                } else {
                    disposition.removeFlag(Disposition.TRANSITION_FLAG);
                }
                if (effect.isChecked()) {
                    disposition.addFlag(Disposition.EFFECT_FLAG);
                } else {
                    disposition.removeFlag(Disposition.EFFECT_FLAG);
                }
                if (border.isChecked()) {
                    disposition.addFlag(Disposition.BORDER_FLAG);
                } else {
                    disposition.removeFlag(Disposition.BORDER_FLAG);
                }

                // Redraw
                recreateDispositions(false);
                mChanged = true;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private Disposition fromUid(String uid) {
        for (Disposition disposition : mDispositions) {
            if (disposition.uid.equals(uid)) {
                return disposition;
            }
        }
        return null;
    }

    private ImageView createFrameSettingFlag(int imageResId) {
        final ImageView image = new ImageView(getContext());
        image.setImageResource(imageResId);
        image.setScaleType(ScaleType.FIT_CENTER);
        return image;
    }
}
