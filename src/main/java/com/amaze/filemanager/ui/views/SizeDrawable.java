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
    Paint mPaint, mPaint1;
    RectF rectF;
    float angle = 0, angle1 = 0;

    public SizeDrawable(Context context) {
        super(context);
    }

    int twenty;

    public SizeDrawable(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        int strokeWidth = dpToPx(20);
        rectF = new RectF(dpToPx(7), dpToPx(7), dpToPx(207), dpToPx(207));
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.RED);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(strokeWidth);
        mPaint1 = new Paint();
        mPaint1.setAntiAlias(true);
        mPaint1.setStyle(Paint.Style.STROKE);
        mPaint1.setColor(Color.BLUE);
        mPaint1.setStrokeCap(Paint.Cap.ROUND);
        mPaint1.setStrokeWidth(strokeWidth);
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

        if (angle > angle1) {
            if (angle != 0) canvas.drawArc(rectF, -90, angle, false, mPaint);
            if (angle1 != 0) canvas.drawArc(rectF, -90, angle1, false, mPaint1);
        } else {
            if (angle1 != 0) canvas.drawArc(rectF, -90, angle1, false, mPaint1);
            if (angle != 0) canvas.drawArc(rectF, -90, angle, false, mPaint);
        }
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public float getAngle1() {
        return angle1;
    }

    public void setAngle1(float angle) {
        this.angle1 = angle;
    }

}
