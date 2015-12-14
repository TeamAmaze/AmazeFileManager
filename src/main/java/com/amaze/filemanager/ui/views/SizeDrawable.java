package com.amaze.filemanager.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * Created by Arpit on 30-07-2015.
 */
public class SizeDrawable extends View {
    Paint mPaint, mPaint1,mPaint2;
    RectF rectF;
    float startangle=-90,angle = 0;
    float startangle1=-90,angle1 = 0;


    float startangle2=-90,angle2=0;

    public SizeDrawable(Context context) {
        super(context);
    }

    int twenty;

    public SizeDrawable(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        int strokeWidth = dpToPx(10);
        rectF = new RectF(dpToPx(7), dpToPx(7), dpToPx(207), dpToPx(207));
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.parseColor("#E53935"));
        mPaint.setStrokeCap(Paint.Cap.BUTT);
        mPaint.setStrokeWidth(strokeWidth);
        mPaint1 = new Paint();
        mPaint1.setAntiAlias(true);
        mPaint1.setStyle(Paint.Style.STROKE);
        mPaint1.setColor(Color.parseColor("#0D47A1"));
        mPaint1.setStrokeCap(Paint.Cap.BUTT);
        mPaint1.setStrokeWidth(strokeWidth);
        mPaint2 = new Paint();
        mPaint2.setAntiAlias(true);
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setColor(Color.parseColor("#4CAF50"));
        mPaint2.setStrokeCap(Paint.Cap.BUTT);
        mPaint2.setStrokeWidth(strokeWidth);
        twenty = dpToPx(10);
    }

    DisplayMetrics displayMetrics;

    public int dpToPx(int dp) {
        if (displayMetrics == null) displayMetrics = getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        rectF.set(twenty, twenty, getWidth() - twenty, getHeight() - twenty);
            if (angle1 != 0) canvas.drawArc(rectF, startangle1, angle1, false, mPaint1);
            if (angle != 0) canvas.drawArc(rectF, startangle, angle, false, mPaint);
        if(angle2!=0)canvas.drawArc(rectF, startangle2, angle2, false, mPaint2);

    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle,float startangle)
    {
        this.angle = angle;
        this.startangle=startangle;
    }

    public float getAngle1() {
        return angle1;
    }

    public void setAngle1(float angle,float startangle1) {
        this.angle1 = angle;
        this.startangle1=startangle1;
    }

    public float getAngle2() {
        return angle2;
    }

    public void setAngle2(float angle2,float startangle2) {
        this.angle2 = angle2;
        this.startangle2=startangle2;
    }

}
