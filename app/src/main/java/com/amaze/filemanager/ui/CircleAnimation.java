package com.amaze.filemanager.ui;

import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.amaze.filemanager.ui.views.SizeDrawable;

/**
 * Created by Arpit on 30-07-2015.
 */
public class CircleAnimation extends Animation {

    private SizeDrawable circle;

    private float newAngle2;
    private float newAngle;
    private float newAngle1;

    public CircleAnimation(SizeDrawable circle, float newAngle, Float secondAngle) {
        this.newAngle1 = newAngle;
        this.newAngle = secondAngle;
        newAngle2 = 360;
        this.circle = circle;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {

        float angle = (interpolatedTime) * 360;
        if (angle < newAngle) {
            circle.setAngle(interpolatedTime, newAngle / 360);
        } else if (angle < newAngle1) {
            circle.setAngle(newAngle / 360, newAngle / 360);
            circle.setAngle1(interpolatedTime, newAngle1 / 360);
        } else {
            circle.setAngle(newAngle / 360, newAngle / 360);
            circle.setAngle1(newAngle1 / 360, newAngle1 / 360);
            circle.setAngle2(interpolatedTime, newAngle2 / 360);
        }
        /*if(angle<newAngle){
            circle.setAngle( angle,p1);
        }else if(angle<newAngle1){
            circle.setAngle( newAngle,p1);
            circle.setAngle1(angle-newAngle,p2);
        }else {
            circle.setAngle(newAngle,p1);
            circle.setAngle1(newAngle1-newAngle,p2);
            circle.setAngle2(angle-newAngle1,p3);
        }*/
        circle.requestLayout();
    }
}