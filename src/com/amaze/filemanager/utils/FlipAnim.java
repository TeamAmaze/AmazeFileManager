package com.amaze.filemanager.utils;

import android.support.v4.view.ViewPager;
import android.view.View;

public class FlipAnim implements ViewPager.PageTransformer {
    private static final float MIN_SCALE = 0.75f;

    public void transformPage(View view, float position) {
        int pageWidth = view.getWidth();
        if (position < -1) { // [-Infinity,-1)
// This page is way off-screen to the left.
            view.setAlpha(0);
        } else if (position <= 0) { // [-1,0]
// Use the default slide transition when moving to the left page
            view.setAlpha(1);
            view.setTranslationY(0);
            //	view.setRotationX(Math.round(360*position));
            view.setRotationY(Math.round(360 * position));
            view.setScaleX(1 + position);
            view.setScaleY(1 + position);
        } else if (position <= 1) { // (0,1]
// Fade the page out.
            view.setAlpha(1 - position);
// Counteract the default slide transition
            view.setTranslationY(pageWidth * position * position);
            view.setTranslationX(pageWidth * position * position);
            //	view.setRotationX(Math.round(360*position));
            view.setRotationY(Math.round(360 * position));
// Scale the page down (between MIN_SCALE and 1)

            view.setScaleX(1 - position);
            view.setScaleY(1 - position);
        } else { // (1,+Infinity]
// This page is way off-screen to the right.
            view.setAlpha(0);
        }
    }
}
