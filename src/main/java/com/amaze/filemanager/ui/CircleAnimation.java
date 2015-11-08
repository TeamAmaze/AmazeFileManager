package com.amaze.filemanager.ui;

import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.amaze.filemanager.ui.views.SizeDrawable;

/**
 * Created by Arpit on 30-07-2015.
 */
public class CircleAnimation extends Animation {

    private SizeDrawable circle;

    private float oldAngle;
    private float newAngle;
    private float oldAngle1;
    private float newAngle1;

    public CircleAnimation(SizeDrawable circle, float newAngle,Float secondAngle) {
        this.oldAngle = circle.getAngle();
        this.newAngle = newAngle;
        this.oldAngle1 = circle.getAngle1();
        this.newAngle1 = secondAngle;
        this.circle = circle;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {
        float angle = oldAngle + ((newAngle - oldAngle) * interpolatedTime);
        circle.setAngle(angle);
        float angle1 = oldAngle1 + ((newAngle1 - oldAngle1) * interpolatedTime);
        circle.setAngle1(angle1);
        circle.requestLayout();
    }
}
