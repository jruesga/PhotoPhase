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
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import org.cyanogenmod.wallpapers.photophase.R;

/**
 * A "Google Now Card Layout" like layout
 */
public class CardLayout extends LinearLayout {

    boolean inverted = false;

    /**
     * Constructor of <code>CardLayout</code>.
     *
     * @param context The current context
     */
    public CardLayout(Context context) {
        super(context);
    }

    /**
     * Constructor of <code>CardLayout</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public CardLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor of <code>CardLayout</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public CardLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Add a new card to the layout
     *
     * @param card The card view to add
     * @param animate If the add should be animated
     */
    public void addCard(final View card, final boolean animate) {
        post(new Runnable() {
            @Override
            public void run() {
                addView(card);
                if (animate) {
                    if (inverted) {
                        card.startAnimation(AnimationUtils.loadAnimation(
                                getContext(), R.anim.cards_animation_up_right));
                    } else {
                        card.startAnimation(AnimationUtils.loadAnimation(
                                getContext(), R.anim.cards_animation_up_left));
                    }
                    inverted = !inverted;
                }
            }
        });
    }
}
