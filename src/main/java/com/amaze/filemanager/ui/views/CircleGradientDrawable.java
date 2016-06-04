package com.amaze.filemanager.ui.views;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;

/**
 * Created by vishal on 30/5/16.
 * Class used to create background of check icon on selection with
 * a Custom {@link Color} and Stroke (boundary)
 */
public class CircleGradientDrawable extends GradientDrawable {

    private static final int STROKE_WIDTH = 2;
    private static final String STROKE_COLOR = "#EEEEEE";
    private DisplayMetrics mDisplayMetrics;

    public CircleGradientDrawable(String color, DisplayMetrics metrics) {
        this.mDisplayMetrics = metrics;
        setShape(OVAL);
        setSize(1, 1);
        setColor(Color.parseColor(color));
        setStroke(dpToPx(STROKE_WIDTH), Color.parseColor(STROKE_COLOR));
    }

    private int dpToPx(int dp) {
        int px = Math.round(dp * mDisplayMetrics.xdpi/mDisplayMetrics.DENSITY_DEFAULT);
        return px;
    }
}
