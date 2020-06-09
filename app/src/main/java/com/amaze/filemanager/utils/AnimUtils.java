/*
 * Copyright 2015 Google Inc.
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

package com.amaze.filemanager.utils;

import android.content.Context;
import android.os.Handler;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;

import com.amaze.filemanager.ui.views.ThemedTextView;

/**
 * Utility methods for working with animations.
 */
public class AnimUtils {

    private static Interpolator fastOutSlowIn;

    public static Interpolator getFastOutSlowInInterpolator(Context context) {
        if (fastOutSlowIn == null) {
            fastOutSlowIn = AnimationUtils.loadInterpolator(context,
                    android.R.interpolator.fast_out_slow_in);
        }
        return fastOutSlowIn;
    }

    /**
     * Animates filenames textview to marquee after a delay.
     * Make sure to set {@link TextView#setSelected(boolean)} to false in order to stop the marquee later
     */
    public static void marqueeAfterDelay(int delayInMillis, ThemedTextView marqueeView) {
        new Handler().postDelayed(() -> {
            // marquee works only when text view has focus
            marqueeView.setSelected(true);
        }, delayInMillis);
    }
}