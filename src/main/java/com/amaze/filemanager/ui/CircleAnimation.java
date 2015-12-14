package com.amaze.filemanager.ui;

import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.amaze.filemanager.ui.views.SizeDrawable;

/**
 * Created by Arpit on 30-07-2015.
 */
public class CircleAnimation extends Animation {

    private SizeDrawable circle;

    private float newAngle;
    private float newAngle1;
    private float newAngle2;
    float p1,p2,p3;
    float q1,q2,q3;
    public CircleAnimation(SizeDrawable circle, float newAngle,Float secondAngle) {
        this.newAngle = newAngle;
        this.newAngle1 = secondAngle;
        this.newAngle2 = 360;
        this.circle = circle;
        p1=-90+newAngle1;
        p2=-90;p3=-90+newAngle;
        q1=newAngle/360;
        q2=newAngle1/360;
        q3=newAngle2/360;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {
        if(interpolatedTime<q2){
         float f=   newAngle1*interpolatedTime/q2;
        circle.setAngle1(f,p2);
            System.out.println(f+"\t"+interpolatedTime+"\t"+q2+"\t"+p2);
        }
        else if(interpolatedTime<q1){
            float f1=newAngle*interpolatedTime/q1;
            System.out.println(f1+"\t"+interpolatedTime+"\t"+q1+"\t"+p1);
         circle.setAngle(f1,p1);
        }
        else if(interpolatedTime<q3){
            float f1=newAngle2*interpolatedTime/q3;
            System.out.println(f1+"\t"+interpolatedTime+"\t"+q3+"\t"+p3);
            circle.setAngle2(f1,p3);
        }

        circle.requestLayout();
    }
}
