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
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ImageView.ScaleType;

import org.cyanogenmod.wallpapers.photophase.R;
import org.cyanogenmod.wallpapers.photophase.model.Disposition;

import java.util.List;

/**
 * A class that allow to select the frames disposition visually 
 */
public class DispositionView extends RelativeLayout {

    private List<Disposition> mDispositions;
    private int mCols;
    private int mRows;

    /**
     * Constructor of <code>DispositionView</code>.
     *
     * @param context The current context
     */
    public DispositionView(Context context) {
        super(context);
    }

    /**
     * Constructor of <code>DispositionView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public DispositionView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        addView(v, params);
    }

    /**
     * Method that returns the location of the frame from its disposition
     *
     * @param disposition The source disposition
     * @return Rect The location on parent view
     */
    private Rect getLocationFromDisposition(Disposition disposition) {
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        int cw = w / mCols;
        int ch = h / mRows;

        Rect location = new Rect();
        location.left = disposition.x * cw;
        location.top = disposition.y * ch;
        location.right = location.left + disposition.w * cw;
        location.bottom = location.top + disposition.h * ch;
        return location;
    }
}
