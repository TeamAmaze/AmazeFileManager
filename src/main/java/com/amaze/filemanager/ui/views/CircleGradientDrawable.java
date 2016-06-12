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
    private static final String STROKE_COLOR_LIGHT = "#EEEEEE";
    private static final String STROKE_COLOR_DARK = "#424242";
    private DisplayMetrics mDisplayMetrics;

    /**
     * Constructor
     * @param color the hex color of circular icon
     * @param theme current theme light/dark which will determine the boundary color
     * @param metrics to convert the boundary width for {@link #setStroke} method from dp to px
     */
    public CircleGradientDrawable(String color, int theme, DisplayMetrics metrics) {
        this.mDisplayMetrics = metrics;
        setShape(OVAL);
        setSize(1, 1);
        setColor(Color.parseColor(color));
        setStroke(dpToPx(STROKE_WIDTH), (theme == 1) ? Color.parseColor(STROKE_COLOR_DARK)
                : Color.parseColor(STROKE_COLOR_LIGHT));
    }

    private int dpToPx(int dp) {
        int px = Math.round(dp * mDisplayMetrics.xdpi/mDisplayMetrics.DENSITY_DEFAULT);
        return px;
    }
}
