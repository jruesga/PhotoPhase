/*
 * Copyright (C) 2013 Jorge Ruesga
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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

/**
 * A scroll view that notifies the end of the scroll to create new views
 * dynamically.
 */
public class VerticalEndlessScroller extends ScrollView {

    /**
     * Interface to communicate end-scroll events
     */
    public interface OnEndScrollListener {
        /**
         * Called when the scroll reachs the end of the scroll
         */
        void onEndScroll();
    }

    private OnEndScrollListener mCallback;
    private int mEndPadding = 0;
    private boolean mSwitch = false;

    /**
     * Constructor of <code>VerticalEndlessScroller</code>.
     *
     * @param context The current context
     */
    public VerticalEndlessScroller(Context context) {
        super(context);
    }

    /**
     * Constructor of <code>VerticalEndlessScroller</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public VerticalEndlessScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor of <code>VerticalEndlessScroller</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public VerticalEndlessScroller(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Method that set the callback for notify end-scroll events
     *
     * @param callback The callback
     */
    public void setCallback(OnEndScrollListener callback) {
        mCallback = callback;
    }

    /**
     * Method that set the end padding for fired the event
     *
     * @param endPadding The end padding
     */
    public void setEndPadding(int endPadding) {
        this.mEndPadding = endPadding;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        // We take the last son in the scrollview
        View view = getChildAt(getChildCount() - 1);
        int diff = (view.getBottom() - (getHeight() + getScrollY()));
        if ((!mSwitch && diff <= mEndPadding)) {
            if (mCallback != null) {
                mCallback.onEndScroll();
                mSwitch = true;
                return;
            }
        } else if (diff > mEndPadding) {
            mSwitch = false;
        }
        if (!mSwitch) {
//            super.onScrollChanged(l, t, oldl, oldt);
        }
    }

}
