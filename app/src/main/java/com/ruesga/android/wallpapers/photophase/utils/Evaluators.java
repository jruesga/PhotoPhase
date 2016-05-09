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

package com.ruesga.android.wallpapers.photophase.utils;

import android.animation.IntEvaluator;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

/**
 * A class with helpful evaluators
 */
public class Evaluators {

    /**
     * A width evaluator
     */
    public static class WidthEvaluator extends IntEvaluator {
        private final View mView;

        /**
         * Constructor of <code>WidthEvaluator</code>
         *
         * @param v The view
         */
        public WidthEvaluator(View v) {
            super();
            mView = v;
        }

        @Override
        @NonNull
        public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
            Integer num = super.evaluate(fraction, startValue, endValue);
            ViewGroup.LayoutParams params = mView.getLayoutParams();
            params.width = num;
            mView.setLayoutParams(params);
            return num;
        }
    }

    /**
     * A height evaluator
     */
    public static class HeightEvaluator extends IntEvaluator {
        private final View mView;

        /**
         * Constructor of <code>HeightEvaluator</code>
         *
         * @param v The view
         */
        public HeightEvaluator(View v) {
            super();
            mView = v;
        }

        @Override
        @NonNull
        public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
            Integer num = super.evaluate(fraction, startValue, endValue);
            ViewGroup.LayoutParams params = mView.getLayoutParams();
            params.height = num;
            mView.setLayoutParams(params);
            return num;
        }
    }
}
