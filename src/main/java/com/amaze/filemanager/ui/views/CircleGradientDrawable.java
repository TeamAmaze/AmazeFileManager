package com.amaze.filemanager.ui.views;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

/**
 * Created by vishal on 30/5/16.
 * Class used to create background of check icon on selection with
 * a Custom {@link Color} and Stroke (boundary)
 */
public class CircleGradientDrawable extends GradientDrawable {

    private static final int STROKE_WIDTH = 8;
    private static final String STROKE_COLOR = "#EEEEEE";
    public CircleGradientDrawable(String color) {
        setShape(OVAL);
        setSize(10, 10);
        setColor(Color.parseColor(color));
        setStroke(STROKE_WIDTH, Color.parseColor(STROKE_COLOR));
    }
}
